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

internal class PasswordlessAuthClient(
    private val reachFiveApi: ReachFiveApi,
    override val sdkConfig: SdkConfig,
) : PasswordlessAuth {

    override fun startPasswordless(
        email: String?,
        phoneNumber: String?,
        redirectUrl: String,
        successWithNoContent: SuccessWithNoContent,
        failure: Failure<ReachFiveError>,
        activity: Activity,
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
                    responseType = SessionUtilsClient.codeResponseType,
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

    override fun verifyPasswordless(
        phoneNumber: String,
        verificationCode: String,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>,
        activity: Activity,
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

internal interface PasswordlessAuth {
    val sdkConfig: SdkConfig

    fun startPasswordless(
        email: String? = null,
        phoneNumber: String? = null,
        redirectUrl: String = sdkConfig.scheme,
        successWithNoContent: SuccessWithNoContent,
        failure: Failure<ReachFiveError>,
        activity: Activity
    )

    fun verifyPasswordless(
        phoneNumber: String,
        verificationCode: String,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>,
        activity: Activity
    )
}