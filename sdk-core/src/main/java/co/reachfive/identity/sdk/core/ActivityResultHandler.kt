package co.reachfive.identity.sdk.core

import android.content.Intent
import co.reachfive.identity.sdk.core.models.AuthToken
import co.reachfive.identity.sdk.core.models.ReachFiveError
import co.reachfive.identity.sdk.core.utils.Failure
import co.reachfive.identity.sdk.core.utils.Success

sealed interface ActivityResultHandler

open class LoginResultHandler(
    private val reachFive: ReachFive,
    val requestCode: Int,
    val resultCode: Int,
    val intent: Intent?,
) : ActivityResultHandler {
    fun handle(success: Success<AuthToken>, failure: Failure<ReachFiveError>) {
        reachFive.onLoginActivityResult(requestCode, resultCode, intent, success, failure)
    }
}

open class WebauthnActionHandler(
    private val reachFive: ReachFive,
    val requestCode: Int,
    val intent: Intent?,
) : ActivityResultHandler {
    fun handle(success: Success<Unit>, failure: Failure<ReachFiveError>) {
        reachFive.onWebauthnDeviceAddResult(requestCode, intent, success, failure)
    }
}