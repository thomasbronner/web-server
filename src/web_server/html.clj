(ns web-server.html
  "handle all html generation"
  (:require
    [web-server.db :as db]
    [hiccup.core :refer :all]))



(def contact-html
  (let[line (first db/contact-rs)
       klass {:class "nomargin"}]
    (str
      (html [:p klass (str (:first_name line) " " (:last_name line))])
      (html [:p klass (:address line)])
      (html [:p klass (str (:zip_code line) " " (:city line) ", " (:country line) )])
      (html [:p klass (:tel line)])
      (html [:p klass [:a {:href (str "mailto:" (:mail line))} (:mail line)]])
      )))


        ;  <p class="nomargin"><a href="mailto:thomas.bronner@gmail.com">thomas.bronner@gmail.com</a></p>

; convert education resultset (list of maps) to html table
(def education-header
[:thead [:tr  [:th {:colspan 2 :align :center} "Annee"] [:th "Etablissement"] [:th "Domaine"] [:th "Diplôme"]]])

(def education-rows
  (apply vector
         (conj
           (map
            (fn [l] [:tr [:td ( int (:from l))] [:td (int (:to l))] [:td (:institution l)] [:td (:field_fr l)] [:td (:degree l)] ])
            db/rs-education)
          :tbody)))

(def education-table-html
  (html [:table {:class "pure-table"} education-header education-rows]))

; convert clients result set to html list
(def client-list
  (apply vector
         (conj
           (map
             (fn[l] [:li (str (:name l) (if-not (nil? (:city l)) ", ") (:city l))])
             db/rs-clients)
           :ul)))


(def client-list-html (html client-list))

;convert clients result set to html table with logo
(def client-with-logo-rows
  (apply vector
         (conj
           (map
             (fn [l] [:tr [:td (:name l)] [:td (:city l)]
                      [:td (if-not (nil? (:logo_png_base64 l))
                             [:img {:src (str "data:image/png;base64," (:logo_png_base64 l) ) }])]])
             db/rs-clients)
           :tbody)))

(def client-table-with-logo [:table {:class "borderless" }client-with-logo-rows])

(def client-table-with-logo-html (html client-table-with-logo))

;convert employee result sets to html
(def employer-rows
  (apply vector(conj
                 (map
                   (fn [line]
                     (let [tasks (:tasks line)]
                       [:tr
                        [:td  (:from line)]
                        [:td  (:to line)]
                        [:td  (:employer line)]
                        [:td  (:service line)]
                        [:td  (:city line)]
                        [:td [:ul  (map (fn [x] [:li x]) tasks) ]]]
                       ))
                   db/rs-salariat-tasks)
                 :tbody )))


(def employer-table-html (html [:table {:class "pure-table"} employer-rows]))

(def employer-html
  (apply str
  (map
    (fn [line]
      (let [       tasks (:tasks line)
                   from  (int (:from line))
                   to (int (:to line))
                   employer (:employer line)
                   service (if-not (nil? (:service line)) (str " " (:service line)))
                   city (if-not (nil? (:city line)) (str ", " (:city line)))
                   ]
        (str
          (html [:h4 {:class "content-subhead"} (str from "-" to " " employer service city)])
          (html [:ul  (map (fn [x] [:li x]) tasks) ])
          )
        ))
    db/rs-salariat-tasks)))

;skills
(defn skill-type-form-html "form for hiccup with selected checkboxes as parameters" [ids]
  (html [:form {:class "pure-form pure-form-aligned" :method "post"}
         ((fn [input]
            (loop [[line & remain] input
                   result [:fieldset]
                   counter 1]
              (let[id (:id_skill_type line)
                   name-fr (:short_name_fr line)
                   tooltip-fr (:long_name_fr line)
                   attrib {:type "checkbox" :id (str "name" counter) :name "skill-type-ids" :value id}]
                (if (nil? line) ;; put the buttons at the end
                  (-> result
                    (conj [:button {:type "submit" :class "pure-button pure-button-primary" :name "display"} "Afficher"])
                     (conj [:button {:type "submit" :class "pure-button pure-button-primary" :name "select-all-none"} "Tout/rien"]))
                  (recur remain
                         [:div {:class "pure-control-group"}
                          (-> result
                              ;input
                              (conj
                                ;set checked if id is part of ids
                                (if (some #(= id %) ids) ;TODO marche pas
                                  [:input (assoc attrib :checked true)]
                                  [:input attrib]))
                              ;label for input + tooltip
                              (conj [:label {:for (str "name" counter) :class "tooltip" } name-fr (if-not (nil? tooltip-fr) [:span {:class "tooltiptext"} tooltip-fr]) ])
                              )]
                         (inc counter)
                         )
                  ))))
          db/rs-skill-types)]))

(defn skills-of-types-html "fetch skills corresponding to skill type ids and format for hiccup" [ids]
  (if-not (or (nil? ids) (and (coll? ids) (empty? ids)))
    (str
      ;;(html [:h3 {:class "content-subhead"} "Détail"])
      (html [:table {:class "pure-table"}
             [:thead [:tr [:th "Technologie"] [:th "Version"] [:th "Maîtrise"[:a {:href "#mastery"} " * "] ] ]]
             [:tbody
              (map
                (fn [line]
                  (let [tech (:name line)
                        version (:version line)
                        mastery (:mastery line)]
                    [:tr [:td tech] [:td version] [:td (str mastery " %")] ]
                    ))
                (db/rs-skills-of-types ids))]]))))


;(use 'web-server.db :reload)
