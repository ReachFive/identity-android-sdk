package com.reach5.identity.sdk.web

import android.app.Activity
import android.content.Intent
import android.util.Log
import com.reach5.identity.sdk.core.Provider
import com.reach5.identity.sdk.core.ProviderCreator
import com.reach5.identity.sdk.core.api.ReachFiveApi
import com.reach5.identity.sdk.core.api.ReachFiveApiCallback
import com.reach5.identity.sdk.core.models.*
import com.reach5.identity.sdk.core.models.requests.AuthCodeRequest
import com.reach5.identity.sdk.core.utils.Failure
import com.reach5.identity.sdk.core.utils.Pkce
import com.reach5.identity.sdk.core.utils.Success

class WebProvider : ProviderCreator {
    override val name: String = "customtab"

    override fun create(
        providerConfig: ProviderConfig,
        sdkConfig: SdkConfig,
        reachFiveApi: ReachFiveApi,
        activity: Activity
    ): Provider {
        return ConfiguredWebProvider(providerConfig, sdkConfig, reachFiveApi)
    }
}

class ConfiguredWebProvider(
    private val providerConfig: ProviderConfig,
    private val sdkConfig: SdkConfig,
    private val reachFiveApi: ReachFiveApi
) : Provider {

    override val name: String = providerConfig.provider
    override val requestCode: Int = REQUEST_CODE

    companion object {
        // Intent data extra keys
        const val AUTH_CODE_KEY = "AUTH_CODE"
        const val CONFIG_KEY = "CONFIG"
        const val PKCE_KEY = "PKCE"

        const val REQUEST_CODE = 52558

        // Result codes
        const val UNEXPECTED_ERROR_CODE = -1
        const val SUCCESS_CODE = 0
        const val ABORT_CODE = 1
        const val NO_AUTH_ERROR_CODE = 2
    }

    override fun login(origin: String, activity: Activity) {
        val intent = Intent(activity, ReachFiveLoginActivity::class.java)

        intent.putExtra(
            CONFIG_KEY, WebProviderConfig(
                providerConfig = providerConfig,
                sdkConfig = sdkConfig,
                origin = origin
            )
        )
        intent.putExtra(PKCE_KEY, Pkce.generate())
        activity.startActivityForResult(intent, requestCode)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>
    ) {
        if (requestCode == this.requestCode && data != null) {
            return when (resultCode) {
                SUCCESS_CODE -> {
                    val authCode = data.getStringExtra(AUTH_CODE_KEY)!!
                    val pkce = data.getParcelableExtra<Pkce>(PKCE_KEY)!!

                    val authCodeRequest = AuthCodeRequest(
                        sdkConfig.clientId,
                        authCode,
                        SdkConfig.REDIRECT_URI,
                        pkce.codeVerifier
                    )

                    reachFiveApi
                        .authenticateWithCode(authCodeRequest, SdkInfos.getQueries())
                        .enqueue(
                            ReachFiveApiCallback(
                                success = { it.toAuthToken().fold(success, failure) },
                                failure = failure
                            )
                        )
                }
                ABORT_CODE -> {
                    Log.d("ReachFive", "The custom tab has been closed.")
                    Unit
                }
                NO_AUTH_ERROR_CODE -> {
                    failure(ReachFiveError.from("No authorization code found in activity result."))
                }
                else -> {
                    Log.e("ReachFive", "Unexpected event.")
                    Unit
                }
            }
        }
    }

    override fun toString(): String {
        return providerConfig.provider
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