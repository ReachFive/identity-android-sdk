package com.reach5.identity.sdk.core

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import com.reach5.identity.sdk.core.api.*
import com.reach5.identity.sdk.core.models.*
import com.reach5.identity.sdk.core.utils.Failure
import com.reach5.identity.sdk.core.utils.Success

class ReachFive(val context: Context, val sdkConfig: SdkConfig, val providersCreators: List<ProviderCreator>) {

    companion object {
        private const val TAG = "Reach5_ReachFive"
    }

    private val reachFiveApi: ReachFiveApi = ReachFiveApi.create(sdkConfig)

    private var providers: List<Provider> = listOf()

    fun initialize(success: Success<List<Provider>>, failure: Failure<ReachFiveError>): ReachFive {
        providersConfigs(success, failure)
        return this
    }

    private fun providersConfigs(success: Success<List<Provider>>, failure: Failure<ReachFiveError>) {
        reachFiveApi.providersConfigs(SdkInfos.getQueries()).enqueue(ReachFiveApiCallback<ProvidersConfigsResult>({
            Log.d(TAG, "providersConfigs success=$it")
            providers = createProviders(it)
            success(providers)
        }, failure))
    }

    private fun createProviders(providersConfigsResult: ProvidersConfigsResult): List<Provider> {
        val webViewProvider = providersCreators.find { it.name == "webview" }
        return providersConfigsResult.items?.mapNotNull { config ->
            val nativeProvider = providersCreators.find { it.name == config.provider }
            when {
                nativeProvider != null -> nativeProvider.create(config, sdkConfig, reachFiveApi, context)
                webViewProvider != null -> webViewProvider.create(config, sdkConfig, reachFiveApi, context)
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

    fun signupWithPassword(
        profile: Profile,
        success: Success<OpenIdTokenResponse>,
        failure: Failure<ReachFiveError>
    ) {
        reachFiveApi.signupWithPassword(SignupRequest(
            clientId = sdkConfig.clientId,
            data = profile
        ), SdkInfos.getQueries()).enqueue(ReachFiveApiCallback(success, failure))
    }

    fun loginWithPassword(
        username: String,
        password: String,
        success: Success<OpenIdTokenResponse>,
        failure: Failure<ReachFiveError>
    ) {
        reachFiveApi.loginWithPassword(LoginRequest(
            clientId = sdkConfig.clientId,
            grantType = "password",
            username = username,
            password = password
        ), SdkInfos.getQueries()).enqueue(ReachFiveApiCallback(success, failure))
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?, success: Success<OpenIdTokenResponse>, failure: Failure<ReachFiveError>) {
        Log.d(TAG, "ReachFive.onActivityResult requestCode=$requestCode")
        val provider =  providers.find { p -> p.requestCode == requestCode }
        if (provider != null) {
            provider.onActivityResult(requestCode, resultCode, data, success, failure)
        } else {
            failure(ReachFiveError.from("No provider found for this requestCode: $requestCode"))
        }
    }

    fun logout(callback: () -> Unit) {
        Log.d(TAG, "ReachFive.logout")
        // TODO
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
}
