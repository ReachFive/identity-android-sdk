package com.reach5.identity.sdk.facebook

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.FacebookSdk
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.reach5.identity.sdk.core.Provider
import com.reach5.identity.sdk.core.utils.Failure
import com.reach5.identity.sdk.core.ProviderCreator
import com.reach5.identity.sdk.core.api.LoginProviderRequest
import com.reach5.identity.sdk.core.utils.Success
import com.reach5.identity.sdk.core.api.ReachFiveApi
import com.reach5.identity.sdk.core.api.ReachFiveApiCallback
import com.reach5.identity.sdk.core.models.*

class FacebookProvider : ProviderCreator {
    companion object {
        const val NAME = "facebook"
    }

    override val name: String = NAME

    override fun create(providerConfig: ProviderConfig, sdkConfig: SdkConfig, reachFiveApi: ReachFiveApi, context: Context): Provider {
        return ConfiguredFacebookProvider(providerConfig, sdkConfig, reachFiveApi, context)
    }
}

class ConfiguredFacebookProvider(private val providerConfig: ProviderConfig, val sdkConfig: SdkConfig, val reachFiveApi: ReachFiveApi, context: Context): Provider {
    companion object {
        const val TAG = "Reach5_FbProvider"
    }

    override val requestCode: Int = 64206
    override val name: String = FacebookProvider.NAME

    private lateinit var origin: String

    private val callbackManager = CallbackManager.Factory.create()

    init {
        FacebookSdk.setApplicationId(providerConfig.clientId)
        // FIXME resolve deprecation
        @Suppress("DEPRECATION")
        FacebookSdk.sdkInitialize(context)
    }


    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        success: Success<OpenIdTokenResponse>,
        failure: Failure<ReachFiveError>
    ) {
        LoginManager.getInstance().registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult?) {
                val accessToken = result?.accessToken?.token
                if (accessToken != null) {
                    val loginProviderRequest = LoginProviderRequest(
                        provider = name,
                        providerToken = accessToken,
                        clientId = sdkConfig.clientId,
                        origin = origin,
                        scope = providerConfig.scope.joinToString { " " }.plus("openid")
                    )
                    reachFiveApi.loginWithProvider(loginProviderRequest, SdkInfos.getQueries()).enqueue(
                        ReachFiveApiCallback(success, failure)
                    )
                } else {
                    failure(ReachFiveError.from("Facebook didn't return an access token!"))

                }
            }

            override fun onCancel() {
                // TODO
            }

            override fun onError(error: FacebookException?) {
                // TODO
            }

        })
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }

    override fun login(origin: String, activity: Activity) {
        Log.d(TAG, "login with native provider")
        this.origin = origin
        LoginManager.getInstance().logInWithReadPermissions(activity, providerConfig.scope)
    }
}
