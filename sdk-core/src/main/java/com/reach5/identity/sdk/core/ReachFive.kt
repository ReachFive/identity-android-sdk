package com.reach5.identity.sdk.core

import android.app.Activity
import android.content.Intent
import android.util.Log
import com.reach5.identity.sdk.core.api.*
import com.reach5.identity.sdk.core.models.*
import com.reach5.identity.sdk.core.utils.Failure
import com.reach5.identity.sdk.core.utils.Success

class ReachFive(val activity: Activity, val sdkConfig: SdkConfig, val providersCreators: List<ProviderCreator>) {

    companion object {
        private const val TAG = "Reach5"
    }

    val defaultScope = listOf(
        // Access the ID token
        "openid",
        // Access the email address
        "email",
        // Access the phone number
        "phone",
        // Access the profile's personal information
        "profile"
    )

    private val reachFiveApi: ReachFiveApi = ReachFiveApi.create(sdkConfig)

    private var providers: List<Provider> = listOf()

    fun initialize(success: Success<List<Provider>> = {}, failure: Failure<ReachFiveError> = {}): ReachFive {
        providersConfigs(success, failure)
        return this
    }

    private fun providersConfigs(success: Success<List<Provider>>, failure: Failure<ReachFiveError>) {
        reachFiveApi.providersConfigs(SdkInfos.getQueries()).enqueue(ReachFiveApiCallback<ProvidersConfigsResult>({
            providers = createProviders(it)
            success(providers)
        }, failure))
    }

    private fun createProviders(providersConfigsResult: ProvidersConfigsResult): List<Provider> {
        val webViewProvider = providersCreators.find { it.name == "webview" }
        return providersConfigsResult.items?.mapNotNull { config ->
            val nativeProvider = providersCreators.find { it.name == config.provider }
            when {
                nativeProvider != null -> nativeProvider.create(config, sdkConfig, reachFiveApi, activity)
                webViewProvider != null -> webViewProvider.create(config, sdkConfig, reachFiveApi, activity)
                else -> {
                    Log.w(TAG, "Non supported provider found, please add webview or native provider")
                    null
                }
            }
        } ?: listOf()
    }

    fun getProvider(name: String): Provider? {
        return providers.find { p -> p.name == name }
    }

    fun getProviders(): List<Provider> {
        return providers
    }

    fun loginWithProvider(name: String, origin: String, activity: Activity) {
        getProvider(name)?.login(origin, activity)
    }

    fun signup(
        profile: Profile,
        scope: List<String> = defaultScope,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>
    ) {
        val signupRequest = SignupRequest(
            clientId = sdkConfig.clientId,
            data = profile,
            scope = formatScope(scope)
        )
        reachFiveApi
            .signup(signupRequest, SdkInfos.getQueries())
            .enqueue(ReachFiveApiCallback({
                it.toAuthToken().fold(success, failure)
            }, failure))
    }

    /**
     * @param username You can use email or phone number
     */
    fun loginWithPassword(
        username: String,
        password: String,
        scope: List<String> = defaultScope,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>
    ) {
        reachFiveApi.loginWithPassword(
            LoginRequest(
                clientId = sdkConfig.clientId,
                grantType = "password",
                username = username,
                password = password,
                scope = formatScope(scope)
            ), SdkInfos.getQueries()).enqueue(ReachFiveApiCallback({
            it.toAuthToken().fold(success, failure)
        }, failure))
    }

    fun updateProfile(
        authToken: AuthToken,
        profile: Profile,
        success: Success<Profile>,
        failure: Failure<ReachFiveError>
    ) {
        reachFiveApi
            .updateProfile("${authToken.tokenType} ${authToken.accessToken}", profile, SdkInfos.getQueries())
            .enqueue(ReachFiveApiCallback(success , failure))
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?, success: Success<AuthToken>, failure: Failure<ReachFiveError>) {
        val provider =  providers.find { p -> p.requestCode == requestCode }
        if (provider != null) {
            provider.onActivityResult(requestCode, resultCode, data, success, failure)
        } else {
            failure(ReachFiveError.from("No provider found for this requestCode: $requestCode"))
        }
    }

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray, failure: Failure<ReachFiveError>) {
        val provider =  providers.find { p -> p.requestCode == requestCode }
        if (provider != null) {
            provider.onRequestPermissionsResult(requestCode, permissions, grantResults, failure)
        } else {
            failure(ReachFiveError.from("No provider found for this requestCode: $requestCode"))
        }
    }

    fun logout(callback: () -> Unit) {
        providers.forEach {
            it.logout()
        }.also {
            callback()
        }
    }

    fun onStop() {
        providers.forEach {
            it.onStop()
        }
    }

    private fun formatScope(scope: List<String>): String {
        return scope.joinToString(" ")
    }
}
