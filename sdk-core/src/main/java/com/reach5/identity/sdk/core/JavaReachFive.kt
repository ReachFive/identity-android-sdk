package com.reach5.identity.sdk.core

import android.app.Activity
import android.content.Intent
import com.reach5.identity.sdk.core.models.*
import com.reach5.identity.sdk.core.models.requests.ProfileSignupRequest
import com.reach5.identity.sdk.core.models.requests.UpdatePasswordRequest
import com.reach5.identity.sdk.core.utils.Callback
import com.reach5.identity.sdk.core.utils.Failure
import com.reach5.identity.sdk.core.utils.SuccessWithNoContent

class JavaReachFive(activity: Activity, sdkConfig: SdkConfig, providersCreators: List<ProviderCreator>) {
    private val reach5 = ReachFive(activity, sdkConfig, providersCreators)

    fun initialize(success: Callback<List<Provider>>, failure: Callback<ReachFiveError>): ReachFive {
        return reach5.initialize(success::call, failure::call)
    }

    fun getProvider(name: String): Provider? {
        return reach5.getProvider(name)
    }

    fun getProviders(): List<Provider> {
        return reach5.getProviders()
    }

    fun loginWithNativeProvider(name: String, origin: String, activity: Activity) {
        return reach5.loginWithProvider(name, origin, activity)
    }

    /**
     * Sign-up with required scopes
     */
    fun signup(
        profile: ProfileSignupRequest,
        scope: Collection<String>,
        success: Callback<AuthToken>,
        failure: Callback<ReachFiveError>
    ) {
        return reach5.signup(profile, scope, success::call, failure::call)
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
        return reach5.loginWithPassword(username, password, success = success::call, failure = failure::call)
    }

    fun logout(
        redirectTo: String? = null,
        successWithNoContent: Callback<Unit>,
        failure: Callback<ReachFiveError>
    ) {
        return reach5.logout(redirectTo, { successWithNoContent.call(Unit) }, failure::call)
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
        authToken: AuthToken,
        updatePhoneNumberRequest: UpdatePasswordRequest,
        successWithNoContent: Callback<Unit>,
        failure: Callback<ReachFiveError>
    ) {
        return reach5.updatePassword(
            authToken,
            updatePhoneNumberRequest,
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

    fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        success: Callback<AuthToken>,
        failure: Callback<ReachFiveError>
    ) {
        return reach5.onActivityResult(requestCode, resultCode, data, success::call, failure::call)
    }

    fun onStop() {
        reach5.onStop()
    }
}
