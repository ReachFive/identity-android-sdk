package co.reachfive.identity.sdk.core

import android.content.Intent
import co.reachfive.identity.sdk.core.RedirectionActivity.Companion.Result
import co.reachfive.identity.sdk.core.api.ReachFiveApi
import co.reachfive.identity.sdk.core.api.ReachFiveApiCallback
import co.reachfive.identity.sdk.core.models.AuthToken
import co.reachfive.identity.sdk.core.models.ReachFiveError
import co.reachfive.identity.sdk.core.models.SdkConfig
import co.reachfive.identity.sdk.core.models.SdkInfos
import co.reachfive.identity.sdk.core.models.requests.AuthCodeRequest
import co.reachfive.identity.sdk.core.utils.Failure
import co.reachfive.identity.sdk.core.utils.Success

class RedirectionActivityResultHandler(
    private val sdkConfig: SdkConfig,
    private val reachFiveApi: ReachFiveApi,
) {
    fun handle(
        resultCode: Int,
        intent: Intent?,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>
    ) {
        val result = Result.valueOf(resultCode.toString())
        if (result == Result.ReachFiveError || result == Result.UnexpectedError) {
            val error = intent?.getParcelableExtra<ReachFiveError>(ReachFiveError.INTENT_EXTRA_KEY)
            if (error != null) failure(error)
            else failure(ReachFiveError.Unexpected) // TODO
        } else if (result == Result.Success && intent != null) {
            val code = intent.getStringExtra(RedirectionActivity.CODE_KEY)!! // TODO/cbu
            val codeVerifier = intent.getStringExtra(RedirectionActivity.CODE_VERIFIER_KEY)!!

            val authCodeRequest = AuthCodeRequest(
                clientId = sdkConfig.clientId,
                code = code,
                redirectUri = sdkConfig.scheme,
                codeVerifier = codeVerifier
            )

            reachFiveApi
                .authenticateWithCode(authCodeRequest, SdkInfos.getQueries())
                .enqueue(
                    ReachFiveApiCallback(
                        success = { it.toAuthToken().fold(success, failure) },
                        failure = failure
                    )
                )
        } else {
            failure(ReachFiveError.NoIntent)
        }
    }
}