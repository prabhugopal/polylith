(ns polylith.clj.core.util.interface.time
  (:require [polylith.clj.core.util.time :as time]))

(defn current-time []
  (time/current-time))

(defn print-execution-time [start-time]
  (time/print-execution-time start-time))
