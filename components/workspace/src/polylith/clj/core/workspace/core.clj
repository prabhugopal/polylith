(ns polylith.clj.core.workspace.core
  (:require [clojure.string :as str]
            [polylith.clj.core.file.interface :as file]
            [polylith.clj.core.common.interface :as common]
            [polylith.clj.core.validator.interface :as validator]
            [polylith.clj.core.workspace.alias :as alias]
            [polylith.clj.core.workspace.base :as base]
            [polylith.clj.core.workspace.component :as component]
            [polylith.clj.core.workspace.project :as project]
            [polylith.clj.core.workspace.interfaces :as interfaces]))

(defn brick->lib-imports [brick]
  (into {} (mapv (juxt :name #(select-keys % [:lib-imports-src
                                              :lib-imports-test]))
                 brick)))

(defn brick->loc [bricks]
  (into {} (map (juxt :name #(select-keys % [:lines-of-code-src
                                             :lines-of-code-test]))
                bricks)))

(defn workspace-name [ws-dir]
  (let [cleaned-ws-dir (if (= "." ws-dir) "" ws-dir)
        path (file/absolute-path cleaned-ws-dir)
        index (str/last-index-of path file/sep)]
    (if (nil? index)
      path
      (subs path (inc index)))))

(defn project-sorter [{:keys [is-dev name]}]
  [is-dev name])

(defn enrich-workspace [{:keys [ws-dir user-input settings components bases projects paths] :as workspace}]
  (let [ws-name (workspace-name ws-dir)
        {:keys [top-namespace interface-ns color-mode]} settings
        suffixed-top-ns (common/suffix-ns-with-dot top-namespace)
        interfaces (interfaces/calculate components)
        interface-names (apply sorted-set (mapv :name interfaces))
        enriched-components (mapv #(component/enrich suffixed-top-ns interface-names settings %) components)
        enriched-bases (mapv #(base/enrich suffixed-top-ns interface-names settings %) bases)
        enriched-bricks (concat enriched-components enriched-bases)
        brick->loc (brick->loc enriched-bricks)
        brick->lib-imports (brick->lib-imports enriched-bricks)
        project-to-alias (alias/project-to-alias settings projects)
        enriched-projects (vec (sort-by project-sorter (map #(project/enrich-project % enriched-components enriched-bases brick->loc brick->lib-imports project-to-alias paths settings user-input) projects)))
        messages (validator/validate-ws suffixed-top-ns settings paths interface-names interfaces enriched-components enriched-bases enriched-projects interface-ns user-input color-mode)]
    (assoc workspace :name ws-name
                     :settings settings
                     :interfaces interfaces
                     :components enriched-components
                     :bases enriched-bases
                     :projects enriched-projects
                     :messages messages)))
