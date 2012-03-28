(ns leiningen.webrepl
  (:require
    [ringmon.server         :as server]
    [clojure.string         :as string]
    [leiningen.core.eval    :as eval]
    [leiningen.core.main    :as main]))

(defn- repl-port
 "Try to fin out preconfigured server port value."
 [project]
 (if-let [port (or (System/getenv "LEIN_REPL_PORT")
                   (:repl-port project))]
   (Integer. port)))

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
        proj-opts-strings  (into {} (for [[k v] proj-opts] [k (name v)]))]
    (merge {:lein-webrepl proj-opts-strings}
           (if port {:port port})
           (if no-browser {:local-repl true}))))

(defn- add-ringmon-dep [project]
 "Add lein-webrepl dependency to the project, if not already there."
  (if (some #(= 'ringmon (first %)) (:dependencies project))
    project
    (update-in project [:dependencies] conj ['ringmon "0.1.3-SNAPSHOT"])))

(defn- start-server
 "Start the ringMon nREPL server."
  [project port no-browser]
  (let [conf (make-srv-cfg project port no-browser)]
    (if project
      (eval/eval-in-project (add-ringmon-dep project)
                            `(when-not (ringmon.server/start ~conf)
                               (println "Could not start ringMon server.")
                               (System/exit 1))
                            '(require 'ringmon.server))
      (if (server/start conf) ; outside of a project
        @(promise) ; block forever
        (main/abort "Could not start ringMon server.")))))

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

 ([project]
    (start-server project (repl-port project) nil))

 ([project p1 & [p2]]
    (if (numeric? p1)
      (let [port (Integer. p1)
            p2   p2]
        (if p2
          (if (= p2 "-n")
            (start-server project port true)
            (main/abort "Unrecognized option:" p2))
          (start-server project port false)))
      (let [port? p2]
        (if (= p1 "-n")
          (if (and port? (numeric? port?))
            (start-server project (Integer. port?) true)
            (start-server project (repl-port project) true))
          (main/abort "Unrecognized option:" p1))))))

