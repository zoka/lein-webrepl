# lein-webrepl

A Leiningen plugin that provides browser based nREPL interface

## Installation

This plugin is only supported for Leiningen v 2.0 projects
running Clojure v 1.3.0 and above.

Put `[lein-webrepl "0.1.0-SNAPSHOT"]` into the `:plugins` vector of your
`:user` profile in `~/.lein/profiles.clj` for exasmplr:

```clojure
{:user {:plugins [[lein-clojars "0.8.0"]
                  [lein-webrepl "0.1.0-SNAPSHOT"] ; <--- added 
                  [lein-marginalia "0.7.0"]
                  [lein-pprint "1.0.0"]]}}
```

## Usage

This plugin works both on a project level and standalone 
(outside of a project folder).

```bash
    $ lein webrepl
```
To get usage help enter this:

```bash
    $ lein help webrepl
```

and you will get 

```bash
 Start a web repl session with the current project or standalone.

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
project, it'll be standalone and the classpath will be that of Leiningen.
```

Note: At this stage passing parameters to webrepl does not work, it will 
always start at port `8888` and web browser window 
pointing at `localhost:8888`.

## License

Copyright © 2012 Zoran Tomicic

Distributed under the Eclipse Public License, the same as Clojure.
