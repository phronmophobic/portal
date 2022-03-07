(ns portal.ui.viewer.treemap
  (:require-macros [membrane.component :as component
                    :refer [defui
                            defeffect]])
  (:require ["react" :as react]
            [membrane.webgl :as webgl]
            [membrane.ui :as ui]
            [treemap-clj.view
             :refer [render-depth
                     render-value-labels
                     render-background-types
                     render-hierarchy-lines
                     render-keys
                     treemap-explore
                     wrap-treemap-events]]
            [treemap-clj.core
             :refer [treemap
                     keyed-treemap
                     make-rect]]
            [membrane.component :as component]))

(defn- use-resize []
  (let [ref              (react/useRef nil)
        [rect set-rect!] (react/useState #js {:height 200 :width 200})]
    (react/useEffect
     (fn []
       (when-let [el (.-current ref)]
         (let [resize-observer
               (js/ResizeObserver.
                (fn []
                  (set-rect! (.getBoundingClientRect el))))]
           (.observe resize-observer el)
           (fn []
             (.disconnect resize-observer)))))
     #js [(.-current ref)])
    [ref rect]))

(defn update-treemap
  [app-state obj]
  (let [[w h] [400 400]

        treemap-rect (make-rect w h)

        tm (keyed-treemap obj treemap-rect)

        background-opacity (if (<= (* w h) (* 350 350))
                             0.5
                             0.2)
        tm-render (wrap-treemap-events
                   tm
                   [(render-depth tm background-opacity)
                    (render-keys tm)])]

    (reset! app-state
            {:tm-render (webgl/->Cached tm-render)
             :tm tm})))

(defui treemap-container [{:keys [x y tm-render hover-rect]}]
  (ui/on
   :set
   (fn [$ref value]
     (if (= $ref $hover-rect)
       [[:set $hover-rect (dissoc value :obj)]]
       [[:set $ref value]]))
   (treemap-explore {:tm-render tm-render
                     :hover-rect hover-rect})))

(defn treemap-embed [opts value]
  (let [canvas (react/useRef nil)
        [debug set-debug!] (react/useState nil)

        [app set-app!] (react/useState nil)
        [app-state set-app-state!] (react/useState (atom {}))]
    (react/useEffect
     (fn []
       (when-let [el (.-current canvas)]
         (let [app (membrane.webgl/run
                     (component/make-app #'treemap-container app-state)
                     {:container el})]
           (set-app! app)
           (update-treemap app-state value)
           ((::webgl/repaint app))))
       
       (fn []
         ;; unmount
         ))
     #js [canvas])
    
    [:div
     [:canvas {:ref canvas
               :width "400"
               :height "400"
               :style {:background "white"}
               :tabindex "1"}]]))

(defn treemap-viewer [value]
  ^{:key (hash value)}
  [treemap-embed {} value])

(def viewer
  {:predicate map?
   :component treemap-viewer
   :name :com.phronemophobic/treemap})
