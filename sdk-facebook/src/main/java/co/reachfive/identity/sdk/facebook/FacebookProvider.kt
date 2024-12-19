package co.reachfive.identity.sdk.facebook

import android.app.Activity
import android.content.Context
import android.content.Intent
import co.reachfive.identity.sdk.core.Provider
import co.reachfive.identity.sdk.core.ProviderCreator
import co.reachfive.identity.sdk.core.SessionUtilsClient
import co.reachfive.identity.sdk.core.models.AuthToken
import co.reachfive.identity.sdk.core.models.ProviderConfig
import co.reachfive.identity.sdk.core.models.ReachFiveError
import co.reachfive.identity.sdk.core.models.SdkConfig
import co.reachfive.identity.sdk.core.utils.Failure
import co.reachfive.identity.sdk.core.utils.Success
import co.reachfive.identity.sdk.facebook.FacebookProvider.Companion.REQUEST_CODE
import com.facebook.*
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult

class FacebookProvider : ProviderCreator {
    companion object {
        const val NAME = "facebook"
        const val REQUEST_CODE = 64206
    }

    override val name: String = NAME

    override fun create(
        providerConfig: ProviderConfig,
        sessionUtils: SessionUtilsClient,
        context: Context,
        sdkConfig: SdkConfig,
    ): Provider {
        return ConfiguredFacebookProvider(providerConfig, sessionUtils)
    }
}

internal class ConfiguredFacebookProvider(
    private val providerConfig: ProviderConfig,
    private val sessionUtils: SessionUtilsClient,
) : Provider {
    override val requestCode: Int = REQUEST_CODE
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
        LoginManager.getInstance().logInWithReadPermissions(activity, providerConfig.scope)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>
    ) {
        LoginManager.getInstance()
            .registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    val accessToken = result.accessToken.token
                    sessionUtils.loginWithProvider(
                        name,
                        providerAccessToken = accessToken,
                        origin = origin,
                        scope = scope,
                        success = success,
                        failure = failure
                    )
                }

                override fun onCancel() {
                    failure(ReachFiveError.from("User cancel"))
                }

                override fun onError(error: FacebookException) {
                    if (error is FacebookAuthorizationException) {
                        if (AccessToken.getCurrentAccessToken() != null) {
                            LoginManager.getInstance().logOut()
                        }
                    }

                    failure(ReachFiveError.from(error))
                }
            })
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
        failure: Failure<ReachFiveError>,
        activity: Activity
    ) {
        // Do nothing
    }
}
