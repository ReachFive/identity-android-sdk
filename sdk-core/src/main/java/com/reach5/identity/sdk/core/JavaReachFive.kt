package com.reach5.identity.sdk.core

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.reach5.identity.sdk.core.models.Profile
import com.reach5.identity.sdk.core.models.AuthToken
import com.reach5.identity.sdk.core.models.ReachFiveError
import com.reach5.identity.sdk.core.models.SdkConfig
import com.reach5.identity.sdk.core.utils.Callback

class JavaReachFive(activity: Activity, sdkConfig: SdkConfig, providersCreators: List<ProviderCreator>) {
    private val reach5 = ReachFive(activity, sdkConfig, providersCreators)

    val defaultScope = reach5.defaultScope

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
        profile: Profile,
        scope: List<String>,
        success: Callback<AuthToken>,
        failure: Callback<ReachFiveError>
    ) {
        return reach5.signup(profile, scope, success::call, failure::call)
    }

    /**
     * Sign-up with no required scopes (needed by the Java API)
     */
    fun signup(
        profile: Profile,
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
        scope: List<String>,
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

    fun updateProfile(
        authToken: AuthToken,
        profile: Profile,
        success: Callback<Profile>,
        failure: Callback<ReachFiveError>
    ) {
        return reach5.updateProfile(authToken, profile, success::call, failure::call)
    }

    fun requestPasswordReset(
        authToken: AuthToken,
        email: String?,
        redirectUrl: String?,
        phoneNumber: String?,
        success: Callback<Unit>,
        failure: Callback<ReachFiveError>
    ) {
        return reach5.requestPasswordReset(authToken, email, redirectUrl, phoneNumber, success::call, failure::call)
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

    fun logout() {
        reach5.logout {}
    }

    fun onStop() {
        reach5.onStop()
    }
}
