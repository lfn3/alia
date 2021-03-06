(ns qbits.alia.cluster-options
  (:require
   [qbits.alia.enum :as enum]
   [clojure.java.io :as io]
   [qbits.alia.timestamp-generator :as tsg]
   [qbits.alia.policy.address-translater :as at])
  (:import
   (com.datastax.driver.core
    Cluster$Builder
    HostDistance
    PoolingOptions
    ProtocolOptions$Compression
    QueryOptions
    SocketOptions
    SSLOptions
    TimestampGenerator)
   (com.datastax.driver.core.policies
    AddressTranslater
    LoadBalancingPolicy
    ReconnectionPolicy
    RetryPolicy
    SpeculativeExecutionPolicy)
   (com.datastax.driver.auth DseAuthProvider)
   (javax.net.ssl
    TrustManagerFactory
    KeyManagerFactory
    SSLContext)
   (java.security KeyStore)))

(defmulti set-cluster-option! (fn [k ^Cluster$Builder builder option] k))

(defmethod set-cluster-option! :contact-points
  [_ builder hosts]
  (.addContactPoints ^Cluster$Builder builder
                     ^"[Ljava.lang.String;"
                     (into-array (if (sequential? hosts) hosts [hosts]))))

(defmethod set-cluster-option! :port
  [_ builder port]
  (.withPort ^Cluster$Builder builder (int port)))

(defmethod set-cluster-option! :load-balancing-policy
  [_ ^Cluster$Builder builder ^LoadBalancingPolicy policy]
  (.withLoadBalancingPolicy builder policy))

(defmethod set-cluster-option! :reconnection-policy
  [_ ^Cluster$Builder builder ^ReconnectionPolicy policy]
  (.withReconnectionPolicy builder policy))

(defmethod set-cluster-option! :retry-policy
  [_ ^Cluster$Builder builder ^RetryPolicy policy]
  (.withRetryPolicy builder policy))

(defmethod set-cluster-option! :speculative-execution-policy
  [_ ^Cluster$Builder builder ^SpeculativeExecutionPolicy policy]
  (.withSpeculativeExecutionPolicy builder policy))

(defmethod set-cluster-option! :pooling-options
  [_ ^Cluster$Builder builder {:keys [core-connections-per-host
                                      max-connections-per-host
                                      max-simultaneous-requests-per-connection
                                      min-simultaneous-requests-per-connection]
                               :as pooling-options}]
  (doseq [[opt x] pooling-options]
    (set-cluster-option! opt builder x))
  builder)

(defn ^PoolingOptions ^:no-doc pooling-options
  [^Cluster$Builder builder]
  (-> builder .getConfiguration .getPoolingOptions))

(defmethod set-cluster-option! :core-connections-per-host
  [_ ^Cluster$Builder builder core-connections-per-host]
  (let [po (pooling-options builder)]
    (doseq [[dist value] core-connections-per-host]
      (.setCoreConnectionsPerHost po (enum/host-distance dist) (int value)))
    builder))

(defmethod set-cluster-option! :max-connections-per-host
  [_ ^Cluster$Builder builder max-connections-per-host]
  (let [po (pooling-options builder)]
    (doseq [[dist value] max-connections-per-host]
      (.setMaxConnectionsPerHost po (enum/host-distance dist) (int value)))
    builder))

(defmethod set-cluster-option! :max-simultaneous-requests-per-connection
  [_ ^Cluster$Builder builder max-simultaneous-requests-per-connection]
  (let [po (pooling-options builder)]
    (doseq [[dist value] max-simultaneous-requests-per-connection]
      (.setMaxSimultaneousRequestsPerConnectionThreshold po (enum/host-distance dist)
                                                         (int value)))
    builder))

(defmethod set-cluster-option! :min-simultaneous-requests-per-connection
  [_ ^Cluster$Builder builder min-simultaneous-requests-per-connection]
  (let [po (pooling-options builder)]
    (doseq [[dist value] min-simultaneous-requests-per-connection]
      (.setMinSimultaneousRequestsPerConnectionThreshold po (enum/host-distance dist)
                                                         (int value)))
    builder))

(defn ^SocketOptions ^:no-doc socket-options
  [^Cluster$Builder builder]
  (-> builder .getConfiguration .getSocketOptions))

(defmethod set-cluster-option! :connect-timeout-millis
  [_ ^Cluster$Builder builder connect-timeout-millis]
  (-> builder socket-options (.setConnectTimeoutMillis (int connect-timeout-millis)))
  builder)

(defmethod set-cluster-option! :read-timeout-millis
  [_ ^Cluster$Builder builder read-timeout-millis]
  (-> builder socket-options (.setReadTimeoutMillis (int read-timeout-millis)))
  builder)

(defmethod set-cluster-option! :receive-buffer-size
  [_ ^Cluster$Builder builder receive-buffer-size]
  (-> builder socket-options (.setReceiveBufferSize (int receive-buffer-size)))
  builder)

(defmethod set-cluster-option! :send-buffer-size
  [_ ^Cluster$Builder builder send-buffer-size]
  (-> builder socket-options (.setSendBufferSize (int send-buffer-size)))
  builder)

(defmethod set-cluster-option! :so-linger
  [_ ^Cluster$Builder builder so-linger]
  (-> builder socket-options (.setSoLinger (int so-linger)))
  builder)

(defmethod set-cluster-option! :tcp-no-delay?
  [_ ^Cluster$Builder builder tcp-no-delay?]
  (-> builder socket-options (.setTcpNoDelay (boolean tcp-no-delay?)))
  builder)

(defmethod set-cluster-option! :reuse-address?
  [_ ^Cluster$Builder builder reuse-address?]
  (-> builder socket-options (.setReuseAddress (boolean reuse-address?)))
  builder)

(defmethod set-cluster-option! :keep-alive?
  [_ ^Cluster$Builder builder keep-alive?]
  (-> builder socket-options (.setKeepAlive (boolean keep-alive?)))
  builder)

(defmethod set-cluster-option! :socket-options
  [_ ^Cluster$Builder builder {:keys [connect-timeout-millis
                                      read-timeout-millis
                                      receive-buffer-size
                                      send-buffer-size
                                      so-linger
                                      tcp-no-delay?
                                      reuse-address?
                                      keep-alive?]
                               :as socket-options}]
  (doseq [[opt x] socket-options]
    (set-cluster-option! opt builder x))
  builder)

(defn ^QueryOptions ^:no-doc query-options
  [^Cluster$Builder builder]
  (-> builder .getConfiguration .getQueryOptions))

(defmethod set-cluster-option! :fetch-size
  [_ ^Cluster$Builder builder fetch-size]
  (-> builder query-options (.setFetchSize (int fetch-size)))
  builder)

(defmethod set-cluster-option! :consistency
  [_ ^Cluster$Builder builder consistency]
  (-> builder query-options (.setConsistencyLevel (enum/consistency-level consistency)))
  builder)

(defmethod set-cluster-option! :serial-consistency
  [_ ^Cluster$Builder builder serial-consistency]
  (-> builder query-options (.setSerialConsistencyLevel (enum/consistency-level serial-consistency)))
  builder)


(defmethod set-cluster-option! :query-options
  [_ ^Cluster$Builder builder {:keys [fetch-size
                                      consistency
                                      serial-consistency]
                               :as query-options}]
  (doseq [[opt x] query-options]
    (set-cluster-option! opt builder x))
  builder)

(defmethod set-cluster-option! :metrics?
  [_ ^Cluster$Builder builder metrics?]
  (when (not metrics?)
    (.withoutMetrics builder))
  builder)

(defmethod set-cluster-option! :jmx-reporting?
  [_ ^Cluster$Builder builder jmx-reporting?]
  (when (not jmx-reporting?)
    (.withoutJMXReporting builder))
  builder)

(defmethod set-cluster-option! :credentials
  [_ ^Cluster$Builder builder {:keys [user password]}]
  (.withCredentials builder user password))

(defmethod set-cluster-option! :kerberos?
  [_ ^Cluster$Builder builder kerberos?]
  (when kerberos?
    (.withAuthProvider builder (DseAuthProvider.)))
  builder)

(defmethod set-cluster-option! :compression
  [_ ^Cluster$Builder builder option]
  (.withCompression builder (enum/compression option)))

(defmethod set-cluster-option! :ssl?
  [_ ^Cluster$Builder builder ssl?]
  (when ssl? (.withSSL builder)))

(defmethod set-cluster-option! :ssl-options
  [_ ^Cluster$Builder builder ssl-options]
  (.withSSL builder
            (if (instance? ssl-options SSLOptions)
              ssl-options
              ;; Following part is inspired by
              ;; http://www.datastax.com/dev/blog/accessing-secure-dse-clusters-with-cql-native-protocolker
              ;; and
              ;; https://github.com/clojurewerkz/cassaforte/pull/60
              (let [{:keys [keystore-path keystore-password cipher-suites]} ssl-options
                    keystore (KeyStore/getInstance "JKS")
                    ssl-context (SSLContext/getInstance "SSL")
                    keymanager (KeyManagerFactory/getInstance (KeyManagerFactory/getDefaultAlgorithm))
                    trustmanager (TrustManagerFactory/getInstance (TrustManagerFactory/getDefaultAlgorithm))
                    password (char-array keystore-password)]
                (.load keystore (io/input-stream keystore-path) password)
                (.init keymanager keystore password)
                (.init trustmanager keystore)
                (.init ssl-context
                       (.getKeyManagers keymanager)
                       (.getTrustManagers trustmanager) nil)
                (SSLOptions. ssl-context
                             (if cipher-suites
                               (into-array String cipher-suites)
                               SSLOptions/DEFAULT_SSL_CIPHER_SUITES))))))

(defmethod set-cluster-option! :timestamp-generator
  [_ ^Cluster$Builder builder ts-generator]
  (.withTimestampGenerator builder
                           (if (instance? TimestampGenerator)
                             ts-generator
                             (case ts-generator
                               :atomic-monotonic (tsg/atomic-monotonic)
                               :server-side (tsg/server-side)
                               :thread-local (tsg/thread-local))))
  builder)

(defmethod set-cluster-option! :address-translater
  [_ ^Cluster$Builder builder at]
  (.withAddressTranslater builder
                          (if (instance? AddressTranslater)
                            at
                            (case at
                              :ec2-multi-region (at/ec2-multi-region-address-translater))))
  builder)

(defmethod set-cluster-option! :netty-options
  [_ ^Cluster$Builder builder netty-options]
  (.withNettyOptions builder netty-options)
  builder)

(defmethod set-cluster-option! :max-schema-agreement-wait-seconds
  [_ ^Cluster$Builder builder max-schema-agreement-wait-seconds]
  (.withMaxSchemaAgreementWaitSeconds builder
                                      (int max-schema-agreement-wait-seconds))
  builder)

(defmethod set-cluster-option! :cluster-name
  [_ ^Cluster$Builder builder cluster-name]
  (.withClusterName builder (name cluster-name))
  builder)

(defn set-cluster-options!
  ^Cluster$Builder
  [^Cluster$Builder builder options]
  (reduce (fn [builder [k option]]
            (set-cluster-option! k builder option))
          builder
          options))
