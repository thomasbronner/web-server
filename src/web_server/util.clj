(ns web-server.util
  "misc util functions")

(defn DEBUG "show something in console" [label value] (println)(print (str "DEBUG-" label ":"))(println value));

(defn try-to-int "convert string to integer" [string]
  (if (string? string)
    (try
      (Integer/parseInt string)
      (catch Exception e string))
    string))


(defn parse-map-for-int "convert map values to integer" [map-to-update]
  (loop [  map-to-update map-to-update
           [current-key & remaining-keys] (keys map-to-update)
           result {}]
    (if (nil? current-key)
      result
      (recur (dissoc map-to-update current-key)
             remaining-keys
             (assoc result current-key (try-to-int (get map-to-update current-key)))))))

(defn parse-for-int "parse for string representing integers, return something similar" [thing]
  (if (nil? thing)
    thing
    (if(coll? thing)
      (cond
        (vector? thing) (vec (map try-to-int thing))
        (list? thing)  (map try-to-int thing)
        (set? thing) (set (map try-to-int thing))
        (map? thing) (parse-map-for-int thing)
        )
      (try-to-int thing))));


(defn split-search-string [string]
  (if (string? string)
    (clojure.string/split string #" ")))


(defn vectorize "put non collection value in vector" [thing] (if (or (coll? thing) (nil? thing)) thing [thing]))

