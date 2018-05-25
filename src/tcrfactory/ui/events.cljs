(ns tcrfactory.ui.events
  (:require
    [cljs-solidity-sha3.core :refer [solidity-sha3]]
    [cljs-web3.core :as web3]
    [cljs-web3.eth :as web3-eth]
    [district.ui.smart-contracts.queries :as contract-q]
    [district.ui.web3-accounts.queries :as accounts-q]
    [district.ui.web3-sync-now.events :as sync-now-events]
    [district.ui.web3-tx.events :as tx-events]
    [re-frame.core :as re-frame :refer [reg-event-fx trim-v console dispatch]]
    [tcrfactory.shared.contract.registry-entry :refer [vote-option->num]]))

(def interceptors [trim-v])

(comment
  (dispatch [:create-registry {:registry/title "Top HODL Tokens for 2018"
                               :registry/description "Best tokens to hodl in 2018"
                               :registry/total-supply (web3/to-wei 1000000 :ether)
                               :registry/token-name "HODL"
                               :registry/token-symbol "HODL"
                               :registry/challenge-period-duration 300
                               :registry/commit-period-duration 300
                               :registry/reveal-period-duration 300
                               :registry/deposit (web3/to-wei 10 :ether)}])

  (dispatch [:create-registry-entry {:registry-entry-factory "0x93b7e67938ffbbd430d2a9744f73b3be51bf203d"
                                     :registry-token "0x1642d08eb9d06e8142a8ab1c20b6476acbb2b039"
                                     :deposit (web3/to-wei 10 :ether)
                                     :title "DNT"
                                     :description "A hodlable token"}])

  (dispatch [:create-challenge {:registry-entry "0x8ce83cb07c9853d0e34d2f591d8b0511de79a54c"
                                :registry-token "0x1642d08eb9d06e8142a8ab1c20b6476acbb2b039"
                                :deposit (web3/to-wei 10 :ether)
                                :description "This token no good"}])

  (dispatch [:commit-vote {:registry-entry "0x8ce83cb07c9853d0e34d2f591d8b0511de79a54c"
                           :registry-token "0x1642d08eb9d06e8142a8ab1c20b6476acbb2b039"
                           :amount (web3/to-wei 1 :ether)
                           :vote-option :vote.option/vote-against
                           :salt "a"}])

  (dispatch [::sync-now-events/increment-now 300])

  (dispatch [:reveal-vote {:registry-entry "0x8ce83cb07c9853d0e34d2f591d8b0511de79a54c"
                           :vote-option :vote.option/vote-against
                           :salt "a"}])

  )

(reg-event-fx
  :create-registry
  interceptors
  (fn [{:keys [db]} [args]]
    (println args)
    (println [(:registry/title args)
                    (:registry/description args)
                    (:registry/token-name args)
                    (:registry/token-symbol args)
                    (:registry/total-supply args)
                    (contract-q/contract-address db :minime-token-factory)
                    (:registry/challenge-period-duration args)
                    (:registry/commit-period-duration args)
                    (:registry/reveal-period-duration args)
                    (:registry/deposit args)])
    {:dispatch [::tx-events/send-tx {:instance (contract-q/instance db :registry-factory)
                                     :fn :create-registry
                                     :args [(:registry/title args)
                                            (:registry/description args)
                                            (:registry/token-name args)
                                            (:registry/token-symbol args)
                                            (:registry/total-supply args)
                                            (contract-q/contract-address db :minime-token-factory)
                                            (:registry/challenge-period-duration args)
                                            (:registry/commit-period-duration args)
                                            (:registry/reveal-period-duration args)
                                            (:registry/deposit args)]
                                     :tx-opts {:from (accounts-q/active-account db)
                                               :gas 6200000}
                                     :on-tx-success [:create-registry-success]
                                     :on-tx-hash-error [:create-registry-error]
                                     :on-tx-error [:create-registry-error]}]}))


(reg-event-fx
  :create-registry-success
  interceptors
  (fn [{:keys [db]} args]
    (console :log :create-registry-success args)
    nil))


(reg-event-fx
  :create-registry-error
  interceptors
  (fn [{:keys [db]} args]
    (console :error :create-registry-error args)
    nil))


(reg-event-fx
  :create-registry-entry
  interceptors
  (fn [{:keys [db]} [{:keys [:registry-entry-factory :registry-token :deposit :title :description]}]]
    (let [extra-data (web3-eth/contract-get-data (contract-q/instance db :registry-entry-factory)
                                                 :create-registry-entry
                                                 (accounts-q/active-account db)
                                                 title
                                                 description)]
      {:dispatch [::tx-events/send-tx {:instance (contract-q/instance db :registry-token registry-token)
                                       :fn :approve-and-call
                                       :args [registry-entry-factory
                                              deposit
                                              extra-data]
                                       :tx-opts {:from (accounts-q/active-account db)
                                                 :gas 3000000}
                                       :on-tx-success [:create-registry-entry-success]
                                       :on-tx-hash-error [:create-registry-entry-error]
                                       :on-tx-error [:create-registry-entry-error]}]})))


(reg-event-fx
  :create-registry-entry-success
  interceptors
  (fn [{:keys [db]} args]
    (console :log :create-registry-entry-success args)
    nil))


(reg-event-fx
  :create-registry-entry-error
  interceptors
  (fn [{:keys [db]} args]
    (console :error :create-registry-entry-error args)
    nil))


(reg-event-fx
  :create-challenge
  interceptors
  (fn [{:keys [db]} [{:keys [:registry-entry :registry-token :deposit :description]}]]
    (let [extra-data (web3-eth/contract-get-data (contract-q/instance db :registry-entry)
                                                 :create-challenge
                                                 (accounts-q/active-account db)
                                                 description)]
      {:dispatch [::tx-events/send-tx {:instance (contract-q/instance db :registry-token registry-token)
                                       :fn :approve-and-call
                                       :args [registry-entry
                                              deposit
                                              extra-data]
                                       :tx-opts {:from (accounts-q/active-account db)
                                                 :gas 3000000}
                                       :on-tx-success [:create-challenge-success]
                                       :on-tx-hash-error [:create-challenge-error]
                                       :on-tx-error [:create-challenge-error]}]})))


(reg-event-fx
  :create-challenge-success
  interceptors
  (fn [{:keys [db]} args]
    (console :log :create-challenge-success args)
    nil))


(reg-event-fx
  :create-challenge-error
  interceptors
  (fn [{:keys [db]} args]
    (console :error :create-challenge-error args)
    nil))


(reg-event-fx
  :commit-vote
  interceptors
  (fn [{:keys [db]} [{:keys [:registry-entry :registry-token :amount :vote-option :salt]}]]
    (let [extra-data (web3-eth/contract-get-data (contract-q/instance db :registry-entry)
                                                 :commit-vote
                                                 (accounts-q/active-account db)
                                                 amount
                                                 (solidity-sha3 (vote-option->num vote-option) salt))]
      {:dispatch [::tx-events/send-tx {:instance (contract-q/instance db :registry-token registry-token)
                                       :fn :approve-and-call
                                       :args [registry-entry
                                              amount
                                              extra-data]
                                       :tx-opts {:from (accounts-q/active-account db)
                                                 :gas 3000000}
                                       :on-tx-success [:commit-vote-success]
                                       :on-tx-hash-error [:commit-vote-error]
                                       :on-tx-error [:commit-vote-error]}]})))


(reg-event-fx
  :commit-vote-success
  interceptors
  (fn [{:keys [db]} args]
    (console :log :commit-vote-success args)
    nil))


(reg-event-fx
  :commit-vote-error
  interceptors
  (fn [{:keys [db]} args]
    (console :error :commit-vote-error args)
    nil))


(reg-event-fx
  :reveal-vote
  interceptors
  (fn [{:keys [db]} [{:keys [:registry-entry :vote-option :salt]}]]
    {:dispatch [::tx-events/send-tx {:instance (contract-q/instance db :registry-entry registry-entry)
                                     :fn :reveal-vote
                                     :args [(vote-option->num vote-option)
                                            salt]
                                     :tx-opts {:from (accounts-q/active-account db)
                                               :gas 3000000}
                                     :on-tx-success [:reveal-vote-success]
                                     :on-tx-hash-error [:reveal-vote-error]
                                     :on-tx-error [:reveal-vote-error]}]}))


(reg-event-fx
  :reveal-vote-success
  interceptors
  (fn [{:keys [db]} args]
    (console :log :reveal-vote-success args)
    nil))


(reg-event-fx
  :reveal-vote-error
  interceptors
  (fn [{:keys [db]} args]
    (console :error :reveal-vote-error args)
    nil))
