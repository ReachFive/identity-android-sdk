package co.reachfive.identity.sdk.core

import android.app.Activity
import android.content.Intent
import android.util.Log
import co.reachfive.identity.sdk.core.api.ReachFiveApi
import co.reachfive.identity.sdk.core.api.ReachFiveApiCallback
import co.reachfive.identity.sdk.core.models.*
import co.reachfive.identity.sdk.core.utils.Failure
import co.reachfive.identity.sdk.core.utils.Success

internal interface SocialLoginAuth {
    var defaultScope: Set<String>

    fun getProvider(name: String): Provider?

    fun getProviders(): List<Provider>

    fun loginWithProvider(
        name: String,
        scope: Collection<String> = defaultScope,
        origin: String,
        state: String? = null,
        nonce: String? = null,
        activity: Activity
    )

    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
        failure: Failure<ReachFiveError>
    )
}

internal class SocialLoginAuthClient(
    private val reachFiveApi: ReachFiveApi,
    private val activity: Activity,
    private val sdkConfig: SdkConfig,
    private val providersCreators: List<ProviderCreator>,
    override var defaultScope: Set<String> = emptySet(),
) : SocialLoginAuth {

    private var providers: List<Provider> = emptyList()

    override fun getProviders(): List<Provider> = providers

    internal fun onStop() = providers.forEach { it.onStop() }

    internal fun logoutFromAll() = providers.forEach { it.logout() }

    internal fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>
    ) {
        val provider = providers.find { p -> p.requestCode == requestCode }
        if (provider != null) {
            provider.onActivityResult(requestCode, resultCode, data, success, failure)
        } else {
            failure(ReachFiveError.from("No provider found for this requestCode: $requestCode"))
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
        failure: Failure<ReachFiveError>
    ) {
        providers
            .find { p -> p.requestCode == requestCode }
            ?.onRequestPermissionsResult(requestCode, permissions, grantResults, failure)
    }

    override fun loginWithProvider(
        name: String,
        scope: Collection<String>,
        origin: String,
        state: String?,
        nonce: String?,
        activity: Activity
    ) {
        getProvider(name)?.login(origin, scope, activity)
    }

    override fun getProvider(name: String): Provider? =
        providers.find { p -> p.name == name }

    internal fun providersConfigs(
        success: Success<List<Provider>>,
        failure: Failure<ReachFiveError>
    ) {
        if (providersCreators.isNotEmpty()) {
            reachFiveApi
                .providersConfigs(SdkInfos.getQueries())
                .enqueue(ReachFiveApiCallback<ProvidersConfigsResult>({
                    providers = createProviders(it)
                    success(providers)
                }, failure = failure))
        } else success(emptyList())
    }

    private fun createProviders(providersConfigsResult: ProvidersConfigsResult): List<Provider> {
        val webViewProvider = providersCreators.find { it.name == "webview" }
        return providersConfigsResult.items?.mapNotNull { config ->
            val nativeProvider = providersCreators.find { it.name == config.provider }
            when {
                nativeProvider != null -> nativeProvider.create(
                    config,
                    sdkConfig,
                    reachFiveApi,
                    activity
                )
                webViewProvider != null -> webViewProvider.create(
                    config,
                    sdkConfig,
                    reachFiveApi,
                    activity
                )
                else -> {
                    Log.w(
                        ReachFive.TAG,
                        "Non supported provider found, please add webview or native provider"
                    )
                    null
                }
            }
        } ?: listOf()
    }
}