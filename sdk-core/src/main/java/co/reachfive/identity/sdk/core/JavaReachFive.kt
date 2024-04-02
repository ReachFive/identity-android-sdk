package co.reachfive.identity.sdk.core

import android.app.Activity
import android.content.Context
import android.content.Intent
import co.reachfive.identity.sdk.core.models.AuthToken
import co.reachfive.identity.sdk.core.models.CredentialType
import co.reachfive.identity.sdk.core.models.Profile
import co.reachfive.identity.sdk.core.models.ReachFiveError
import co.reachfive.identity.sdk.core.models.SdkConfig
import co.reachfive.identity.sdk.core.models.requests.ProfileSignupRequest
import co.reachfive.identity.sdk.core.models.requests.ProfileWebAuthnSignupRequest
import co.reachfive.identity.sdk.core.models.requests.UpdatePasswordRequest
import co.reachfive.identity.sdk.core.models.requests.webAuthn.WebAuthnLoginRequest
import co.reachfive.identity.sdk.core.utils.Callback

class JavaReachFive(
    sdkConfig: SdkConfig,
    providersCreators: List<ProviderCreator>,
) {
    private val reach5 = ReachFive(sdkConfig, providersCreators)

    fun isReachFiveLoginRequestCode(code: Int): Boolean =
        reach5.isReachFiveLoginRequestCode(code)

    fun isReachFiveActionRequestCode(code: Int): Boolean =
        reach5.isReachFiveActionRequestCode(code)

    fun initialize(
        success: Callback<Unit>,
        failure: Callback<ReachFiveError>
    ): ReachFive {
        return reach5.initialize(success::call, failure::call)
    }

    fun loadSocialProviders(
        context: Context,
        success: Callback<List<Provider>>,
        failure: Callback<ReachFiveError>,
    ) {
        return reach5.loadSocialProviders(context, success::call, failure::call)
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
        failure: Callback<ReachFiveError>,
        origin: String? = null
    ) {
        return reach5.signup(profile, scope, redirectUrl, origin, success::call, failure::call)
    }

    /**
     * Sign-up with no required scopes (needed by the Java API)
     */
    fun signup(
        profile: ProfileSignupRequest,
        success: Callback<AuthToken>,
        failure: Callback<ReachFiveError>,
        origin: String? = null
    ) {
        return reach5.signup(
            profile,
            origin = origin,
            success = success::call,
            failure = failure::call
        )
    }

    /**
     * Passwordless
     */
    fun startPasswordless(
        email: String? = null,
        phoneNumber: String? = null,
        redirectUri: String,
        success: Callback<Unit>,
        failure: Callback<ReachFiveError>,
        activity: Activity,
        origin: String? = null
    ) {
        reach5.startPasswordless(
            email,
            phoneNumber,
            redirectUri,
            { success.call(Unit) },
            failure::call,
            activity,
            origin
        )
    }

    fun verifyPasswordless(
        phoneNumber: String,
        verificationCode: String,
        success: Callback<AuthToken>,
        failure: Callback<ReachFiveError>,
        activity: Activity
    ) {
        reach5.verifyPasswordless(
            phoneNumber,
            verificationCode,
            success::call,
            failure::call,
            activity
        )
    }

    /**
     * Login with required scopes
     * @param username You can use email or phone number
     */
    fun loginWithPassword(
        email: String? = null,
        phoneNumber: String? = null,
        customIdentifier: String? = null,
        password: String,
        scope: Collection<String>,
        success: Callback<AuthToken>,
        failure: Callback<ReachFiveError>,
        origin: String? = null
    ) {
        return reach5.loginWithPassword(
            email,
            phoneNumber,
            customIdentifier,
            password,
            scope,
            origin,
            success::call,
            failure::call
        )
    }

    /**
     * Login with no required scopes (needed by the Java API)
     * @param username You can use email or phone number
     */
    fun loginWithPassword(
        email: String? = null,
        phoneNumber: String? = null,
        customIdentifier: String? = null,
        password: String,
        success: Callback<AuthToken>,
        failure: Callback<ReachFiveError>,
        origin: String? = null
    ) {
        return reach5.loginWithPassword(
            email,
            phoneNumber,
            customIdentifier,
            password,
            origin = origin,
            success = success::call,
            failure = failure::call
        )
    }

    fun discoverableLogin(
        scope: Collection<String>,
        origin: String? = null,
        success: Callback<AuthToken>,
        failure: Callback<ReachFiveError>,
        activity: Activity,
        requestCredentialTypes: Set<CredentialType>
    ) {
        return reach5.discoverableLogin(
            scope,
            origin,
            success::call,
            failure::call,
            activity,
            requestCredentialTypes
        )
    }

    fun loginWithPasskey(
        loginRequest: WebAuthnLoginRequest,
        scope: Collection<String>,
        origin: String? = null,
        success: Callback<AuthToken>,
        failure: Callback<ReachFiveError>,
        activity: Activity
    ) {
        return reach5.loginWithPasskey(
            loginRequest,
            scope,
            origin,
            success::call,
            failure::call,
            activity
        )
    }

    fun signupWithPasskey(
        profile: ProfileWebAuthnSignupRequest,
        friendlyName: String?,
        scope: Collection<String>,
        origin: String? = null,
        success: Callback<AuthToken>,
        failure: Callback<ReachFiveError>,
        activity: Activity
    ) {
        return reach5.signupWithPasskey(
            profile,
            friendlyName,
            scope,
            origin,
            success::call,
            failure::call,
            activity
        )
    }

    fun registerNewPasskey(
        authToken: AuthToken,
        friendlyName: String?,
        success: Callback<Unit>,
        failure: Callback<ReachFiveError>,
        activity: Activity
    ) {
        return reach5.registerNewPasskey(
            authToken,
            friendlyName,
            success::call,
            failure::call,
            activity
        )
    }

    fun loginWithWeb(
        scope: Collection<String>,
        state: String? = null,
        nonce: String? = null,
        origin: String? = null,
        activity: Activity
    ) {
        return reach5.loginWithWeb(scope, state, nonce, origin, activity)
    }

    fun loginWithWebView(
        scope: Collection<String>,
        state: String? = null,
        nonce: String? = null,
        origin: String? = null,
        activity: Activity
    ) {
        return reach5.loginWithWebView(scope, state, nonce, origin, activity)
    }

    fun logout(
        success: Callback<Unit>,
        failure: Callback<ReachFiveError>
    ) {
        return reach5.logout({ success.call(Unit) }, failure::call)
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
        success: Callback<Unit>,
        failure: Callback<ReachFiveError>
    ) {
        return reach5.verifyPhoneNumber(
            authToken,
            phoneNumber,
            verificationCode,
            { success.call(Unit) },
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
        success: Callback<Unit>,
        failure: Callback<ReachFiveError>
    ) {
        return reach5.updatePassword(
            updatePasswordRequest,
            { success.call(Unit) },
            failure::call
        )
    }

    fun requestPasswordReset(
        email: String?,
        redirectUrl: String?,
        phoneNumber: String?,
        success: Callback<Unit>,
        failure: Callback<ReachFiveError>
    ) {
        return reach5.requestPasswordReset(
            email,
            redirectUrl,
            phoneNumber,
            { success.call(Unit) },
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

    fun onAddNewWebAuthnDeviceResult(
        requestCode: Int,
        data: Intent?,
        success: Callback<Unit>,
        failure: Callback<ReachFiveError>,
    ) {
        return reach5.onAddNewWebAuthnDeviceResult(requestCode, data, success::call, failure::call)
    }

    fun onLoginActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        loginSuccess: Callback<AuthToken>,
        failure: Callback<ReachFiveError>,
        activity: Activity,
        origin: String? = null
    ) {
        return reach5.onLoginActivityResult(
            requestCode,
            resultCode,
            data,
            loginSuccess::call,
            failure::call,
            activity,
            origin
        )
    }

    fun onStop() {
        reach5.onStop()
    }
}
