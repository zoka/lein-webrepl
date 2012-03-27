# lein-webrepl

A Leiningen plugin that provides browser based nREPL interface, based on
[ringMon](https://github.com/zoka/ringMon).

## Installation

This plugin is only supported for Leiningen v 2.0 projects
running Clojure v 1.3.0 and above.

Put `[lein-webrepl "0.1.0-SNAPSHOT"]` into the `:plugins` vector of your
`:user` profile in `~/.lein/profiles.clj` for example:

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
    $ lein2 webrepl
```
To get usage help enter this:

```bash
    $ lein2 help webrepl
```

and you will get

```
Start a web REPL session with the current project or standalone.

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
classpath will be that of Leiningen.
```

## License

Copyright © 2012 Zoran Tomicic

Distributed under the Eclipse Public License, the same as Clojure.
