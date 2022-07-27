package co.reachfive.identity.sdk.core

import android.app.Activity
import android.content.Intent
import co.reachfive.identity.sdk.core.models.AuthToken
import co.reachfive.identity.sdk.core.models.Profile
import co.reachfive.identity.sdk.core.models.ReachFiveError
import co.reachfive.identity.sdk.core.models.SdkConfig
import co.reachfive.identity.sdk.core.models.requests.ProfileSignupRequest
import co.reachfive.identity.sdk.core.models.requests.UpdatePasswordRequest
import co.reachfive.identity.sdk.core.utils.Callback

class JavaReachFive(
    activity: Activity,
    sdkConfig: SdkConfig,
    providersCreators: List<ProviderCreator>
) {
    private val reach5 = ReachFive(activity, sdkConfig, providersCreators)

    fun isReachFiveLoginRequestCode(code: Int): Boolean =
        reach5.isReachFiveLoginRequestCode(code)

    fun isReachFiveActionRequestCode(code: Int): Boolean =
        reach5.isReachFiveActionRequestCode(code)

    fun initialize(
        success: Callback<List<Provider>>,
        failure: Callback<ReachFiveError>
    ): ReachFive {
        return reach5.initialize(success::call, failure::call)
    }

    fun getProvider(name: String): Provider? {
        return reach5.getProvider(name)
    }

    fun getProviders(): List<Provider> {
        return reach5.getProviders()
    }

    fun loginWithProvider(
        name: String,
        scope: Collection<String>,
        origin: String,
        activity: Activity
    ) {
        return reach5.loginWithProvider(name, scope, origin, activity)
    }

    fun loginWithProvider(name: String, origin: String, activity: Activity) {
        return reach5.loginWithProvider(name, reach5.defaultScope, origin, activity)
    }

    /**
     * Sign-up with required scopes
     */
    fun signup(
        profile: ProfileSignupRequest,
        scope: Collection<String>,
        redirectUrl: String? = null,
        success: Callback<AuthToken>,
        failure: Callback<ReachFiveError>
    ) {
        return reach5.signup(profile, scope, redirectUrl, success::call, failure::call)
    }

    /**
     * Sign-up with no required scopes (needed by the Java API)
     */
    fun signup(
        profile: ProfileSignupRequest,
        success: Callback<AuthToken>,
        failure: Callback<ReachFiveError>
    ) {
        return reach5.signup(profile, success = success::call, failure = failure::call)
    }

    /**
     * Passwordless
     */
    fun startPasswordless(
        email: String? = null,
        phoneNumber: String? = null,
        redirectUri: String,
        successWithNoContent: Callback<Unit>,
        failure: Callback<ReachFiveError>
    ) {
        reach5.startPasswordless(
            email,
            phoneNumber,
            redirectUri,
            { successWithNoContent.call(Unit) },
            failure::call
        )
    }

    fun verifyPasswordless(
        phoneNumber: String,
        verificationCode: String,
        success: Callback<AuthToken>,
        failure: Callback<ReachFiveError>
    ) {
        reach5.verifyPasswordless(phoneNumber, verificationCode, success::call, failure::call)
    }

    /**
     * Login with required scopes
     * @param username You can use email or phone number
     */
    fun loginWithPassword(
        username: String,
        password: String,
        scope: Collection<String>,
        success: Callback<AuthToken>,
        failure: Callback<ReachFiveError>
    ) {
        return reach5.loginWithPassword(username, password, scope, success::call, failure::call)
    }

    /**
     * Login with no required scopes (needed by the Java API)
     * @param username You can use email or phone number
     */
    fun loginWithPassword(
        username: String,
        password: String,
        success: Callback<AuthToken>,
        failure: Callback<ReachFiveError>
    ) {
        return reach5.loginWithPassword(
            username,
            password,
            success = success::call,
            failure = failure::call
        )
    }

    fun loginWithWeb(
        scope: Collection<String>,
        state: String? = null,
        nonce: String? = null,
        origin: String? = null,
    ) {
        return reach5.loginWithWeb(scope, state, nonce, origin)
    }

    fun logout(
        successWithNoContent: Callback<Unit>,
        failure: Callback<ReachFiveError>
    ) {
        return reach5.logout({ successWithNoContent.call(Unit) }, failure::call)
    }

    fun getProfile(
        authToken: AuthToken,
        success: Callback<Profile>,
        failure: Callback<ReachFiveError>
    ) {
        return reach5.getProfile(authToken, success::call, failure::call)
    }

    fun verifyPhoneNumber(
        authToken: AuthToken,
        phoneNumber: String,
        verificationCode: String,
        successWithNoContent: Callback<Unit>,
        failure: Callback<ReachFiveError>
    ) {
        return reach5.verifyPhoneNumber(
            authToken,
            phoneNumber,
            verificationCode,
            { successWithNoContent.call(Unit) },
            failure::call
        )
    }

    fun updateEmail(
        authToken: AuthToken,
        email: String,
        redirectUrl: String? = null,
        success: Callback<Profile>,
        failure: Callback<ReachFiveError>
    ) {
        return reach5.updateEmail(authToken, email, redirectUrl, success::call, failure::call)
    }

    fun updatePhoneNumber(
        authToken: AuthToken,
        phoneNumber: String,
        success: Callback<Profile>,
        failure: Callback<ReachFiveError>
    ) {
        return reach5.updatePhoneNumber(authToken, phoneNumber, success::call, failure::call)
    }

    fun updateProfile(
        authToken: AuthToken,
        profile: Profile,
        success: Callback<Profile>,
        failure: Callback<ReachFiveError>
    ) {
        return reach5.updateProfile(authToken, profile, success::call, failure::call)
    }

    fun updatePassword(
        updatePasswordRequest: UpdatePasswordRequest,
        successWithNoContent: Callback<Unit>,
        failure: Callback<ReachFiveError>
    ) {
        return reach5.updatePassword(
            updatePasswordRequest,
            { successWithNoContent.call(Unit) },
            failure::call
        )
    }

    fun requestPasswordReset(
        email: String?,
        redirectUrl: String?,
        phoneNumber: String?,
        successWithNoContent: Callback<Unit>,
        failure: Callback<ReachFiveError>
    ) {
        return reach5.requestPasswordReset(
            email,
            redirectUrl,
            phoneNumber,
            { successWithNoContent.call(Unit) },
            failure::call
        )
    }

    fun refreshAccessToken(
        authToken: AuthToken,
        success: Callback<AuthToken>,
        failure: Callback<ReachFiveError>
    ) {
        return reach5.refreshAccessToken(
            authToken,
            success::call,
            failure::call
        )
    }

    fun onWebauthnDeviceAddResult(
        requestCode: Int,
        data: Intent?,
        success: Callback<Unit>,
        failure: Callback<ReachFiveError>,
    ) {
        return reach5.onWebauthnDeviceAddResult(requestCode, data, success::call, failure::call)
    }

    fun onLoginActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        success: Callback<AuthToken>,
        failure: Callback<ReachFiveError>,
    ) {
        return reach5.onLoginActivityResult(requestCode, resultCode, data, success::call, failure::call)
    }

    fun onStop() {
        reach5.onStop()
    }
}
