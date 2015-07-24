(ns demo.views
  (:require-macros [reagent.ratom :as ratom])
  (:require [re-frame.core :as re-frame]))

;;---------- Generic plumbing
(def default-db
  {:temperatures {1 "Not hot" 2 "Hot"}
   :fruit [{:id 1 :temperature 1 :name "Apples"}
           {:id 2 :temperature 1 :name "Oranges"}
           {:id 3 :temperature 2 :name "Peppers"}]})

(re-frame/register-handler
 :initialize-db
 (fn  [_ _]
   default-db))

(re-frame/register-handler
 :change-a-temperature
 (fn [db _]
   (let [temperatures (:temperatures db)
         idx (rand-nth (keys temperatures))]
     (update-in db [:temperatures idx] #(str % "x")))))

(re-frame/register-handler
 :change-a-fruit
 (fn [db _]
   (let [fruit (:fruit db)
         idx (rand-int (count fruit))]
     (update-in db [:fruit idx :name] #(str % "x")))))

(re-frame/register-sub
 :fruit
 (fn [db _]
   (ratom/reaction (-> db deref :fruit))))

(re-frame/register-sub
 :fruit/ids
 (fn [db _]
   (let [fruit (re-frame/subscribe [:fruit])]
     (ratom/reaction
      (map :id @fruit)))))

(re-frame/register-sub
 :fruit/by-id
 (fn [db _]
   (let [fruit (re-frame/subscribe [:fruit])]
     (ratom/reaction
      (into {} (map (fn [f] [(:id f) f]) @fruit))))))

(re-frame/register-sub
 :temperatures
 (fn [db _]
   (ratom/reaction (-> db deref :temperatures))))

;;---------- Common components
(defn table-row [fruit]
  ;; PROBLEM#Keys - 1 Why isn't the following sufficient?
  ;; ^{:key (:id fruit)}
  [:tr
   ;; PROBLEM#Keys - 2 Why isn't the following sufficient?
   ;; {:key (:id fruit)}
   [:td (:name fruit)]
   [:td (:temperature-desc fruit)]])

(defn table
  [desc fruit row-cmp id-fn]
  (.log js/console desc)
  [:div
   [:hr desc]
   [:table
    [:thead
     [:th "Fruit"]
     [:th "Temperature"]]
    [:tbody
     (for [f @fruit]
       ;; PROBLEM#Keys - This is the only place where a key can be successfully
       ;; added.
       ^{:key (id-fn f)}
       [row-cmp f])]]])

;;---------- Style one, a subscription for the entire table.
;; The subscription is a function of all fruits and all temperatures so
;; any change to anything should cause the whole table to be re-rendered.
(re-frame/register-sub
 :style-one/decorated-fruit
 (fn [db _]
   (let [fruit (re-frame/subscribe [:fruit])
         temperatures (re-frame/subscribe [:temperatures])
         decorate (fn [temps {:keys [temperature] :as row}]
                    (assoc row :temperature-desc (temps temperature)))]
     (ratom/reaction
      ;; note - deref here not in decorate otherwise deref isn't noticed
      (map (partial decorate @temperatures) @fruit)))))

(defn style-1-row [row]
  (.log js/console "style-1-row for " (clj->js row))
  [table-row row])

(defn style-1-table []
  (let [fruit (re-frame/subscribe [:style-one/decorated-fruit])]
    (fn []
      [table "Subscription per table" fruit style-1-row :id])))

;;---------- Style two, a single subscription shared by all rows
;; Every row component has access to the temperatures component and
;; the rendering is stiched together in the row component.
;; Changes to any fruit update the table but no-op changes to the rows are
;; correctly discarded: the row component is only called if the data is
;; different.
;; However, any change to the temperatures cause every row to be rendered.
(defn style-2-row [row]
  (let [temperatures (re-frame/subscribe [:temperatures])]
    (fn [row]
      (.log js/console "style-2-row for " (clj->js row))
      [table-row (assoc row :temperature-desc (@temperatures (:temperature row)))])))

(defn style-2-table []
  (let [fruit (re-frame/subscribe [:fruit])]
    (fn []
      [table "Temperature subscription per row" fruit style-2-row :id])))

;;---------- Style three - each row is a subscription
;; This appears like it should be the most efficient - each row is scoped to a
;; row specific subscription, however that subscription is sensitive to
;; any changes in any fruit and any temperature.
(re-frame/register-sub
 :style-three/decorated-row
 (fn [db [_ fruit-id]]
   (let [id->fruit (re-frame/subscribe [:fruit/by-id])
         temperatures (re-frame/subscribe [:temperatures])]
     (ratom/reaction
      (let [fruit (@id->fruit fruit-id)]
        (assoc fruit :temperature-desc (@temperatures (:temperature fruit))))))))

(defn style-3-row [fruit-id]
  (let [decorated-row (re-frame/subscribe [:style-three/decorated-row fruit-id])]
    (fn [fruit-id]
      (.log js/console "style-3-row for " (clj->js @decorated-row))
      [table-row @decorated-row])))

(defn style-3-table []
  (let [ids (re-frame/subscribe [:fruit/ids])]
    (fn []
      [table "Fruit subscription per row (bad)" ids style-3-row identity])))

;;---------- Style four - each row is a subscription scoped only to that subscription
;; Although this requires more boilerplate it should be the most efficient.
;; Each row is passed an id and then a new subscription for each fruit is created.
;; Any changes to fruit _or_ temperatures should only cause re-rendering of the
;; row that is sensitive to that fruit or temperature.
(re-frame/register-sub
 :style-four/decorated-row
 (fn [db [_ fruit-id]]
   (let [id->fruit (re-frame/subscribe [:fruit/by-id])
         temperatures (re-frame/subscribe [:temperatures])
         the-fruit (ratom/reaction (@id->fruit fruit-id))
         the-temperature (ratom/reaction (@temperatures (:temperature @the-fruit)))]
     (ratom/reaction
      (assoc @the-fruit :temperature-desc @the-temperature)))))

(defn style-4-row [fruit-id]
  (let [decorated-row (re-frame/subscribe [:style-four/decorated-row fruit-id])]
    (fn [fruit-id]
      (.log js/console "style-4-row for " (clj->js @decorated-row))
      [table-row @decorated-row])))

(defn style-4-table []
  (let [ids (re-frame/subscribe [:fruit/ids])]
    (fn []
      [table "Fruit subscription per row (good)" ids style-4-row identity])))

;;---------- The rest of the UI
(defn change-fruit []
  [:button {:on-click #(re-frame/dispatch [:change-a-fruit])}
   "Change a fruit"])

(defn change-temperature []
  [:button {:on-click #(re-frame/dispatch [:change-a-temperature])}
   "Change a temperature"])

(defn main-panel []
  [:div
   [style-1-table]
   [style-2-table]
   [style-3-table]
   [style-4-table]
   [:div
    [change-fruit]
    [change-temperature]]])
