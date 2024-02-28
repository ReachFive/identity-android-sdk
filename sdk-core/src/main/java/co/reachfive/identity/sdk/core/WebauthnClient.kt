package co.reachfive.identity.sdk.core

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import co.reachfive.identity.sdk.core.ReachFive.Companion.TAG
import co.reachfive.identity.sdk.core.WebauthnAuth.Companion.RC_LOGIN
import co.reachfive.identity.sdk.core.api.ReachFiveApi
import co.reachfive.identity.sdk.core.api.ReachFiveApiCallback
import co.reachfive.identity.sdk.core.models.AuthToken
import co.reachfive.identity.sdk.core.models.ReachFiveError
import co.reachfive.identity.sdk.core.models.SdkConfig
import co.reachfive.identity.sdk.core.models.SdkInfos
import co.reachfive.identity.sdk.core.models.requests.ProfileWebAuthnSignupRequest
import co.reachfive.identity.sdk.core.models.requests.webAuthn.*
import co.reachfive.identity.sdk.core.models.responses.AuthenticationToken
import co.reachfive.identity.sdk.core.models.responses.webAuthn.AuthenticationOptions
import co.reachfive.identity.sdk.core.models.responses.webAuthn.DeviceCredential
import co.reachfive.identity.sdk.core.models.responses.webAuthn.RegistrationOptions
import co.reachfive.identity.sdk.core.utils.Failure
import co.reachfive.identity.sdk.core.utils.Success
import com.google.android.gms.fido.Fido
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAssertionResponse
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAttestationResponse
import com.google.android.gms.fido.fido2.api.common.AuthenticatorErrorResponse
import com.google.android.gms.fido.fido2.api.common.ErrorCode

internal class WebauthnAuthClient(
    private val reachFiveApi: ReachFiveApi,
    private val sdkConfig: SdkConfig,
    private val sessionUtils: SessionUtilsClient,
) : WebauthnAuth {
    private var authToken: AuthToken? = null

    override var defaultScope: Set<String> = emptySet()

    override fun signupWithWebAuthn(
        profile: ProfileWebAuthnSignupRequest,
        originWebauthn: String,
        friendlyName: String?,
        origin: String?,
        success: Success<Unit>,
        failure: Failure<ReachFiveError>,
        activity: Activity
    ) {
        val newFriendlyName = formatFriendlyName(friendlyName)

        reachFiveApi
            .createWebAuthnSignupOptions(
                WebAuthnRegistrationRequest(originWebauthn, newFriendlyName, profile, sdkConfig.clientId),
                SdkInfos.getQueries() + if (origin != null) mapOf("origin" to origin) else emptyMap()
            )
            .enqueue(
                ReachFiveApiCallback.withContent<RegistrationOptions>(
                    success = { registrationOptions ->
                        startFIDO2RegisterTask(
                            registrationOptions,
                            WebauthnAuth.RC_SIGNUP,
                            failure,
                            activity
                        )
                        success(Unit)
                    },
                    failure = failure
                )
            )
    }

    internal fun onSignupWithWebAuthnResult(
        resultCode: Int,
        intent: Intent,
        scope: Collection<String>,
        origin: String?,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>,
        activity: Activity
    ) {
        when (resultCode) {
            Activity.RESULT_OK ->
                if (intent.hasExtra(Fido.FIDO2_KEY_ERROR_EXTRA))
                    failure(extractFIDO2Error(intent))
                else if (intent.hasExtra(Fido.FIDO2_KEY_RESPONSE_EXTRA))
                    handleSignupSuccess(intent, scope, origin, success, failure, activity)

            Activity.RESULT_CANCELED -> {
                Log.d(TAG, "Operation is cancelled")
                failure(ReachFiveError.WebauthnActionCanceled)
            }

            else -> {
                Log.e(TAG, "Operation failed, with resultCode: $resultCode")
                if (intent.hasExtra(Fido.FIDO2_KEY_ERROR_EXTRA))
                    failure(extractFIDO2Error(intent))
                else
                    failure(ReachFiveError.Unexpected)
            }
        }
    }

    private fun handleSignupSuccess(
        intent: Intent,
        scope: Collection<String>,
        origin: String?,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>,
        activity: Activity,
    ) {
        val webauthnId =
            activity
                .getSharedPreferences(SHAREDPREFS_NAME, Context.MODE_PRIVATE)
                .getString(SHAREDPREFS_USERID, null)

        if (webauthnId == null) {
            Log.e(TAG, "Could not retrieve Webauthn ID!")
            failure(ReachFiveError.from("Could not retrieve Webauthn ID!"))
        } else
            extractRegistrationPublicKeyCredential(intent)?.let { registrationPublicKeyCredential ->
                reachFiveApi
                    .signupWithWebAuthn(
                        WebauthnSignupCredential(
                            webauthnId = webauthnId,
                            publicKeyCredential = registrationPublicKeyCredential
                        ),
                        if (origin != null) mapOf("origin" to origin) else emptyMap()
                    )
                    .enqueue(
                        ReachFiveApiCallback.withContent<AuthenticationToken>(
                            success = {
                                sessionUtils.loginCallback(
                                    it.tkn,
                                    scope,
                                    success,
                                    failure
                                )
                            },
                            failure = failure
                        )
                    )
            }
    }

    override fun addNewWebAuthnDevice(
        authToken: AuthToken,
        originWebauthn: String,
        friendlyName: String?,
        failure: Failure<ReachFiveError>,
        activity: Activity
    ) {
        this.authToken = authToken
        val newFriendlyName = formatFriendlyName(friendlyName)

        reachFiveApi
            .createWebAuthnRegistrationOptions(
                authToken.authHeader,
                WebAuthnRegistrationRequest(originWebauthn, newFriendlyName)
            )
            .enqueue(
                ReachFiveApiCallback.withContent<RegistrationOptions>(
                    success = { registrationOptions ->
                        startFIDO2RegisterTask(
                            registrationOptions,
                            WebauthnAuth.RC_REGISTER_DEVICE,
                            failure,
                            activity
                        )
                    },
                    failure = failure
                )
            )
    }

    internal fun onAddNewWebAuthnDeviceResult(
        intent: Intent,
        success: Success<Unit>,
        failure: Failure<ReachFiveError>
    ) {
        if (intent.hasExtra(Fido.FIDO2_KEY_ERROR_EXTRA)) {
            failure(extractFIDO2Error(intent))
        } else if (intent.hasExtra(Fido.FIDO2_KEY_RESPONSE_EXTRA)) {
            val authToken = this.authToken
            if (authToken != null) {
                extractRegistrationPublicKeyCredential(intent)?.let { registrationPublicKeyCredential ->
                    reachFiveApi
                        .registerWithWebAuthn(
                            authToken.authHeader,
                            registrationPublicKeyCredential
                        )
                        .enqueue(ReachFiveApiCallback.noContent(success, failure))
                }
            } else failure(ReachFiveError.from("No auth token!"))
        }
    }

    override fun loginWithWebAuthn(
        loginRequest: WebAuthnLoginRequest,
        failure: Failure<ReachFiveError>,
        activity: Activity,
        origin: String?
    ) {
        reachFiveApi.createWebAuthnAuthenticationOptions(
            WebAuthnLoginRequest.enrichWithClientId(
                loginRequest,
                sdkConfig.clientId
            ),
            if (origin != null) mapOf("origin" to origin) else emptyMap()
        ).enqueue(
            ReachFiveApiCallback.withContent<AuthenticationOptions>(
                success = { authenticationOptions ->
                    val fido2ApiClient = Fido.getFido2ApiClient(activity)

                    val fido2PendingIntentTask =
                        fido2ApiClient.getSignPendingIntent(authenticationOptions.toFido2Model())

                    fido2PendingIntentTask.addOnSuccessListener { fido2PendingIntent ->
                        if (fido2PendingIntent != null) {
                            Log.d(TAG, "Launching Fido2 PendingIntent")
                            activity.startIntentSenderForResult(
                                fido2PendingIntent.intentSender,
                                RC_LOGIN,
                                null,
                                0,
                                0,
                                0
                            )
                        } else {
                            failure(ReachFiveError.from("Unexpected: null Fido2 PendingIntent"))
                        }
                    }

                    fido2PendingIntentTask.addOnFailureListener {
                        failure(ReachFiveError.from(it))
                    }
                },
                failure = failure
            )
        )
    }

    internal fun onLoginWithWebAuthnResult(
        resultCode: Int,
        intent: Intent,
        origin: String?,
        scope: Collection<String>,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>,
    ) {
        when (resultCode) {
            Activity.RESULT_OK ->
                if (intent.hasExtra(Fido.FIDO2_KEY_ERROR_EXTRA))
                    extractFIDO2Error(intent).let { failure(it) }
                else if (intent.hasExtra(Fido.FIDO2_KEY_RESPONSE_EXTRA))
                    handleLoginSuccess(intent, origin, scope, success, failure)

            Activity.RESULT_CANCELED -> {
                Log.d(TAG, "Operation is cancelled")
                failure(ReachFiveError.WebauthnActionCanceled)
            }

            else -> {
                Log.e(TAG, "Operation failed, with resultCode: $resultCode")
                if (intent.hasExtra(Fido.FIDO2_KEY_ERROR_EXTRA))
                    failure(extractFIDO2Error(intent))
                else
                    failure(ReachFiveError.Unexpected)
            }
        }

    }

    private fun handleLoginSuccess(
        intent: Intent,
        origin: String?,
        scope: Collection<String>,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>,
    ) {
        val fido2Response = intent.getByteArrayExtra(Fido.FIDO2_KEY_RESPONSE_EXTRA)

        val authenticatorAssertionResponse: AuthenticatorAssertionResponse =
            AuthenticatorAssertionResponse.deserializeFromBytes(fido2Response)

        val authenticationPublicKeyCredential: AuthenticationPublicKeyCredential =
            WebAuthnAuthentication.createAuthenticationPublicKeyCredential(
                authenticatorAssertionResponse
            )

        return reachFiveApi
            .authenticateWithWebAuthn(authenticationPublicKeyCredential, if (origin != null) mapOf("origin" to origin) else emptyMap())
            .enqueue(
                ReachFiveApiCallback.withContent<AuthenticationToken>(
                    success = { sessionUtils.loginCallback(it.tkn, scope, success, failure) },
                    failure = failure
                )
            )
    }

    override fun listWebAuthnDevices(
        authToken: AuthToken,
        success: Success<List<DeviceCredential>>,
        failure: Failure<ReachFiveError>
    ) =
        reachFiveApi
            .getWebAuthnRegistrations(authToken.authHeader, SdkInfos.getQueries())
            .enqueue(ReachFiveApiCallback.withContent<List<DeviceCredential>>(success, failure))

    override fun removeWebAuthnDevice(
        authToken: AuthToken,
        deviceId: String,
        success: Success<Unit>,
        failure: Failure<ReachFiveError>
    ) =
        reachFiveApi
            .deleteWebAuthnRegistration(
                authToken.authHeader,
                deviceId,
                SdkInfos.getQueries()
            )
            .enqueue(ReachFiveApiCallback.noContent(success, failure))

    private fun startFIDO2RegisterTask(
        registrationOptions: RegistrationOptions,
        requestCode: Int,
        failure: Failure<ReachFiveError>,
        activity: Activity
    ) {
        val fido2ApiClient = Fido.getFido2ApiClient(activity)
        val fido2PendingIntentTask =
            fido2ApiClient.getRegisterPendingIntent(registrationOptions.toFido2Model())

        activity
            .getSharedPreferences(SHAREDPREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .run {
                putString(SHAREDPREFS_USERID, registrationOptions.options.publicKey.user.id)
                apply()
            }

        fido2PendingIntentTask.addOnSuccessListener { fido2PendingIntent ->
            if (fido2PendingIntent != null) {
                Log.d(TAG, "Launching Fido2 Pending Intent")
                activity.startIntentSenderForResult(
                    fido2PendingIntent.intentSender,
                    requestCode,
                    null,
                    0,
                    0,
                    0
                )
            }
        }

        fido2PendingIntentTask.addOnFailureListener {
            failure(ReachFiveError("FAILURE Launching Fido2 Pending Intent"))
        }
    }

    private companion object {
        const val SHAREDPREFS_NAME = "webauthn"
        const val SHAREDPREFS_USERID = "user_id"

        fun formatFriendlyName(friendlyName: String?): String {
            return if (friendlyName.isNullOrEmpty()) android.os.Build.MODEL else friendlyName
        }

        fun extractFIDO2Error(intent: Intent): ReachFiveError {
            return intent
                .getByteArrayExtra(Fido.FIDO2_KEY_ERROR_EXTRA)
                ?.let { errorBytes ->
                    AuthenticatorErrorResponse.deserializeFromBytes(errorBytes)
                }
                ?.run {
                    ReachFiveError(
                        message = errorMessage ?: "Unexpected error during FIDO2 authentication",
                        code = errorCodeAsInt
                    )
                } ?: ReachFiveError(
                message = "Unexpected error during FIDO2 process.",
                code = ErrorCode.UNKNOWN_ERR.code
            )
        }

        fun extractRegistrationPublicKeyCredential(intent: Intent): RegistrationPublicKeyCredential? {
            return intent
                .getByteArrayExtra(Fido.FIDO2_KEY_RESPONSE_EXTRA)
                ?.let {
                    AuthenticatorAttestationResponse.deserializeFromBytes(it)
                }
                ?.let {
                    WebAuthnRegistration.createRegistrationPublicKeyCredential(it)
                }
        }
    }
}

internal interface WebauthnAuth {
    companion object {
        const val RC_SIGNUP = 31001
        const val RC_LOGIN = 31002
        const val RC_REGISTER_DEVICE = 31003

        fun isWebauthnLoginRequestCode(code: Int): Boolean =
            setOf(
                RC_SIGNUP,
                RC_LOGIN,
                RC_REGISTER_DEVICE
            ).any { it == code }

        fun isWebauthnActionRequestCode(code: Int): Boolean =
            RC_REGISTER_DEVICE == code
    }

    var defaultScope: Set<String>

    fun signupWithWebAuthn(
        profile: ProfileWebAuthnSignupRequest,
        originWebauthn: String,
        friendlyName: String?,
        origin: String? = null,
        success: Success<Unit>,
        failure: Failure<ReachFiveError>,
        activity: Activity
    )

    fun addNewWebAuthnDevice(
        authToken: AuthToken,
        originWebauthn: String,
        friendlyName: String?,
        failure: Failure<ReachFiveError>,
        activity: Activity
    )

    fun loginWithWebAuthn(
        loginRequest: WebAuthnLoginRequest,
        failure: Failure<ReachFiveError>,
        activity: Activity,
        origin: String? = null
    )

    fun listWebAuthnDevices(
        authToken: AuthToken,
        success: Success<List<DeviceCredential>>,
        failure: Failure<ReachFiveError>
    )

    fun removeWebAuthnDevice(
        authToken: AuthToken,
        deviceId: String,
        success: Success<Unit>,
        failure: Failure<ReachFiveError>
    )
}