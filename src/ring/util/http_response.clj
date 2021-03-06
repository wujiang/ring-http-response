(ns ring.util.http-response
  (:require [camel-snake-kebab :refer [->kebab-case]]
            [potemkin :refer [import-vars]]
            [slingshot.slingshot :refer [throw+]]
            ring.util.response))

;; original version: https://github.com/spray/spray/blob/master/spray-http/src/main/scala/spray/http/StatusCode.scala
;; original Copyright © 2011-2013 the spray project <http://spray.io>

(defrecord StatusType [name entity? location? success?])

(defn get-type [status]
  (cond
    (<= 100 status 199) (->StatusType "Informational" false false true)
    (<= 200 status 299) (->StatusType "Success"       true  false true)
    (<= 300 status 399) (->StatusType "Redirection"   false true  true)
    (<= 400 status 499) (->StatusType "ClientError"   true  false false)
    (<= 500 status 599) (->StatusType "ServerError"   true  false false)))

(defn throw!
  "Slingshots an exception with :type :ring.util.http-response/response and the full response in :response"
  [response]
  {:pre (map? response)}
  (throw+ {:type ::response :response response}))

(defmacro defstatus [class-name status name description & [options]]
  (let [{:keys [name entity? location? success?]} (merge (get-type status) options)
        docstring (str status " " class-name " (" name ")\n\n" description)
        fn-name (->kebab-case class-name)
        parameters (cond
                     entity?   ['body]
                     location? ['url]
                     :else     [])
        body (merge
               {:status  status
                :headers {}
                :body    ""}
               (cond
                 entity?   {:body 'body}
                 location? {:headers {"Location" 'url}
                            :body    `(str "<a href=\"" ~'url "\">" ~'url "</a>")}
                 :else     nil))]
    `(do
       (defn ~fn-name ~docstring ~parameters ~body)
       ~(if-not success?
          `(defn ~(symbol (str fn-name "!"))
                 ~(str docstring "\n\nSlingshota an exception with :type :ring.util.http-response/response and the full response in :response")
                 ~parameters
                 (throw! ~body))))))

(defstatus Continue                      100 "Continue" "The server has received the request headers and the client should proceed to send the request body.")
(defstatus SwitchingProtocols            101 "Switching Protocols" "The server is switching protocols because the client requested the switch.")
(defstatus Processing                    102 "Processing" "The server is processing the request but no response is available yet.")

(defstatus OK                            200 "OK" "OK")
(defstatus Created                       201 "Created" "The request has been fulfilled and resulted in a new resource being created.")
(defstatus Accepted                      202 "Accepted" "The request has been accepted for processing but the processing has not been completed.")
(defstatus NonAuthoritativeInformation   203 "Non-Authoritative Information" "The server successfully processed the request but is returning information that may be from another source.")
(defstatus NoContent                     204 "No Content" "" {:entity? false})
(defstatus ResetContent                  205 "Reset Content" "The server successfully processed the request but is not returning any content.")
(defstatus PartialContent                206 "Partial Content" "The server is delivering only part of the resource due to a range header sent by the client.")
(defstatus MultiStatus                   207 "Multi-Status" "The message body that follows is an XML message and can contain a number of separate response codes depending on how many sub-requests were made.")
(defstatus AlreadyReported               208 "Already Reported" "The members of a DAV binding have already been enumerated in a previous reply to this request and are not being included again.")
(defstatus IMUsed                        226 "IM Used" "The server has fulfilled a GET request for the resource and the response is a representation of the result of one or more instance-manipulations applied to the current instance.")

(defstatus MultipleChoices               300 "Multiple Choices" "There are multiple options for the resource that the client may follow." #_"There are multiple options for the resource that the client may follow. The preferred one is <a href=\"%s\">this URI</a>.")
(defstatus MovedPermanently              301 "Moved Permanently" "This and all future requests should be directed to the given URI." #_"This and all future requests should be directed to <a href=\"%s\">this URI</a>.")
(defstatus Found                         302 "Found" "The resource was found but at a different URI." #_"The requested resource temporarily resides under <a href=\"%s\">this URI</a>.")
(defstatus SeeOther                      303 "See Other" "The response to the request can be found under another URI using a GET method." #_"The response to the request can be found under <a href=\"%s\">this URI</a> using a GET method.")
(defstatus NotModified                   304 "Not Modified" "The resource has not been modified since last requested." #_"" {:entity? false})
(defstatus UseProxy                      305 "Use Proxy" "This single request is to be repeated via the proxy given by the Location field." #_"This single request is to be repeated via the proxy under <a href=\"%s\">this URI</a>.")
(defstatus TemporaryRedirect             307 "Temporary Redirect" "The request should be repeated with another URI but future requests can still use the original URI." #_"The request should be repeated with <a href=\"%s\">this URI</a> but future requests can still use the original URI.")
(defstatus PermanentRedirect             308 "Permanent Redirect" "The request and all future requests should be repeated using another URI." #_"The request and all future requests should be repeated using <a href=\"%s\">this URI</a>.")

(defstatus BadRequest                    400 "Bad Request" "The request contains bad syntax or cannot be fulfilled.")
(defstatus Unauthorized                  401 "Unauthorized" "Authentication is possible but has failed or not yet been provided.")
(defstatus PaymentRequired               402 "Payment Required" "Reserved for future use.")
(defstatus Forbidden                     403 "Forbidden" "The request was a legal request but the server is refusing to respond to it.")
(defstatus NotFound                      404 "Not Found" "The requested resource could not be found but may be available again in the future.")
(defstatus MethodNotAllowed              405 "Method Not Allowed" "A request was made of a resource using a request method not supported by that resource;")
(defstatus NotAcceptable                 406 "Not Acceptable" "The requested resource is only capable of generating content not acceptable according to the Accept headers sent in the request.")
(defstatus ProxyAuthenticationRequired   407 "Proxy Authentication Required" "Proxy authentication is required to access the requested resource.")
(defstatus RequestTimeout                408 "Request Timeout" "The server timed out waiting for the request.")
(defstatus Conflict                      409 "Conflict" "The request could not be processed because of conflict in the request such as an edit conflict.")
(defstatus Gone                          410 "Gone" "The resource requested is no longer available and will not be available again.")
(defstatus LengthRequired                411 "Length Required" "The request did not specify the length of its content which is required by the requested resource.")
(defstatus PreconditionFailed            412 "Precondition Failed" "The server does not meet one of the preconditions that the requester put on the request.")
(defstatus RequestEntityTooLarge         413 "Request Entity Too Large" "The request is larger than the server is willing or able to process.")
(defstatus RequestUriTooLong             414 "Request-URI Too Long" "The URI provided was too long for the server to process.")
(defstatus UnsupportedMediaType          415 "Unsupported Media Type" "The request entity has a media type which the server or resource does not support.")
(defstatus RequestedRangeNotSatisfiable  416 "Requested Range Not Satisfiable" "The client has asked for a portion of the file but the server cannot supply that portion.")
(defstatus ExpectationFailed             417 "Expectation Failed" "The server cannot meet the requirements of the Expect request-header field.")
(defstatus EnhanceYourCalm               420 "Enhance Your Calm" "You are being rate-limited.") ;; Twitter only
(defstatus UnprocessableEntity           422 "Unprocessable Entity" "The request was well-formed but was unable to be followed due to semantic errors.")
(defstatus Locked                        423 "Locked" "The resource that is being accessed is locked.")
(defstatus FailedDependency              424 "Failed Dependency" "The request failed due to failure of a previous request.")
(defstatus UnorderedCollection           425 "Unordered Collection" "The collection is unordered.")
(defstatus UpgradeRequired               426 "Upgrade Required" "The client should switch to a different protocol.")
(defstatus PreconditionRequired          428 "Precondition Required" "The server requires the request to be conditional.")
(defstatus TooManyRequests               429 "Too Many Requests" "The user has sent too many requests in a given amount of time.")
(defstatus RequestHeaderFieldsTooLarge   431 "Request Header Fields Too Large" "The server is unwilling to process the request because either an individual header field or all the header fields collectively are too large.")
(defstatus RetryWith                     449 "Retry With" "The request should be retried after doing the appropriate action.")
(defstatus BlockedByParentalControls     450 "Blocked by Windows Parental Controls" "Windows Parental Controls are turned on and are blocking access to the given webpage.")
(defstatus UnavailableForLegalReasons    451 "Unavailable For Legal Reasons" "Resource access is denied for legal reasons.")

(defstatus InternalServerError           500 "Internal Server Error" "There was an internal server error.")
(defstatus NotImplemented                501 "Not Implemented" "The server either does not recognize the request method or it lacks the ability to fulfill the request.")
(defstatus BadGateway                    502 "Bad Gateway" "The server was acting as a gateway or proxy and received an invalid response from the upstream server.")
(defstatus ServiceUnavailable            503 "Service Unavailable" "The server is currently unavailable (because it is overloaded or down for maintenance).")
(defstatus GatewayTimeout                504 "Gateway Timeout" "The server was acting as a gateway or proxy and did not receive a timely request from the upstream server.")
(defstatus HTTPVersionNotSupported       505 "HTTP Version Not Supported" "The server does not support the HTTP protocol version used in the request.")
(defstatus VariantAlsoNegotiates         506 "Variant Also Negotiates" "Transparent content negotiation for the request results in a circular reference.")
(defstatus InsufficientStorage           507 "Insufficient Storage" "Insufficient storage to complete the request.")
(defstatus LoopDetected                  508 "Loop Detected" "The server detected an infinite loop while processing the request.")
(defstatus BandwidthLimitExceeded        509 "Bandwidth Limit Exceeded" "Bandwidth limit has been exceeded.")
(defstatus NotExtended                   510 "Not Extended" "Further extensions to the request are required for the server to fulfill it.")
(defstatus NetworkAuthenticationRequired 511 "Network Authentication Required" "The client needs to authenticate to gain network access.")
(defstatus NetworkReadTimeout            598 "Network read timeout error" "")
(defstatus NetworkConnectTimeout         599 "Network connect timeout error" "")

(import-vars

  ;; rest of the public interface at ring.util.response
  [ring.util.response

   status
   header
   file-response
   content-type
   charset
   set-cookie
   response?
   url-response
   resource-response
   get-header])

