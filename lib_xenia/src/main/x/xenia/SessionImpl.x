import collections.CircularArray;
import collections.IdentitySet;

import net.IPAddress;

import web.CookieConsent;
import web.Header;
import web.TrustLevel;
import web.codecs.Base64Format;

import HttpServer.RequestInfo;
import SessionCookie.CookieId;


/**
 * An implementation of the `Session` interface.
 *
 * TODO CP
 * - doc why is it not concurrent
 * - doc why it must not do any I/O
 */
service SessionImpl
        implements Session
    {
    // ----- constructors --------------------------------------------------------------------------

    /**
     * Construct a SessionImpl instance.
     *
     * @param manager      the SessionManager
     * @param sessionId    the internal session identifier
     * @param requestInfo  the request information
     */
    construct(SessionManager manager, Int sessionId, RequestInfo requestInfo)
        {
        initialize(this, manager, sessionId, requestInfo);
        }

    /**
     * Construction helper, used directly by the constructor, but also via reflection.
     *
     * @param structure    the structure of the SessionImpl being constructed
     * @param manager      the SessionManager
     * @param sessionId    the internal session identifier
     * @param requestInfo  the request information
     * @param tls          True if the request was received over a TLS connection
     */
    static void initialize(SessionImpl:struct structure,
                           SessionManager     manager,
                           Int                sessionId,
                           RequestInfo        requestInfo)
        {
        Time now = xenia.clock.now;

        structure.manager_        = manager;
        structure.created         = now;
        structure.lastUse         = now;
        structure.versionChanged_ = now;
        structure.ipAddress       = requestInfo.getClientAddress();
        structure.userAgent       = extractUserAgent(requestInfo);
        structure.cookieConsent   = None;
        structure.trustLevel      = None;
        structure.internalId_     = sessionId;
        structure.sessionId       = idToString_(sessionId);
        structure.prevTLS_        = requestInfo.tls;
        }


    // ----- session implementation properties -----------------------------------------------------

    /**
     * All of the `Session` objects within an application are managed by a `SessionManager`.
     */
    protected/private SessionManager manager_;

    /**
     * This flag tracks whether the session has been accessed via a TLS connection previously.
     */
    protected/private Boolean prevTLS_;

    /**
     * The internal session identity.
     */
    public/private Int internalId_;

    /**
     * The current version of the session. Each time a significant change occurs to the user agent,
     * such as cookie consent, IP address change, etc., the version of the session is incremented.
     */
    protected/private Int version_;

    /**
     * The time at which the version was last incremented. Tracking this information allows for
     * tolerance to concurrent requests which may already be in flight with older versions of the
     * session's cookies. Once the round trip verification of the updated session cookies has
     * completed, no new requests using the older cookies should be possible, but HTTP does not
     * guarantee strict ordering, and high request concurrency (including unpredictable delays for
     * some of those concurrent requests) can cause out of order processing of incoming requests.
     */
    protected/private Time versionChanged_;

    /**
     * A bitset of which `CookieId`s are known to have a cookie that has been created for this
     * session.
     */
    public/private Int knownCookies_ = CookieId.None;

    /**
     * The information tracked for each of the session cookies, indexed by the `CookieId.ordinal`.
     */
    protected/private SessionCookieInfo_?[] sessionCookieInfos_ = new SessionCookieInfo_?[CookieId.count];

    /**
     * A record of access to this session from a particular `IPAddress`.
     */
    protected static class AddressAccess_(IPAddress address, Time firstAccess, Time lastAccess);

    /**
     * A limited history by `IPAddress` of access to this session.
     */
    protected/private CircularArray<AddressAccess_> recentAddresses_ = new CircularArray(8);

    /**
     * All of the `IPAddress` that requests for this session have originated from.
     */
    protected/private Set<IPAddress> allAddresses_ = new HashSet();

    /**
     * If the session has been destroyed, it may contain "forwarding addresses" for the user agent
     * to follow.
     */
    protected/private SessionRename[] renamed_ = [];

    /**
     * A limited collection of recent requests that are held for debugging purposes and to allow a
     * management console to provide visibility to information that may help identify an application
     * level error.
     */
    protected/private CircularArray<Request> recentRequests_ = new CircularArray(8);

    /**
     * Internal recording of events related to the session, maintained for security and debugging
     * purposes. This information will be pruned to prevent unlimited size growth.
     */
    protected/private List<LogEntry> log_ = [];

    /**
     * A data structure to keep track of concurrently executing requests.
     *
     * Note: These are tracked primarily for debugging purposes; it is not possible through the
     * Session interface to query for the current request objects, but only for the count of current
     * requests.
     */
    protected/private IdentitySet<Request> requests_ = new IdentitySet(7);


    // ----- inner types ---------------------------------------------------------------------------

    /**
     * Information about a SessionCookie related to this session.
     *
     * * The [cookie] property holds the cookie data itself.
     * * The [sent] property indicates if the cookie has been sent to the user agent, and if so,
     *   when it was sent.
     * * The [verified] property indicates that the cookie was successfully received by the user
     *   agent, and if so, when it was verified as received.
     */
    protected static class SessionCookieInfo_(SessionCookie cookie, Time? sent=Null, Time? verified=Null, Time? expires=Null);

    /**
     * A record of the session renaming that caused TODO
     */
    protected static const SessionRename(IPAddress address, Int version, String newId);

    /**
     * Information collected for each log entry.
     */
    static const LogEntry(Time time, IPAddress address, Boolean tls, Int version, String text);


    // ----- session implementation API ------------------------------------------------------------

    void requestBegin_(Request request)
        {
        assert requests_.addIfAbsent(request);
        }

    void requestEnd_(Request request)
        {
        assert requests_.removeIfPresent(request);
        }


    // ----- session interface ---------------------------------------------------------------------

    @Override
    @Lazy(() -> new HashMap())
    public/private Map<String, Shareable> attributes;

    @Override
    public/private Time created;

    @Override
    public/private Time? destroyed;

    @Override
    public/private Int requestCount;

    @Override
    public Int activeRequests.get()
        {
        return requests_.size;
        }

    @Override
    public/private Time lastUse;

    @Override
    public/private IPAddress ipAddress;

    @Override
    public/private String userAgent;

    @Override
    Boolean exclusiveAgent;

    @Override
    CookieConsent cookieConsent;

    @Override
    public/private String? userId;

    @Override
    public/private Time? lastAuthenticated;

    @Override
    TrustLevel trustLevel;

    @Override
    public/private String sessionId;

    @Override
    void authenticate(String userId, Boolean exclusiveAgent)
        {
        this.userId            = userId;
        this.exclusiveAgent    = exclusiveAgent;
        this.lastAuthenticated = clock.now;
        }

    @Override
    void deauthenticate()
        {
        TODO
        }

    @Override
    void destroy()
        {
        TODO
        }

    @Override
    void sessionCreated()
        {
        // TODO on each of these events, the execution needs to reach this level, otherwise we need
        //      to issue a warning to the developer (but just the first time)
        //
        //      idea: use a mixin with state that tracks whether each method has been invoked, and
        //      then records when it gets to this point, so we can determine when a call is made to
        //      a session event and it doesn't get to the end of the event chain
        }

    @Override
    void sessionDestroyed()
        {
        }

    @Override
    void sessionMergedFrom(Session temp)
        {
        }

    @Override
    void sessionForked()
        {
        }

    @Override
    void sessionAuthenticated(String user)
        {
        }

    @Override
    void sessionDeauthenticated(String user)
        {
        }


    // ----- helpers -------------------------------------------------------------------------------

    /**
     * Obtain the specified cookie information.
     *
     * @param cookieId  which session cookie to ask for
     *
     * @return True if the specified session cookie exists
     * @return (conditional) the SessionCookie
     * @return (conditional) the time that the SessionCookie was sent to the user agent
     * @return (conditional) the time that the SessionCookie was first sent back from the user agent
     * @return (conditional) the time that the SessionCookie will be expired by the user agent
     */
    conditional (SessionCookie cookie,
                 Time?         sent,
                 Time?         verified,
                 Time?         expires  ) getCookie_(CookieId cookieId)
        {
        SessionCookieInfo_? info = sessionCookieInfos_[1 << cookieId.ordinal];
        return info == Null
                ? False
                : (True, info.cookie, info.sent, info.verified, info.expires);
        }

    /**
     * Make sure that the specified cookies exist.
     *
     * @param cookieId  which session cookie to ask for
     */
    void ensureCookies_(Int requiredCookies)
        {
        if (requiredCookies & knownCookies_ == requiredCookies)
            {
            return;
            }

        // cookies need to be created, which causes the session version to change
        knownCookies_ |= requiredCookies;
        incrementVersion_();
        }

    /**
     * Make sure that the specified cookies exist.
     *
     * @param cookieId  which session cookie to ask for
     */
    void incrementVersion_()
        {
        ++version_;

        assert knownCookies_ != 0;

        Boolean[] cookieSet = knownCookies_.toBooleanArray();
        Time now     = xenia.clock.now;
        Time expires = now + manager_.persistentCookieDuration;
        for (CookieId cookieId : CookieId.values)
            {
            Int i = cookieId.ordinal;
            if (cookieSet[i])
                {
                // create the session cookie
                SessionCookie cookie = new SessionCookie(internalId_, cookieId, knownCookies_,
                        cookieConsent, cookieId.persistent ? expires : Null, ipAddress, now);
                sessionCookieInfos_[i] = new SessionCookieInfo_(cookie);
                }
            }
        }

    /**
     * Convert an integer session id to a generic-looking (external) string identifier.
     *
     * @param the internal session id
     *
     * @return the session identifier string
     */
    static String idToString_(Int id)
        {
        Byte[] bytes = id == 0 ? [0] : id.toByteArray()[id.leadingZeroCount / 8 ..< 8];
        return Base64Format.Instance.encode(bytes);
        }

    /**
     * Convert a generic-looking (external) session id string to an implementation-specific internal
     * `Int` id value.
     *
     * @param str  the session identifier string
     *
     * @return the internal session id
     */
    static Int stringToId_(String str)
        {
        // the string contains a base-64 encoded set of up to 8 bytes, which are the 8 bytes of the
        // int session id
        Byte[] bytes = Base64Format.Instance.decode(str);

        // note: we need to take care to never communicate the exception text back to the user
        //       agent; this information should only be used on the server for debugging purposes
        assert bytes.size <= 8 as $"Invalid session {str.quoted()}, byte length={bytes.size}";

        return bytes.toInt64();
        }
    }