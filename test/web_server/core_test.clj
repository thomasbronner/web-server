(ns web-server.core-test
  (:require [clojure.test :refer :all]
            [web-server.core :refer :all]))

;; (deftest test-vectorize-string
;;   (testing "I'm testing vectorize-string"
;;            (is (=  (vectorize-string "[]") []))
;;            (is (=  (vectorize-string "") []))
;;            (is (=  (vectorize-string nil) []))
;;            (is (=  (vectorize-string "[1]") [1]))
;;            (is (=  (vectorize-string "1") [1]))
;;            (is (=  (vectorize-string "[1 2]") [1 2]))
;;            (is (=  (vectorize-string "1 2") [1 2]))
;;            (is (=  (vectorize-string "[ \"1\" ]") [1]))
;;            (is (=  (vectorize-string "[ \"1\" \"2\" ]") [1 2]))
;;   ))
