package co.reachfive.identity.sdk.facebook

import android.app.Activity
import android.content.Intent
import android.util.Log
import co.reachfive.identity.sdk.core.Provider
import co.reachfive.identity.sdk.core.ProviderCreator
import co.reachfive.identity.sdk.core.ReachFiveWebAuthn
import co.reachfive.identity.sdk.core.api.ReachFiveApi
import co.reachfive.identity.sdk.core.api.ReachFiveApiCallback
import co.reachfive.identity.sdk.core.models.*
import co.reachfive.identity.sdk.core.models.requests.LoginProviderRequest
import co.reachfive.identity.sdk.core.utils.Failure
import co.reachfive.identity.sdk.core.utils.Success
import com.facebook.*
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult

class FacebookProvider : ProviderCreator {
    companion object {
        const val NAME = "facebook"
    }

    override val name: String = NAME

    override fun create(
        providerConfig: ProviderConfig,
        sdkConfig: SdkConfig,
        reachFiveApi: ReachFiveApi,
        activity: Activity
    ): Provider {
        return ConfiguredFacebookProvider(providerConfig, sdkConfig, reachFiveApi)
    }
}

class ConfiguredFacebookProvider(
    private val providerConfig: ProviderConfig,
    val sdkConfig: SdkConfig,
    val reachFiveApi: ReachFiveApi,
) : Provider {
    override val requestCode: Int = 64206
    override val name: String = FacebookProvider.NAME

    private lateinit var origin: String
    private lateinit var scope: Collection<String>

    private val callbackManager = CallbackManager.Factory.create()

    init {
        FacebookSdk.setApplicationId(providerConfig.clientId)
    }

    override fun login(origin: String, scope: Collection<String>, activity: Activity) {
        this.origin = origin
        this.scope = scope
        val loginManager = LoginManager.getInstance()
        Log.d("SDK_DEBUG", "LoginManager.authType: "+loginManager.authType)
        LoginManager.getInstance().logInWithReadPermissions(activity, providerConfig.scope)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>
    ) {
        Log.d("SDK_DEBUG", "FacebookProvider.onActivityResult");
        LoginManager.getInstance()
            .registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    Log.d("SDK_DEBUG", "FacebookProvider.onActivityResult.onSuccess: "+result);
                    val accessToken = result.accessToken.token
                    val loginProviderRequest = LoginProviderRequest(
                        provider = name,
                        providerToken = accessToken,
                        clientId = sdkConfig.clientId,
                        origin = origin,
                        scope = scope.joinToString(" ")
                    )
                    reachFiveApi.loginWithProvider(loginProviderRequest, SdkInfos.getQueries())
                        .enqueue(
                            ReachFiveApiCallback(success = {
                                it.toAuthToken().fold(success, failure)
                            }, failure = failure)
                        )
                }

                override fun onCancel() {
                    Log.d("SDK_DEBUG", "FacebookProvider.onActivityResult.onCancel");
                    failure(ReachFiveError.from("User cancel"))
                }

                override fun onError(error: FacebookException) {
                    Log.d("SDK_DEBUG", "FacebookProvider.onActivityResult.onError");
                    if (error is FacebookAuthorizationException) {
                        if (AccessToken.getCurrentAccessToken() != null) {
                            LoginManager.getInstance().logOut()
                        }
                    } else
                        failure(ReachFiveError.from(error))
                }
            })
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
        failure: Failure<ReachFiveError>
    ) {
        // Do nothing
    }
}
