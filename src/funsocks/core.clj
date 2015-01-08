(ns funsocks.core
  (:require [immutant.web            :as web]
            [immutant.web.websocket  :as ws]
            [immutant.web.middleware :as web-middleware]
            [compojure.route         :as route]
            [ring.util.response :refer (redirect)]
            [compojure.core     :refer (GET defroutes)]
            [environ.core       :refer (env)]))

(def websocket-callbacks
  {:on-open     (fn [ch handshake]
                  (ws/send! ch "Ready to reverse the messages."))
   :on-close    (fn [ch {:keys [code reason]}]
                  (prn "close code:" code "reason:" reason))
   :on-message  (fn [ch m]
                  (prn "Received message:" m)
                  (ws/send! ch (apply str (reverse m))))})

(defroutes routes
  (GET "/" {c :context} (redirect (str c "/index.html")))
  (route/resources "/"))

(defn -main [& args]
  (web/run
    (-> routes
        (web-middleware/wrap-session {:timeout 20})
        (ws/wrap-websocket websocket-callbacks))
    (merge {"host" (env :demo-web-host), "port" (env :demo-web-port)}
           args)))
