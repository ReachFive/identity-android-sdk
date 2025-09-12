package co.reachfive.identity.sdk.core.models.responses

import co.reachfive.identity.sdk.core.models.AuthToken

sealed class SignupResponse {
    data class AchievedLogin(val authToken: AuthToken): SignupResponse()
    object AwaitingIdentifierVerification : SignupResponse()
}