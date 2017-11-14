(defproject web-server "1.0.0"
  :description "A small customised web server"
  :url "http://thomasbronner.hd.free.fr"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/java.jdbc "0.7.3"]
                 [postgresql/postgresql "9.3-1102.jdbc41"] ;; load the driver
                 [ring "1.6.2"]
                 [compojure "1.6.0"]
				         [hiccup "1.0.5"]
                 [de.ubercode.clostache/clostache "1.4.0"]]
  :plugins [[lein-ring "0.12.1"]]
  :ring {:handler web-server.core/webapp
         :auto-refresh? true}
  :main web-server.core
  :min-lein-version "2.0.0")
