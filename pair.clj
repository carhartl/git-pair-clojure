#!/usr/bin/env bb

;; Requires: https://github.com/borkdude/babashka
;; $ brew install borkdude/brew/babashka

(def help "Usage: $0 [-n] [-p <partial pair name>] [-i <issue-number>]

Options:
  -i <issue number>.      The JIRA issue number
  -p <partial pair name>  Part of the pair name
  -n                      dry run - only print template


This will create a file .git/.gitmessage.txt and set commit.template option to it.")

(def cli-options
  [["-p" "--pair PAIR" "Part of the pair name"]
   ["-i" "--issue ISSUE" "The JIRA issue number", :default "#0000"]
   ["-n" "--dry-run" "dry run - only print template", :default false]])

(def team #{
  "John Doe"  
  "Klaus Hartl"
})

(def template ".git/.gitmessage.txt")

(defn find-pair [team substring]
  (let [regex (re-pattern (str "(?i).*" substring ".*"))]
    (->> team
         (filter #(re-matches regex %))
         first)))
  ; (first (filter #(re-matches (re-pattern (str "(?i).*" substring ".*")) %) team)))

(defn format-pair [name]
  (let [first-and-last (str/split name #" ")]
    (->> first-and-last
         (map #(.toLowerCase %))
         (map #(first %))
         (str/join))))
  ; (str/join (map #(first (.toLowerCase %)) (str/split name #" "))))

(defn format-issue [issue]
  (if (str/starts-with? issue "#")
    issue
    (str "#" issue)))

(defn current-git-user []
  (->> (shell/sh "git" "config" "user.name")
       :out
       str/trim))
  ; (str/trim (:out (shell/sh "git" "config" "user.name"))))

(defn write-to-file [message]
  (spit template message))

(defn set-git-config []
  (shell/sh "git" "config" "commit.template" (str "$PWD/" template)))

(defn create-git-message []
  (let [options (tools.cli/parse-opts *command-line-args* cli-options)
        {{:keys [pair issue dry-run]} :options} options
        message (str
          (format-pair (find-pair team (current-git-user)))
          (if pair (str "/" (format-pair (find-pair team pair))))
          " "
          (format-issue issue)
          " "
          "Subject\n"
          "\n"
          "Some context/description")]
    (if dry-run
      (println message)
      (do
        (write-to-file message)
        (set-git-config)
        nil))))

(defn usage []
  (println "\033[0;31mThis is not a git directory!\033[0m")
  (println "")
  (println help)
  (System/exit 1))

(if (.isDirectory (io/file ".git"))
  (create-git-message)
  (usage))
