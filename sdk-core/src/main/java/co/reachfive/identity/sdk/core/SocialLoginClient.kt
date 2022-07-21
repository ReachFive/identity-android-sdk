package co.reachfive.identity.sdk.core

import android.app.Activity
import android.util.Log
import co.reachfive.identity.sdk.core.api.ReachFiveApi
import co.reachfive.identity.sdk.core.api.ReachFiveApiCallback
import co.reachfive.identity.sdk.core.models.ProvidersConfigsResult
import co.reachfive.identity.sdk.core.models.ReachFiveError
import co.reachfive.identity.sdk.core.models.SdkConfig
import co.reachfive.identity.sdk.core.models.SdkInfos
import co.reachfive.identity.sdk.core.utils.Failure
import co.reachfive.identity.sdk.core.utils.Success

internal interface SocialLoginClient {
    var defaultScope: Set<String>
    val sdkConfig: SdkConfig

    fun getProvider(name: String): Provider?

    fun loginWithProvider(
        name: String,
        scope: Collection<String> = this.defaultScope,
        origin: String,
        activity: Activity
    )
}

internal class SocialLoginManager(
    val reachFiveApi: ReachFiveApi,
    val activity: Activity,
    override var defaultScope: Set<String>,
    val providersCreators: List<ProviderCreator>,
    override val sdkConfig: SdkConfig,
    var providers: List<Provider> = emptyList(),
) : SocialLoginClient {

    override fun loginWithProvider(
        name: String,
        scope: Collection<String>,
        origin: String,
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
        reachFiveApi
            .providersConfigs(SdkInfos.getQueries())
            .enqueue(ReachFiveApiCallback<ProvidersConfigsResult>({
                providers = createProviders(it)
                success(providers)
            }, failure = failure))
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