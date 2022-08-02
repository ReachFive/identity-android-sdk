package co.reachfive.identity.sdk.core.models

enum class ErrorCode(val code: Int) {
    // OAuth
    OAuthAuthorizationError(303),

    /*
    API error codes
     */
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

    /*
    SDK error codes
     */
    WebFlowCanceled(52001),
    NullIntent(52002);
}