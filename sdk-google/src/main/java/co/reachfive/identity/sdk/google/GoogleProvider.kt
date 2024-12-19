package co.reachfive.identity.sdk.google

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.CancellationSignal
import android.util.Base64
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialManagerCallback
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.GetCredentialException
import co.reachfive.identity.sdk.core.Provider
import co.reachfive.identity.sdk.core.ProviderCreator
import co.reachfive.identity.sdk.core.ReachFive.Companion.TAG
import co.reachfive.identity.sdk.core.RedirectionActivity
import co.reachfive.identity.sdk.core.SessionUtilsClient
import co.reachfive.identity.sdk.core.models.AuthToken
import co.reachfive.identity.sdk.core.models.ProviderConfig
import co.reachfive.identity.sdk.core.models.ReachFiveError
import co.reachfive.identity.sdk.core.models.SdkConfig
import co.reachfive.identity.sdk.core.utils.Failure
import co.reachfive.identity.sdk.core.utils.Success
import co.reachfive.identity.sdk.google.GoogleProvider.Companion.PERMISSIONS_REQUEST_GET_ACCOUNTS
import co.reachfive.identity.sdk.google.GoogleProvider.Companion.REQUEST_CODE
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import java.security.MessageDigest
import java.security.SecureRandom


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
        sdkConfig: SdkConfig
    ): Provider {
        return ConfiguredGoogleProvider(providerConfig, sessionUtils, context, sdkConfig)
    }
}

internal class ConfiguredGoogleProvider(
    private val providerConfig: ProviderConfig,
    private val sessionUtils: SessionUtilsClient,
    private val context: Context,
    private val sdkConfig: SdkConfig,
) : Provider {
    private lateinit var origin: String
    private lateinit var scope: Collection<String>

    override val requestCode: Int = REQUEST_CODE
    override val name: String = GoogleProvider.NAME

    private val credentialManager: CredentialManager = CredentialManager.create(this.context)
    private val md = MessageDigest.getInstance("SHA-256")
    private val b64Flags = Base64.URL_SAFE + Base64.NO_WRAP + Base64.NO_PADDING

    override fun login(origin: String, scope: Collection<String>, activity: Activity) {
        this.origin = origin
        this.scope = scope

        // Going through the redirection activity is useless,
        // this is merely a trick to preserve the Provider interface for now
        val intent = Intent(activity, RedirectionActivity::class.java)
        intent.putExtra(RedirectionActivity.PROVIDER_KEY, providerConfig.provider)
        intent.putExtra(RedirectionActivity.SCHEME, sdkConfig.scheme)
        intent.data = Uri.parse(sdkConfig.scheme)

        activity.startActivityForResult(intent, REQUEST_CODE)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>
    ) {
        val nonce = SecureRandom()
            .let { secureRandom -> ByteArray(32).also { secureRandom.nextBytes(it) } }
            .let { Base64.encodeToString(it, b64Flags) }

        val nonceHash = md.digest(nonce.toByteArray()).let { Base64.encodeToString(it, b64Flags) }

        val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(this.providerConfig.clientId)
            .setAutoSelectEnabled(true)
            .setNonce(nonceHash)
            .build()

        val cancellationSignal = CancellationSignal()

        credentialManager.getCredentialAsync(
            context = context,
            request = GetCredentialRequest(listOf(googleIdOption)),
            cancellationSignal = cancellationSignal,
            executor = ContextCompat.getMainExecutor(context),
            callback = object :
                CredentialManagerCallback<GetCredentialResponse, GetCredentialException> {

                override fun onError(e: GetCredentialException) {
                    Log.d(TAG, "Error retrieving Google credentials", e)
                    failure(ReachFiveError.from(e))
                }

                override fun onResult(result: GetCredentialResponse) {
                    when (val credential = result.credential) {
                        is CustomCredential -> {
                            if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)
                                try {
                                    val idToken =
                                        GoogleIdTokenCredential.createFrom(credential.data).idToken

                                    sessionUtils.idTokenCallback(
                                        provider = providerConfig.provider,
                                        idToken = idToken,
                                        nonce = nonce,
                                        success = success,
                                        failure = failure,
                                        scope = scope,
                                        origin = origin
                                    )
                                } catch (e: GoogleIdTokenParsingException) {
                                    Log.d(TAG, "Received an invalid google id token response", e)
                                    failure(ReachFiveError.from(e))
                                }
                            else {
                                Log.d(TAG, "Unexpected type of custom credential")
                                failure(ReachFiveError.from("Unexpected type of custom credential"))
                            }
                        }

                        else -> {
                            Log.d(TAG, "Unexpected type of credential")
                            failure(ReachFiveError.from("Unexpected type of credential"))
                        }
                    }
                }
            }
        )
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

    override fun logout() {
        val cancellationSignal = CancellationSignal()

        credentialManager.clearCredentialStateAsync(
            request = ClearCredentialStateRequest(),
            cancellationSignal = cancellationSignal,
            executor = ContextCompat.getMainExecutor(context),
            callback = object: CredentialManagerCallback<Void?, ClearCredentialException> {

                override fun onError(e: ClearCredentialException) {
                    Log.e(TAG, "Error logging out from Google", e)
                }

                override fun onResult(result: Void?) {
                    Log.d(TAG, "Successful logout from Google")
                }

            }
        )
    }
}
