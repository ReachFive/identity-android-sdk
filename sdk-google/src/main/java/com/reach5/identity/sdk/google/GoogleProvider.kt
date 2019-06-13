package com.reach5.identity.sdk.google

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks
import com.google.android.gms.common.api.Scope
import com.reach5.identity.sdk.core.utils.Failure
import com.reach5.identity.sdk.core.Provider
import com.reach5.identity.sdk.core.ProviderCreator
import com.reach5.identity.sdk.core.utils.Success
import com.reach5.identity.sdk.core.api.LoginProviderRequest
import com.reach5.identity.sdk.core.api.ReachFiveApi
import com.reach5.identity.sdk.core.api.ReachFiveApiCallback
import com.reach5.identity.sdk.core.models.*

class GoogleProvider : ProviderCreator {
    companion object {
        const val NAME = "google"
    }

    override val name: String = NAME

    override fun create(providerConfig: ProviderConfig, sdkConfig: SdkConfig, reachFiveApi: ReachFiveApi, context: Context): Provider {
        return ConfiguredGoogleProvider(providerConfig, sdkConfig, reachFiveApi, context)
    }
}

class ConfiguredGoogleProvider(private val providerConfig: ProviderConfig, private val sdkConfig: SdkConfig, private val reachFiveApi: ReachFiveApi, context: Context) : Provider {
    private lateinit var origin: String

    private val googleApiClient: GoogleApiClient

    companion object {
        const val REQUEST_CODE = 142
        private const val TAG = "Reach5_CGProvider"
    }

    override val requestCode: Int = REQUEST_CODE
    override val name: String = GoogleProvider.NAME

    init {
        Log.d(TAG, "GoogleProvider.init clientId=${providerConfig.clientId}")

        val gso = GoogleSignInOptions
            .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(Scope(Scopes.OPEN_ID), *(providerConfig.scope ?: setOf()).map { s -> Scope(s) }.toTypedArray())
            .requestServerAuthCode(providerConfig.clientId)
            .requestEmail()

        googleApiClient = GoogleApiClient.Builder(context)
            .addConnectionCallbacks(object : ConnectionCallbacks {
                override fun onConnected(bundle: Bundle?) {
                    Log.d(TAG, "GoogleProvider.onConnected")
                }

                override fun onConnectionSuspended(n: Int) {
                    Log.d(TAG, "GoogleProvider.onConnected $n")
                }
            })
            .addOnConnectionFailedListener { connectionResult ->
                Log.d(TAG, "addOnConnectionFailedListener $connectionResult")
            }
            .addApi(Auth.GOOGLE_SIGN_IN_API, gso.build())
            .build()
    }

    override fun login(origin: String, activity: Activity) {
        Log.d(TAG, "GoogleProvider.login")
        this.origin = origin
        val signInIntent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient)
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
            val account = task.getResult(ApiException::class.java)
            val authCode = account?.serverAuthCode
            Log.d(TAG, "onActivityResult authCode=$authCode")
            if (authCode != null) {
                loginWithProvider(authCode, origin, success, failure)
            } else {
                failure(ReachFiveError.from("No auth code")) // TODO better message
            }
        } catch (e: ApiException) {
            failure(ReachFiveError.from(e))
        }
    }

    private fun loginWithProvider(
        code: String,
        origin: String,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>
    ) {
        val loginProviderRequest = LoginProviderRequest(
            provider = name,
            clientId = sdkConfig.clientId,
            code = code,
            origin = origin,
            scope = (providerConfig.scope ?: setOf("openid")).joinToString(" ")
        )
        reachFiveApi.loginWithProvider(loginProviderRequest, SdkInfos.getQueries()).enqueue(ReachFiveApiCallback({ it.toAuthToken().fold(success, failure) }, failure))
    }

    override fun onStop() {
        googleApiClient.disconnect()
    }
}
