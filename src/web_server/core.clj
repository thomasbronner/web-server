(ns web-server.core  "Handle all routing and mustache templates bindings"
  (:require
    [ring.adapter.jetty :refer :all]
    [compojure.core :refer :all]
    [compojure.route :refer :all]
    [clostache.parser :refer :all]
    [web-server.html :refer :all]
    [web-server.util :refer :all]
    ))

(def current-lang "fr")


(defn read-template  "take a filename (with no extension) and read that file from /resources/templates"  [template-name]
  (slurp (clojure.java.io/resource
           (str "templates/" template-name ".mustache"))))

(defn render-template  "take a template file and a parameter map, and render it"  [template-file params]
  (render (read-template template-file) params))

;when using directly html, use {{{ ... }}} in template instead of {{ ... }}
(defn index  "read and render template index.mustache"
  ([lang] (index lang nil))
  ([lang params]

   (DEBUG "params" params)

   (let [skill-type-ids    (parse-for-int (get params "skill-type-ids"))
         tech-names        (split-search-string (get params "tech-names"))]
     (render-template (str "index-" lang) {
                                            :education (education-table-html current-lang)
                                            :languages "<strong>TODO</strong>"
                                            :clients client-table-with-logo-html
                                            :salariat (employer-html current-lang)
                                            :freelance (str "<strong>TODO corriger les merdes</strong>" (freelance-html current-lang))
                                            :contact contact-html

                                            :skill-type (cond ;relay the button used to the form generating function
                                                          (contains? params select-all-skills) (skill-type-form-html current-lang skill-type-ids :all)
                                                          (contains? params select-none-skills) (skill-type-form-html current-lang skill-type-ids :none)
                                                          :else (skill-type-form-html current-lang skill-type-ids))

                                            :skill-detail (cond ;relay the button used
                                                            (contains? params select-all-skills) (skills-of-types-html current-lang :all)
                                                            (contains? params select-none-skills) (skills-of-types-html current-lang :none)
                                                            :else (skills-of-types-html current-lang skill-type-ids))

                                            :skill-search-result (skills-containing-html tech-names)
                                            }))))


(defroutes webapp
  (GET "/" [] (index current-lang)) ;renvoie le template mustache "index", généré
  ;(POST "/" request (str request)) ;pour voir la requete
  ;(POST "/" [& params] (str params " string? params ="(string? params) " coll? params ="(coll? params)  )) ;pour voir les param
  (POST "/" [& params]; put all params in a map<String,String>
        (let [lang-param (get params "lang")]
          (def current-lang (if (nil? lang-param) current-lang lang-param));update current lang if such param is provided
        (index current-lang params)))
  (resources "/"); fait pointer / dans html vers /resources/public du serveur. Pour les ressources static comme le css
  (not-found "404 Not Found"))


;(defn -main []  (run-jetty webapp {:port 8080 :join? false})) ; :join? false => give back repl prompt
; def the server once and for all, start/stop with (.start server) / (.stop server)
; :join?                - blocks the thread until server ends (defaults to true)
; Var-quote (#') #'x = (var x)  => getting meta-data for a defined symbol (as opposed to what it's pointing to.)
;(defonce server (run-jetty #'webapp {:port 8080 :join? false}) )


;(use 'web-server.html :reload)
;(use 'web-server.util :reload)



