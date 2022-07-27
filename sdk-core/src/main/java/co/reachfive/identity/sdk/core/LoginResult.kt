package co.reachfive.identity.sdk.core

enum class LoginResult(val code: Int) {

    UNEXPECTED_ERROR(-1),
    SUCCESS(0),
    NO_AUTHORIZATION_CODE(2),
}