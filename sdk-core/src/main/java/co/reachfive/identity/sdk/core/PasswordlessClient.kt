package co.reachfive.identity.sdk.core

import android.app.Activity
import co.reachfive.identity.sdk.core.api.ReachFiveApi
import co.reachfive.identity.sdk.core.api.ReachFiveApiCallback
import co.reachfive.identity.sdk.core.models.AuthToken
import co.reachfive.identity.sdk.core.models.ReachFiveError
import co.reachfive.identity.sdk.core.models.SdkConfig
import co.reachfive.identity.sdk.core.models.SdkInfos
import co.reachfive.identity.sdk.core.models.requests.AuthCodeRequest
import co.reachfive.identity.sdk.core.models.requests.PasswordlessStartRequest
import co.reachfive.identity.sdk.core.models.requests.PasswordlessVerificationRequest
import co.reachfive.identity.sdk.core.utils.Failure
import co.reachfive.identity.sdk.core.utils.PkceAuthCodeFlow
import co.reachfive.identity.sdk.core.utils.Success
import co.reachfive.identity.sdk.core.utils.SuccessWithNoContent

internal interface PasswordlessClient {
    val activity: Activity
    val sdkConfig: SdkConfig
    val reachFiveApi: ReachFiveApi

    fun startPasswordless(
        email: String? = null,
        phoneNumber: String? = null,
        redirectUrl: String = sdkConfig.scheme,
        successWithNoContent: SuccessWithNoContent<Unit>,
        failure: Failure<ReachFiveError>
    ) =
        PkceAuthCodeFlow.generate(redirectUrl).let { pkce ->
            PkceAuthCodeFlow.storeAuthCodeFlow(pkce, activity)
            reachFiveApi.requestPasswordlessStart(
                PasswordlessStartRequest(
                    clientId = sdkConfig.clientId,
                    email = email,
                    phoneNumber = phoneNumber,
                    codeChallenge = pkce.codeChallenge,
                    codeChallengeMethod = pkce.codeChallengeMethod,
                    responseType = ReachFive.codeResponseType,
                    redirectUri = redirectUrl
                ),
                SdkInfos.getQueries()
            ).enqueue(
                ReachFiveApiCallback(
                    successWithNoContent = successWithNoContent,
                    failure = failure
                )
            )
        }

    fun verifyPasswordless(
        phoneNumber: String,
        verificationCode: String,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>
    ) =
        reachFiveApi.requestPasswordlessVerification(
            PasswordlessVerificationRequest(phoneNumber, verificationCode),
            SdkInfos.getQueries()
        ).enqueue(
            ReachFiveApiCallback(
                success = { verificationResponse ->
                    val authCodeFlow = PkceAuthCodeFlow.readAuthCodeFlow(activity)
                    if (authCodeFlow != null) {
                        val authCodeRequest = AuthCodeRequest(
                            sdkConfig.clientId,
                            verificationResponse.authCode,
                            redirectUri = authCodeFlow.redirectUri,
                            codeVerifier = authCodeFlow.codeVerifier
                        )

                        reachFiveApi
                            .authenticateWithCode(authCodeRequest, SdkInfos.getQueries())
                            .enqueue(
                                ReachFiveApiCallback(
                                    success = { tokenResponse ->
                                        tokenResponse.toAuthToken().fold(success, failure)
                                    },
                                    failure = failure
                                )
                            )
                    } else failure(ReachFiveError.from("No PKCE challenge found in memory."))

                },
                failure = failure
            )
        )
}