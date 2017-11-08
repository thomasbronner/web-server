(ns web-server.html
  "handle all html generation"
  (:require
    [web-server.db :refer :all]
    [web-server.util :refer :all]
    [hiccup.core :refer :all]))



(def contact-html
  (let[line (first contact-rs)
       klass {:class "nomargin"}]
    (str
      (html [:p klass (str (:first_name line) " " (:last_name line))])
      (html [:p klass (:address line)])
      (html [:p klass (str (:zip_code line) " " (:city line) ", " (:country line) )])
      (html [:p klass (:tel line)])
      (html [:p klass [:a {:href (str "mailto:" (:mail line))} (:mail line)]])
      )))

; convert education resultset (list of maps) to html table
(def education-header-fr
  [:thead [:tr  [:th {:colspan 2 :align :center} "Annee"] [:th "Etablissement"] [:th "Domaine"] [:th "Diplôme"]]])
(def education-header-en
  [:thead [:tr  [:th {:colspan 2 :align :center} "Year"] [:th "Institution"] [:th "Field"] [:th "Degree"]]])



(defn education-rows [lang]
  (apply vector
         (conj
           (map
             (fn [l] [:tr [:td ( int (:from l))] [:td (int (:to l))] [:td (:institution l)] [:td (:field l)] [:td (:degree l)] ])
             (rs-education lang))
           :tbody)))

(defn education-table-html [lang]
  (cond
    (= lang "en") (html [:table {:class "pure-table"} education-header-en (education-rows lang)])
    :else (html [:table {:class "pure-table"} education-header-fr (education-rows lang)])))

; convert clients result set to html list
(def client-list
  (apply vector
         (conj
           (map
             (fn[l] [:li (str (:name l) (if-not (nil? (:city l)) ", ") (:city l))])
             rs-clients)
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
             rs-clients)
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
                   rs-salariat-tasks)
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
           rs-salariat-tasks)))

;skills

(def display-selected-skills "display-selected-skills")
(def select-none-skills "select-none-skills")
(def select-all-skills "select-all-skills")

(defn skill-type-form-html "form for hiccup with selected checkboxes as parameters
  lang: language
  ids : skill type ids collection to retain checked checkbox
  all-none : :all to check all checkboxes, :none to uncheck all checkboxes"
  ([lang ids] (skill-type-form-html lang ids nil))
  ([lang ids all-none]
   (let [ids (vectorize ids)]
     (html [:form {:class "pure-form pure-form-aligned" :method "post" :action "/#skill-type-form" :id "skill-type-form"}
            ((fn [input]
               (loop [[line & remain] input
                      result [:fieldset]
                      counter 1]
                 (let[id (:id_skill_type line)
                      name- (:short_name line)
                      tooltip- (:long_name line)
                      attrib {:type "checkbox" :id (str "name" counter) :name "skill-type-ids" :value id}]
                   (if (nil? line) ;; put the buttons at the end
                     (if (= lang "en")
                       (-> result
                           (conj [:button {:type "submit" :class "pure-button" :name display-selected-skills } "Display"])
                           (conj [:button {:type "submit" :class "pure-button" :name select-all-skills } "All"])
                           (conj [:button {:type "submit" :class "pure-button" :name select-none-skills } "None"]))
                        (-> result
                           (conj [:button {:type "submit" :class "pure-button" :name display-selected-skills } "Afficher"])
                           (conj [:button {:type "submit" :class "pure-button" :name select-all-skills } "Tout"])
                           (conj [:button {:type "submit" :class "pure-button" :name select-none-skills } "Rien"])))
                     (recur remain
                            [:div {:class "pure-control-group"}
                             (-> result
                                 ;input
                                 (conj
                                   ;set checked if id is part of ids, considering :all or :none in all-none
                                   (if (cond
                                         (= all-none :all) true ; all checked
                                         (= all-none :none ) false; none checked
                                         (some #(= id %) ids) true) ;look in provided ids
                                     [:input (assoc attrib :checked true)]
                                     [:input attrib]))
                                 ;label for input + tooltip
                                 (conj [:label {:for (str "name" counter) :class "tooltip" } name- (if-not (nil? tooltip- ) [:span {:class "tooltiptext"} tooltip- ]) ])
                                 )]
                            (inc counter)
                            )))))
             (rs-skill-types lang))]))))

(defn skills-of-types-html "fetch skills corresponding to skill type ids and format for hiccup, accept :all or :none " [lang ids]
  (if (nil? ids)
    nil
    (let [ids (vectorize ids)]
      (if (or (some #(= :none %) ids) (empty? ids))
        nil
        (html [:table {:class "pure-table"}
               (if (= lang "en")
                 [:thead [:tr [:th "Technology"] [:th "Version"] [:th "Mastery"[:a {:href "#mastery"} " * "] ] ]]
                 [:thead [:tr [:th "Technologie"] [:th "Version"] [:th "Maîtrise"[:a {:href "#mastery"} " * "] ] ]])
               [:tbody
                (map
                  (fn [line]
                    (let [tech (:name line)
                          version (:version line)
                          mastery (:mastery line)]
                      [:tr [:td tech] [:td version] [:td (str mastery " %")] ]
                      ))
                  (rs-skills-of-types lang ids))]])))))

(defn skills-containing-html "fetch skills corresponding to search keyword and format for hiccup" [keywords]
  (if (or (nil? keywords) (empty? keywords))
    nil
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
              (rs-skills-containing keywords))]])))

;(use 'web-server.db :reload)
;(use 'web-server.util :reload)
