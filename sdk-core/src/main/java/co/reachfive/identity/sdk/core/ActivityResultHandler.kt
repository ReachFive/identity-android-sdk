package co.reachfive.identity.sdk.core

import android.app.Activity
import android.content.Intent
import co.reachfive.identity.sdk.core.models.AuthToken
import co.reachfive.identity.sdk.core.models.ReachFiveError
import co.reachfive.identity.sdk.core.utils.Failure
import co.reachfive.identity.sdk.core.utils.Success
import co.reachfive.identity.sdk.core.utils.SuccessWithNoContent

sealed interface ActivityResultHandler

open class LoginResultHandler(
    private val reachFive: ReachFive,
    val requestCode: Int,
    val resultCode: Int,
    val intent: Intent?,
) : ActivityResultHandler {
    fun handle(success: Success<AuthToken>, failure: Failure<ReachFiveError>, activity: Activity) {
        reachFive.onLoginActivityResult(
            requestCode,
            resultCode,
            intent,
            loginSuccess = success,
            failure = failure,
            activity = activity
        )
    }
}

open class WebLogoutHandler(
    private val reachFive: ReachFive,
    private val requestCode: Int,
) : ActivityResultHandler {
    fun handle(success: SuccessWithNoContent<Unit>) {
        reachFive.onWebLogoutActivityResult(requestCode, success)
    }
}

sealed interface WebauthnActionHandler : ActivityResultHandler

open class WebAuthnDeviceAddResult(
    private val reachFive: ReachFive,
    val requestCode: Int,
    val intent: Intent?,
) : WebauthnActionHandler {
    fun handle(success: Success<Unit>, failure: Failure<ReachFiveError>) {
        reachFive.onWebauthnDeviceAddResult(requestCode, intent, success, failure)
    }
}