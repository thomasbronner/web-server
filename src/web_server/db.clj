(ns web-server.db
  "Handle all database interactions, sometime reprocessing fetched data"
  (:require [clojure.java.jdbc :as jdbc]
            [web-server.util :refer :all]))



(def db {:dbtype "postgresql"
         :dbname "TB_PRO"
         :host "192.168.0.1"
         :port 5432
         :user "cv"
         :password "cv"
         })

(def contact-rs
  (let [sql "select * from cv.id"]
    (jdbc/query db [sql])))

(defn rs-education [lang]
  (let [sql (str "select extract(year from \"from\") as \"from\",
                 extract(year from \"to\") as \"to\",institution,
                 field_" lang " as field,degree
                 from cv.education
                 order by \"from\" desc")]
    (jdbc/query db [sql])))


(def rs-clients
  (let [sql "select cl.name as name, ct.name as city,cl.logo_png_base64
        from cv.client cl
        left join cv.city ct using (id_city)
        order by 1"]
    (jdbc/query db [sql])))


(defn rs-salariat [lang]
  (let [sql (str "select id_work_experience,
                 extract(year from \"from\") as from,
                 extract(year from \"to\")as to,
                 e.name as employer,e.department as service,c.name as city,s.name_" lang " as sector
                 from cv.work_experience we
                 join cv.employer e using (id_employer)
                 left join cv.city c using (id_city)
                 left join cv.sector s using (id_sector)
                 order by \"from\" desc")]
    (jdbc/query db [sql])))

(defn get-tasks "get tasks corresponding to a id_work_experience" [lang id_work_experience]
  (let [sql (str "select content_" lang " from cv.task
                 left join cv.work_experience_task using (id_task)
                 where id_work_experience=" id_work_experience)]
    (map (keyword (str "content_" lang)) (jdbc/query db [sql]))))

;"add corresponding tasks to work experiences"
(defn rs-salariat-tasks [lang]
  (map
    (fn [l] (conj l {:tasks (get-tasks lang (:id_work_experience l))}))
    (rs-salariat lang)))

(defn rs-skill-types [lang]
  (let [sql (str "select id_skill_type,long_name_" lang " as long_name,short_name_" lang " as short_name,relevance from cv.skill_type order by relevance")]
    (jdbc/query db [sql])))

(defn rs-skills-of-types "fetch skills correspondint to skill type, support :all keyword" [lang type-ids]
  (if (or (nil? type-ids) (empty? type-ids))
    '()
    (let [sql (if  (or (some #(= :all %) type-ids) (and (= 1 (count type-ids)) (nil? (first type-ids))))
                "select id_skill,name,version,mastery from cv.skill order by mastery desc"
                (str "select id_skill,name,version,mastery from cv.skill where id_skill_type in (" (clojure.string/join "," type-ids) ") order by mastery desc"))]
      (jdbc/query db [sql]))))

(defn rs-skills-containing "fetch skills whose name contains any keywords. Expects a collection" [key-words]
  (if (or (not (coll? key-words)) (empty? key-words))
    '()
    (flatten (map
               (fn [key-word] (jdbc/query db [(str "select id_skill,name,version,mastery from cv.skill where lower(name) like '%" (clojure.string/lower-case key-word) "%';")]))
               key-words))))

(defn rs-freelance-tasks "map tasks and work type to work experiences" [lang]
  (let [work-experiences  (jdbc/query db [(str " select id_project,id_work_experience,
                                                 extract(year from we.\"from\") ||'-'|| extract(month from we.\"from\")  as \"from\",
                                                 extract(year from we.\"to\") ||'-'|| extract(month from we.\"to\")  as \"to\"
                                                 from cv.work_experience we
                                                 where id_employer is null")])

        tasks             (jdbc/query db [(str " select id_work_experience,content_" lang " as content
                                                 from cv.work_experience_task
                                                 left join cv.task t using (id_task)")])
        work-types        (jdbc/query db [(str " select id_work_experience,wt.name_" lang " as work_type
                                                 from cv.work_experience_task
                                                 left join cv.work_experience_work_type using (id_work_experience)
                                                 left join cv.work_type wt using (id_work_type)")])]

    (->> work-experiences
         (map (fn [we] (assoc we :tasks (map :content (filter #(= (:id_work_experience %) (:id_work_experience we )) tasks)))) )
         (map (fn [we] (assoc we :work-types (distinct (map :work_type (filter #(= (:id_work_experience %) (:id_work_experience we )) work-types))) ) ))
         (map #(dissoc % :id_work_experience)))))

(defn rs-freelance "construct a nested collection for all freelance work experience data" [lang]
  (let [projects         (jdbc/query db [(str " select distinct id_project,name as project,description_" lang " as description
                                                from cv.work_experience we
                                                left join cv.project p using (id_project)
                                                where id_employer is null
                                                order by id_project;")])
        clients          (jdbc/query db [(str " select distinct id_project,c.name as client, ci.name as city,ci.country, s.name_" lang " as sector
                                                from cv.work_experience we
                                                left join cv.work_experience_client using (id_work_experience)
                                                left join cv.project p using (id_project)
                                                left join cv.client c using (id_client)
                                                left join cv.city ci using (id_city)
                                                left join cv.sector s using (id_sector)
                                                order by id_project")])
        work-experiences-tasks  (rs-freelance-tasks lang)
        ids                     (map :id_project projects)]


    ;construct main from the project level
    (loop [  result []
             [id & ids] ids]
      (if (nil? id)
        result
        (let [ project (dissoc (first (filter #(= id (:id_project %)) projects)) :id_project)
               clients (filter #(= id (:id_project %)) clients)
               work-experiences-tasks (map #(dissoc % :id_project) (filter #(= id (:id_project %)) work-experiences-tasks))
               ]
          (recur (conj result (assoc project :clients clients :we work-experiences-tasks )) ids ))))))

;(use 'web-server.util :reload)
