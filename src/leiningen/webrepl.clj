(ns leiningen.webrepl
  (:require
            [ringmon.server      :as server]
            [clojure.string      :as string]
            [leiningen.core.eval :as eval]
            [leiningen.core.project :as project]
            [leiningen.core.main :as main]))

"This plugin will only work with Lein v2.0"

(def profile {:dependencies '[[ringmon "0.1.2-SNAPSHOT"]
                              [lein-webrepl "0.1.0-SNAPSHOT"]
                              [ring/ring-jetty-adapter "1.0.1"]]})

(defn- repl-port
  [project]
  (Integer. (or (System/getenv "LEIN_REPL_PORT")
                (:repl-port project)
                -1))) ; return -1 if no port is preconfigured

(defn- make-srv-cfg
  [p nb]
  (if p
    (if-not nb
      {:port p :local-repl true}
      {:port p })
    (if-not nb
      {:local-repl true}
      {})))

(defn- add-webrepl-dep [project]
  (if (some #(= 'lein-webrepl (first %)) (:dependencies project))
    project
    (update-in project [:dependencies] conj ['lein-webrepl "0.1.0-SNAPSHOT"])))

(defn start-ringmon-server
  [cfg-map]
  (server/start cfg-map))

(defn- version-satisfies?
 "From leiningen.core.main."
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
    project)) ; pass through

(defn- add-clojure130-dep [project]
  (let [deps    (:dependencies project)
        clj-dep (first (filter #(= 'org.clojure/clojure (first %)) deps))]
    (if clj-dep
      (enforce-version project clj-dep "1.3.0")
      ; if the project did not specify Clojure dependency
      ; insert the 1.3.0 and hope for the best
      (update-in project [:dependencies] conj ['org.clojure/clojure "1.3.0"]))))

(defn- start-server
  [project port no-browser]
  (let [conf (make-srv-cfg port no-browser)]
    (println "Starting with conf:" conf)
    (if project
      (eval/eval-in-project
        (-> project (add-clojure130-dep)
                    (add-webrepl-dep))
       ;`(start-ringmon-server conf)  ; this does not work
        `(start-ringmon-server {:local-repl true}) ; this works fine (hardcoded map parameter)
        '(require 'leiningen.webrepl))
      (start-ringmon-server conf)))) ; outside of a project, always woks fine

(defn- forever
  []
  (while true
   (Thread/sleep Long/MAX_VALUE)))

(defn ^:no-project-needed webrepl
 "Start a web repl session with the current project or standalone.

USAGE: lein web-repl [-n] [port] | [port] [-n]
This will launch an nREPL server behind the freshly started Jetty instance,
and then it will open your default browser fresh window and make it
connect to the page containing the REPL user interface. The port value
the Jetty is runnig on will be taken from command line if supplied.
The LEIN_REPL_PORT environment variable is checked next, then the value
for the :repl-port key in project.clj, and finally it will default to 8888.
If port value is set to zero value, it is chosen randomly.
If option -n is supplied, no browser window will be opened.
You need this when application is running on a remote host or when you already
have a browser window awaiting connection from a previous run.The port and
option can be supplied in any order or not at all.
If you run this command inside of a project, it will be run in
the context of that classpath. If the command is run outside of a
project, it'll be standalone and the classpath will be that of Leiningen."
  ([] (webrepl nil))
  ([project]
    (let [port (repl-port project)]
      (if (= port -1)
        (start-server project nil  nil)
        (start-server project port nil))
      (forever)))
  ([project p1 & p2]
      (if (re-matches (re-pattern "\\d+") p1)
        (let [port (Integer. p1)
          p2   (first p2)]
          (if p2
            (if (= p2 "-n")
              (start-server project port true)
              (main/abort "Unrecognized option:" p2))
            (start-server project port false)))
        (let [port? (first p2)]
          (if (= p1 "-n")
            (if (and port? (re-matches (re-pattern "\\d+") port?))
              (start-server project (Integer. port?) true)
              (start-server project nil true))
            ((main/abort "Unrecognized option:" p1)))))
      (forever)))

