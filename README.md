# lein-web-repl

A Leiningen plugin that provides browser based nREPL interface

## Installation

This plugin is only supported by Leiningen v 2.0 for projects
running Clojure v 1.3.0 and above.

Put `[lein-webrepl "0.1.0-SNAPSHOT"]` into the `:plugins` vector of your
`:user` profile in `~/.lein/profiles.clj`:

```clojure
{:user {:plugins [[lein-clojars "0.8.0"]
                  [lein-webrepl "0.1.0-SNAPSHOT"]
                  [lein-difftest "1.3.7"]
                  [lein-marginalia "0.7.0"]
                  [lein-pprint "1.0.0"]]}}
```

## Usage

This plugin work both on a project level and standalone.

```bash
    $ lein webrepl
```
To get usage help:

```bash
    $ lein help webrepl
```
Note: At this stage passing parameters to webrepl does not work, it will 
always start at port `8888` and web browser window 
pointing at `localhost:8888`.

## License

Copyright © 2012 Zoran Tomicic

Distributed under the Eclipse Public License, the same as Clojure.
