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
import com.reach5.identity.sdk.core.models.ProviderConfig
import com.reach5.identity.sdk.core.utils.Failure
import com.reach5.identity.sdk.core.ProviderCreator
import com.reach5.identity.sdk.core.utils.Success
import com.reach5.identity.sdk.core.api.ReachFiveApi
import com.reach5.identity.sdk.core.models.OpenIdTokenResponse
import com.reach5.identity.sdk.core.models.ReachFiveError
import com.reach5.identity.sdk.core.models.SdkConfig

class FacebookProvider : ProviderCreator {
    companion object {
        const val NAME = "facebook"
    }

    override val name: String = NAME

    override fun create(providerConfig: ProviderConfig, sdkConfig: SdkConfig, reachFiveApi: ReachFiveApi, context: Context): Provider {
        return ConfiguredFacebookProvider(providerConfig, reachFiveApi, context)
    }
}

class ConfiguredFacebookProvider(override val providerConfig: ProviderConfig, override val reachFiveApi: ReachFiveApi, context: Context): Provider {
    companion object {
        const val TAG = "Reach5_FbProvider"
    }

    override val requestCode: Int = 123121212
    override val name: String = FacebookProvider.NAME

    private val callbackManager = CallbackManager.Factory.create()

    init {
        FacebookSdk.setApplicationId(providerConfig.clientId)
        // FIXME resolve deprecation
        FacebookSdk.sdkInitialize(context)

        LoginManager.getInstance().registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onCancel() {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onError(error: FacebookException?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

        })
    }


    override fun onActivityResult(
        requestCode: Int,
        data: Intent?,
        success: Success<OpenIdTokenResponse>,
        failure: Failure<ReachFiveError>
    ) {
        callbackManager.onActivityResult(requestCode, 0 /* TODO resultCode */, data)
    }

    override fun login(origin: String, activity: Activity) {
        Log.d(TAG, "login with native provider")
        LoginManager.getInstance().logInWithReadPermissions(activity, providerConfig.scope)
    }


}
