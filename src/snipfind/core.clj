(ns ^{:author "Mark Feeney"}
  snipfind.core
  (:require [clojure.tools.cli :refer [cli]]
            [clojure.java.io :refer [as-file reader]])
  (:import [java.io BufferedReader File]))

(set! *warn-on-reflection* true)

(defn snippet-refs [lines]
  "Extract snippet refs from lines (seq of text lines)."
  (->> lines
       (mapcat #(re-seq #"(?i)\bdata-lift=\"(\S+)\.(\S+)\"" %))))

(defn names->files [filenames]
  "Return a lazy seq of File objects created from filenames. If filenames is
  empty then read file names line by line from stdin. Only files that exist are
  returned."
  (let [names (if (zero? (count filenames))
                (line-seq (BufferedReader. *in*))
                filenames)]
    (->> names
         (map as-file)
         (filter #(.exists ^File %)))))

(defn- parse-cli [args]
  (cli args
       ["-h" "--help" "Show help and exit." :flag true]))

(defn -main [& args]
  (let [[opts args banner] (parse-cli args)]
    (when (:help opts)
      (println "snipfind [FILE0 [FILE1 [..]]]")
      (println banner)
      (System/exit 1))
    (->> (names->files args)
         (map reader)
         (map line-seq)
         (mapcat snippet-refs)
         (map #(drop 1 %))
         (group-by #(nth % 0)))))

(comment

  (names->files ["a" "b" "c"])
  (names->files ["/home/mark/.bashrc" "a" "b" "c"])

  (binding [*in* (java.io.StringReader. "foo\n/home/mark/.bashrc\n/home/mark/.profile")]
    (names->files ["-"]))

  (let [lines ["foo bar data-lift=\"Foo.bar?a=b\""
               "asdf" "xx data-lift=\"DoIt\""
               "xdata-lift=\"x\""]]
    (snippet-refs lines))

  (snippet-refs (line-seq (reader (as-file "/home/mark/.bash_profile"))))

)
