(ns funsocks.core
  (:require [immutant.web            :as web]
            [immutant.web.websocket  :as ws]
            [immutant.web.middleware :as web-middleware]
            [compojure.route         :as route]
            [ring.util.response :refer (redirect)]
            [compojure.core     :refer (GET POST defroutes)]
            [environ.core       :refer (env)]
            [clojure.set        :refer (union difference)]))

(defonce connected-channels (atom #{}))

(def websocket-callbacks
  {:on-open     (fn [ch handshake]
                  (swap! connected-channels union #{ch})
                  (ws/send! ch "Ready to reverse the messages."))
   :on-close    (fn [ch {:keys [code reason]}]
                  (swap! connected-channels difference #{ch})
                  (prn "close code:" code "reason:" reason))
   :on-message  (fn [ch m]
                  (prn "Received message:" m)
                  (ws/send! ch (apply str (reverse m))))})

(defn broadcast [req]
  (let [msg (:query-string req)]
    (prn "Broadcasting" msg)
    (doall (map (fn [ch] (ws/send! ch msg))
                @connected-channels))))

(defn postbin [req]
  (broadcast req)
  "Broadcast to all the connected channels")

(defroutes routes
  (GET "/" {c :context} (redirect (str c "/index.html")))
  (POST "/postbin" [] postbin)
  (route/resources "/"))

(defn -main [& args]
  (web/run
    (-> routes
        (web-middleware/wrap-session {:timeout 20})
        (ws/wrap-websocket websocket-callbacks))
    (merge {"host" (env :demo-web-host), "port" (env :demo-web-port)}
           args)))
