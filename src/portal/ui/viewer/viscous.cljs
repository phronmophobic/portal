(ns portal.ui.viewer.viscous
  (:require-macros [membrane.component :as component
                    :refer [defui
                            defeffect]])
  (:require ["react" :as react]
            [membrane.webgl :as webgl]
            [membrane.ui :as ui]
            [com.phronemophobic.viscous :as viscous]
            [membrane.component :as component]))

(defn update-viscous [app-state obj]
  (swap! app-state
         (fn [state]
           (-> state
               (assoc :obj (viscous/wrap obj))
               (dissoc :membrane.component/extra)))))


(defui viscous-container [{:keys [x y tm-render hover-rect]}])

(def canvas-width 600)
(def canvas-height 800)

(defn viscous-embed [opts value]
  (let [canvas (react/useRef nil)
        [debug set-debug!] (react/useState nil)

        [app set-app!] (react/useState nil)
        [app-state set-app-state!] (react/useState (atom {:width 80
                                                          :height 40
                                                          :show-context? true
                                                          :obj (viscous/wrap nil)}))]
    (react/useEffect
     (fn []
       (when-let [el (.-current canvas)]
         (webgl/load-font
          (:name viscous/monospaced)
          "https://fonts.googleapis.com/css2?family=Ubuntu+Mono&display=swap"
          "https://fonts.gstatic.com/s/ubuntumono/v6/EgeuS9OtEmA0y_JRo03MQaCWcynf_cDxXwCLxiixG1c.ttf"
          (fn []
            (let [freetype-font (webgl/get-font viscous/monospaced)
                  space-glyph (-> freetype-font
                                  (.-glyphs)
                                  (.-glyphs)
                                  (aget 3))
                  font-size (:size ui/default-font)
                  fscale (membrane.webgl/font-scale freetype-font font-size)
                  advance (* fscale (.-advanceWidth space-glyph))]
              (set! viscous/cell-width advance)
              (set! viscous/cell-height (membrane.webgl/font-line-height viscous/monospaced)))
            (let [app
                  (membrane.webgl/run
                    (component/make-app #'viscous/inspector app-state)
                    {:container el})]
              (set-app! app)
              (update-viscous app-state value)
              ((::webgl/repaint app)))))
         )
       
       (fn []
         ;; unmount
         ))
     #js [canvas])
    
    [:div
     [:canvas {:ref canvas
               :width (str canvas-width)
               :height (str canvas-height)
               :style {:background "white"}
               :tabindex "1"}]]))

(defn viscous-viewer [value]
  ^{:key (hash value)}
  [viscous-embed {} value])

(def viewer
  {:predicate any?
   :component viscous-viewer
   :name :com.phronemophobic/viscous})
