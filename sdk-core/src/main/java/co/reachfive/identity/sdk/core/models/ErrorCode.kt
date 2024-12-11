package co.reachfive.identity.sdk.core.models

enum class ErrorCode(val code: Int) {
    /* OAuth */
    OAuthAuthorizationError(303),

    /* API error codes */
    BadRequest(400),
    Unauthorized(401),
    Forbidden(403),
    NotFound(404),
    Conflict(409),
    TooManyRequests(429),
    InternalServerError(500),
    NotImplemented(501),
    BadGateway(502),
    ServiceUnavailable(503),
    GatewayTimeout(504),

    /* SDK error codes */
    Unexpected(49000),
    WebauthnActionCanceled(52000),
    WebFlowCanceled(52001),
    NullIntent(52002),
    NoAuthCode(52003),
    NoPkce(52004);

    companion object {
        private val map = ErrorCode.values().associateBy { it.code }
        fun from(code: Int): ErrorCode? = map[code]
    }

}