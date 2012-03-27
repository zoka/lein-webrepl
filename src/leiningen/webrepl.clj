(ns leiningen.webrepl
  (:require
    [ringmon.server         :as server]
    [clojure.string         :as string]
    [leiningen.core.eval    :as eval]
    [leiningen.core.project :as project]
    [leiningen.core.main    :as main]))

"This plugin will only work with Lein v2.0 and Clojure 1.3.0 and above"

(defn- repl-port
 "Try to fin out preconfigured server port value."
  [project]
  (Integer. (or (System/getenv "LEIN_REPL_PORT")
                (:repl-port project)
                -1))) ; return -1 if no port is preconfigured

(defn- make-srv-cfg
 "Take in 'project', 'port' and 'no-browser' and return
 the ringMon configuration map. Both 'port' and 'no-browser' can be nil.
 The :main and :repl-init keys are normally picked up ringMon's side
 from the project.clj actual file. However, here we do have a
 dynamic Leiningen project reference 'project' that may or may not
 have the same values for those 2 keys of interest for REPL operation,
 so just to be on the safe side, make sure that their values are
 propagated into the project runtime using the :lein-webrepl configuration key.
 Non-nil value for this key informs ringMon that it has been started by
 this plugin. Note that namespace vaules need to be 'sanitized'
 into strings before they are passed over to avoid class not found exceptions
 in the project's classpath context. ringMon is setting up
 the namespaces referred to by these 2 keys by executing on the fly
 generated function named 'repl-setup' when a new session is created."
  [project port no-browser]
  (let [proj-opts          (select-keys project [:main :repl-init])
        proj-opts-strings  (into {} (for [[k v] proj-opts] [k (name v)]))
        cfg  {:lein-webrepl proj-opts-strings}]
  (if port
    (if no-browser
      (merge cfg {:port port})
      (merge cfg {:port port :local-repl true}))
    (if no-browser
      cfg
      (merge cfg {:local-repl true})))))

(defn- add-webrepl-dep [project]
 "Add lein-webrepl dependency to the project, if not already there."
  (if (some #(= 'lein-webrepl (first %)) (:dependencies project))
    project
    (update-in project [:dependencies] conj ['lein-webrepl "0.1.0-SNAPSHOT"])))

(defn- version-satisfies?
 "From leiningen.core.main.
  Check if v1 satisfies v2"
  [v1 v2]
  (let [v1 (map #(Integer. %) (re-seq #"\d+" (first (string/split v1 #"-" 2))))
        v2 (map #(Integer. %) (re-seq #"\d+" (first (string/split v2 #"-" 2))))]
    (loop [versions (map vector v1 v2)
           [seg1 seg2] (first versions)]
      (cond (empty? versions) true
            (= seg1 seg2) (recur (rest versions) (first (rest versions)))
            (> seg1 seg2) true
            (< seg1 seg2) false))))

(def ^:private min-version-error
  "Abort: This plugin requires Clojure %s, but your project is using %s")

(defn- enforce-version
 "Abort if dependency version does not satisfy min-version."
  [project dependency min-version]
  (let [v (second dependency)]
    (when-not (version-satisfies? v min-version)
      (main/abort (format min-version-error min-version v)))
    project)) ; Ok, pass through

(defn- add-clojure130-dep [project]
 "Check the Clojure version project is using
  and abort for Clojure versions prior to 1.3.0. When no Clojure
  version is specified (possibly a generic library), add the
  1.3.0 dependency."
  (let [deps    (:dependencies project)
        clj-dep (first (filter #(= 'org.clojure/clojure (first %)) deps))]
    (if clj-dep
      (enforce-version project clj-dep "1.3.0")
      ; If the project did not specify Clojure dependency
      ; insert the 1.3.0 and hope for the best.
      (update-in project [:dependencies] conj ['org.clojure/clojure "1.3.0"]))))

(defn start-ringmon-server
 "A Jetty instance with nREPL server behind AJAX uri (/ringmon/command/)
  will be started on appropriate port, with or
  without browser window poping out."
  [cfg-map]
  (let [ok (server/start cfg-map)]
    (when-not ok
      (main/abort "Could not start ringMon server."))))

(defn- forever
 "Prevent process from exiting."
  []
  (while true
   (Thread/sleep Long/MAX_VALUE)))

(defn- start-server
 "Start the ringMon nREPL server."
  [project port no-browser]
  (let [conf (make-srv-cfg project port no-browser)]
    (if project
      (eval/eval-in-project
        (-> project (add-clojure130-dep)
                    (add-webrepl-dep))
        `(start-ringmon-server ~conf)
        '(require 'leiningen.webrepl))
      (do
        (start-ringmon-server conf)
        (forever)) ))) ; outside of a project

(defn- numeric?
 "Check if string contains a number."
  [s]
  (re-matches (re-pattern "\\d+") s))

(defn ^:no-project-needed webrepl
 "Start a web REPL session with the current project or standalone.

USAGE: lein2 webrepl [-n] [port] | [port] [-n]
This will launch an nREPL server behind the freshly started Jetty
instance and then open a fresh window of your default browser, connecting it
to the page featuring the nREPL front end.

The port value the Jetty is started on will be taken from command line,
if supplied. The LEIN_REPL_PORT environment variable is checked next,
then the value for the :repl-port key in project.clj, and finally it
will default to 8888. If port value is set to be zero, it is chosen randomly.
If option -n is supplied, no browser window will be opened. This is needed
when application is running on a remote host or when there is already
a browser window awaiting connection from the previous run.
For the time being (hopefully not for long) only one session
is supported per browser/per client computer, so if you have 2 or more windows
within the web browser connected to the same server, nREPL output will
be randomly sprinkled accros all of them. The workaround is to use
another browser type side by side (tested on Chrome, Firefox and Safari).

Note that REPL sessions are persistent - if you disconnect the browser for a
while and then load the REPL page again, all buffered session output
will be displayed in the output window.

If you run this command inside of a project, it will be run in
the context of that classpath and it will activate namespaces specified
by :main and :repl-init keys in project.clj when REPL session is established.
If the command is run outside of a project, it'll be standalone and the
classpath will be that of Leiningen."

 ([]
  (webrepl nil))

 ([project]
  (let [port (repl-port project)]
    (if (= port -1)
      (start-server project nil  nil)    ; use default port
      (start-server project port nil))   ; use preconfigured port
  ))

 ([project p1 & p2]
  (if (numeric? p1)
    (let [port (Integer. p1)
      p2   (first p2)]
      (if p2
        (if (= p2 "-n")
          (start-server project port true)
          (main/abort "Unrecognized option:" p2))
        (start-server project port false)))
    (let [port? (first p2)]
      (if (= p1 "-n")
        (if (and port? (numeric? port?))
          (start-server project (Integer. port?) true)
          (let [port (repl-port project)]
            (if (= port -1)
              (start-server project nil true)
              (start-server project port true))))
        ((main/abort "Unrecognized option:" p1)))))
  ))

