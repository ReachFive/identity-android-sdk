package co.reachfive.identity.sdk.google

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import co.reachfive.identity.sdk.core.Provider
import co.reachfive.identity.sdk.core.ProviderCreator
import co.reachfive.identity.sdk.core.SessionUtilsClient
import co.reachfive.identity.sdk.core.api.ReachFiveApi
import co.reachfive.identity.sdk.core.models.AuthToken
import co.reachfive.identity.sdk.core.models.ProviderConfig
import co.reachfive.identity.sdk.core.models.ReachFiveError
import co.reachfive.identity.sdk.core.models.SdkConfig
import co.reachfive.identity.sdk.core.utils.Failure
import co.reachfive.identity.sdk.core.utils.Success
import co.reachfive.identity.sdk.google.GoogleProvider.Companion.PERMISSIONS_REQUEST_GET_ACCOUNTS
import co.reachfive.identity.sdk.google.GoogleProvider.Companion.REQUEST_CODE
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope

class GoogleProvider : ProviderCreator {
    companion object {
        const val NAME = "google"
        const val REQUEST_CODE = 14267
        const val PERMISSIONS_REQUEST_GET_ACCOUNTS = 14278
    }

    override val name: String = NAME

    override fun create(
        providerConfig: ProviderConfig,
        sessionUtils: SessionUtilsClient,
        context: Context,
    ): Provider {
        return ConfiguredGoogleProvider(providerConfig, sessionUtils, context)
    }
}

internal class ConfiguredGoogleProvider(
    private val providerConfig: ProviderConfig,
    private val sessionUtils: SessionUtilsClient,
    private val context: Context,
) : Provider {
    private lateinit var origin: String
    private lateinit var scope: Collection<String>

    private val googleSignInClient: GoogleSignInClient

    override val requestCode: Int = REQUEST_CODE
    override val name: String = GoogleProvider.NAME

    init {
        val gso = GoogleSignInOptions
            .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(
                Scope(Scopes.OPEN_ID),
                *this.providerConfig.scope.map { Scope(it) }.toTypedArray()
            )
            .requestServerAuthCode(providerConfig.clientId)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this.context, gso)
    }

    override fun login(origin: String, scope: Collection<String>, activity: Activity) {
        this.origin = origin
        this.scope = scope

        val signInIntent = googleSignInClient.signInIntent
        activity.startActivityForResult(signInIntent, REQUEST_CODE)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>
    ) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val googleSigninAccount = task.getResult(ApiException::class.java)
            val authCode = googleSigninAccount?.serverAuthCode
            if (authCode != null) {
                sessionUtils.loginWithProvider(name, authCode, origin, scope, success, failure)
            } else {
                failure(ReachFiveError.from("No auth code"))
            }
        } catch (e: ApiException) {
            failure(ReachFiveError.from(e))
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
        failure: Failure<ReachFiveError>,
        activity: Activity,
    ) {
        if (PERMISSIONS_REQUEST_GET_ACCOUNTS == requestCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                login(origin = origin, scope = scope, activity = activity)
            } else {
                failure(ReachFiveError.from("permission denied"))
            }
        }
    }
}
