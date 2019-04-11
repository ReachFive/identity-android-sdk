package com.reach5.identity.sdk.facebook

import android.app.Activity
import android.content.Context
import android.content.Intent
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
        const val NAME = "google"
    }

    override val name: String = NAME

    override fun create(providerConfig: ProviderConfig, sdkConfig: SdkConfig, reachFiveApi: ReachFiveApi, context: Context): Provider {
        return ConfiguredFacebookProvider(providerConfig, reachFiveApi)
    }
}

class ConfiguredFacebookProvider(override val providerConfig: ProviderConfig, override val reachFiveApi: ReachFiveApi) : Provider {
    override fun onActivityResult(
        requestCode: Int,
        data: Intent?,
        success: Success<OpenIdTokenResponse>,
        failure: Failure<ReachFiveError>
    ) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun login(origin: String, activity: Activity) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override val requestCode: Int = 123121212

    override val name: String = FacebookProvider.NAME
}
