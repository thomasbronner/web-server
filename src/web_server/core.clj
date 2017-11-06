(ns web-server.core  "Handle all routing and mustache templates bindings"
  (:require
    [ring.adapter.jetty :refer :all]
    [compojure.core :refer :all]
    [compojure.route :refer :all]
    [clostache.parser :refer :all]
    [web-server.html :refer :all]
    ))

(defn read-template  "take a filename (with no extension) and read that file from /resources/templates"  [template-name]
  (slurp (clojure.java.io/resource
           (str "templates/" template-name ".mustache"))))

(defn render-template  "take a template file and a parameter map, and render it"  [template-file params]
  (render (read-template template-file) params))

(def vec-pattern #"\[.*\]")

(defn vectorize-string "convert params in vector, ex \"[\"1\" \"2\"]\" => [1 2] " [string]
  (if (nil? string)
    []
    (read-string(
                  str "["
                  (-> string
                      (clojure.string/replace "[" "")
                      (clojure.string/replace "]" "")
                      (clojure.string/replace "\"" "")
                      )"]"))))

;when using directly html, use {{{ ... }}} in template instead of {{ ... }}
(defn index  "read and render template index.mustache"  [params]
  (let [skill-type-vector (vectorize-string (get params "skill-type-ids"))]
    (render-template "index" {:education education-table-html
                              :clients client-table-with-logo-html
                              :salariat employer-html
                              :contact contact-html
                              :skill-type (cond ;relay the button used to the form generating function
                                            (contains? params select-all-skills) (skill-type-form-html skill-type-vector :all)
                                            (contains? params select-none-skills) (skill-type-form-html skill-type-vector :none)
                                            :else (skill-type-form-html skill-type-vector))
                              :skill-detail (cond ;relay the button used
                                               (contains? params select-all-skills) (skills-of-types-html :all)
                                              (contains? params select-none-skills) (skills-of-types-html :none)
                                              :else(skills-of-types-html skill-type-vector))
                              })))

(defroutes webapp
  (GET "/" [] (index {:skill-type-ids nil})) ;renvoie le template mustache "index", généré
  ;(POST "/" request (str request)) ;pour voir la requete
  ;(POST "/" [& params] (str params)) ;pour voir les param
  (POST "/" [& params] (index params)); put all params in a map<String,String>
  (resources "/"); fait pointer / dans html vers /resources/public du serveur. Pour les ressources static comme le css
  (not-found "404 Not Found"))


;(defn -main []  (run-jetty webapp {:port 8080 :join? false})) ; :join? false => give back repl prompt
; def the server once and for all, start/stop with (.start server) / (.stop server)
; :join?                - blocks the thread until server ends (defaults to true)
; Var-quote (#') #'x = (var x)  => getting meta-data for a defined symbol (as opposed to what it's pointing to.)
;(defonce server (run-jetty #'webapp {:port 8080 :join? false}) )


;(use 'web-server.html :reload)



