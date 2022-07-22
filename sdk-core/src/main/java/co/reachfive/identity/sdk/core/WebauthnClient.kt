package co.reachfive.identity.sdk.core

import android.app.Activity
import android.content.Intent
import android.util.Log
import co.reachfive.identity.sdk.core.api.ReachFiveApi
import co.reachfive.identity.sdk.core.api.ReachFiveApiCallback
import co.reachfive.identity.sdk.core.models.AuthToken
import co.reachfive.identity.sdk.core.models.ReachFiveError
import co.reachfive.identity.sdk.core.models.SdkConfig
import co.reachfive.identity.sdk.core.models.SdkInfos
import co.reachfive.identity.sdk.core.models.requests.ProfileWebAuthnSignupRequest
import co.reachfive.identity.sdk.core.models.requests.webAuthn.*
import co.reachfive.identity.sdk.core.models.responses.webAuthn.DeviceCredential
import co.reachfive.identity.sdk.core.models.responses.webAuthn.RegistrationOptions
import co.reachfive.identity.sdk.core.utils.Failure
import co.reachfive.identity.sdk.core.utils.Success
import co.reachfive.identity.sdk.core.utils.SuccessWithNoContent
import com.google.android.gms.fido.Fido
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAssertionResponse
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAttestationResponse
import com.google.android.gms.fido.fido2.api.common.AuthenticatorErrorResponse
import com.google.android.gms.fido.fido2.api.common.ErrorCode

internal class WebauthnAuthClient(
    private val reachFiveApi: ReachFiveApi,
    private val sdkConfig: SdkConfig,
    private val activity: Activity,
    private val oAuthClient: ReachFiveOAuthClient,
) : WebauthnAuth {
    override var defaultScope: Set<String> = emptySet()

    override fun signupWithWebAuthn(
        profile: ProfileWebAuthnSignupRequest,
        origin: String,
        friendlyName: String?,
        signupRequestCode: Int,
        successWithWebAuthnId: Success<String>,
        failure: Failure<ReachFiveError>
    ) {
        val newFriendlyName = formatFriendlyName(friendlyName)

        reachFiveApi
            .createWebAuthnSignupOptions(
                WebAuthnRegistrationRequest(origin, newFriendlyName, profile, sdkConfig.clientId),
                SdkInfos.getQueries()
            )
            .enqueue(
                ReachFiveApiCallback(
                    success = {
                        startFIDO2RegisterTask(it, signupRequestCode)
                        successWithWebAuthnId(it.options.publicKey.user.id)
                    },
                    failure = failure
                )
            )
    }

    override fun onSignupWithWebAuthnResult(
        intent: Intent,
        webAuthnId: String,
        scope: Collection<String>,
        failure: Failure<ReachFiveError>
    ) {
        if (intent.hasExtra(Fido.FIDO2_KEY_ERROR_EXTRA)) {
            failure(extractFIDO2Error(intent))
        } else if (intent.hasExtra(Fido.FIDO2_KEY_RESPONSE_EXTRA)) {
            extractRegistrationPublicKeyCredential(intent)?.let { registrationPublicKeyCredential ->
                reachFiveApi
                    .signupWithWebAuthn(
                        WebauthnSignupCredential(
                            webauthnId = webAuthnId,
                            publicKeyCredential = registrationPublicKeyCredential
                        )
                    )
                    .enqueue(
                        ReachFiveApiCallback(
                            success = { oAuthClient.loginCallback(it.tkn, scope) },
                            failure = failure
                        )
                    )
            }
        }
    }

    override fun addNewWebAuthnDevice(
        authToken: AuthToken,
        origin: String,
        friendlyName: String?,
        registerRequestCode: Int,
        failure: Failure<ReachFiveError>
    ) {
        val newFriendlyName = formatFriendlyName(friendlyName)

        reachFiveApi
            .createWebAuthnRegistrationOptions(
                authToken.authHeader,
                WebAuthnRegistrationRequest(origin, newFriendlyName)
            )
            .enqueue(
                ReachFiveApiCallback(
                    success = { startFIDO2RegisterTask(it, registerRequestCode) },
                    failure = failure
                )
            )
    }

    override fun onAddNewWebAuthnDeviceResult(
        authToken: AuthToken,
        intent: Intent,
        successWithNoContent: SuccessWithNoContent<Unit>,
        failure: Failure<ReachFiveError>
    ) {
        if (intent.hasExtra(Fido.FIDO2_KEY_ERROR_EXTRA)) {
            failure(extractFIDO2Error(intent))
        } else if (intent.hasExtra(Fido.FIDO2_KEY_RESPONSE_EXTRA)) {
            extractRegistrationPublicKeyCredential(intent)?.let { registrationPublicKeyCredential ->
                reachFiveApi
                    .registerWithWebAuthn(
                        authToken.authHeader,
                        registrationPublicKeyCredential
                    )
                    .enqueue(
                        ReachFiveApiCallback(
                            successWithNoContent = successWithNoContent,
                            failure = failure
                        )
                    )
            }
        }
    }

    override fun loginWithWebAuthn(
        loginRequest: WebAuthnLoginRequest,
        loginRequestCode: Int,
        failure: Failure<ReachFiveError>
    ) = reachFiveApi.createWebAuthnAuthenticationOptions(
        WebAuthnLoginRequest.enrichWithClientId(
            loginRequest,
            sdkConfig.clientId
        )
    ).enqueue(
        ReachFiveApiCallback(
            success = {
                val fido2ApiClient = Fido.getFido2ApiClient(activity)
                val fido2PendingIntentTask = fido2ApiClient.getSignPendingIntent(it.toFido2Model())
                fido2PendingIntentTask.addOnSuccessListener { fido2PendingIntent ->
                    if (fido2PendingIntent != null) {
                        Log.d(TAG, "Launching Fido2 Pending Intent")
                        activity.startIntentSenderForResult(
                            fido2PendingIntent.intentSender,
                            loginRequestCode,
                            null,
                            0,
                            0,
                            0
                        )
                    }
                }
                fido2PendingIntentTask.addOnFailureListener {
                    throw ReachFiveError("FAILURE Launching Fido2 Pending Intent")
                }
            },
            failure = failure
        )
    )

    override fun onLoginWithWebAuthnResult(
        intent: Intent,
        scope: Collection<String>,
        failure: Failure<ReachFiveError>
    ) {
        if (intent.hasExtra(Fido.FIDO2_KEY_ERROR_EXTRA)) {
            extractFIDO2Error(intent).let { failure(it) }
        } else if (intent.hasExtra(Fido.FIDO2_KEY_RESPONSE_EXTRA)) {
            val fido2Response = intent.getByteArrayExtra(Fido.FIDO2_KEY_RESPONSE_EXTRA)
            val authenticatorAssertionResponse: AuthenticatorAssertionResponse =
                AuthenticatorAssertionResponse.deserializeFromBytes(fido2Response)
            val authenticationPublicKeyCredential: AuthenticationPublicKeyCredential =
                WebAuthnAuthentication.createAuthenticationPublicKeyCredential(
                    authenticatorAssertionResponse
                )

            return reachFiveApi
                .authenticateWithWebAuthn(authenticationPublicKeyCredential)
                .enqueue(
                    ReachFiveApiCallback(
                        success = { oAuthClient.loginCallback(it.tkn, scope) },
                        failure = failure
                    )
                )
        }
    }

    override fun listWebAuthnDevices(
        authToken: AuthToken,
        success: Success<List<DeviceCredential>>,
        failure: Failure<ReachFiveError>
    ) =
        reachFiveApi
            .getWebAuthnRegistrations(authToken.authHeader, SdkInfos.getQueries())
            .enqueue(ReachFiveApiCallback(success = success, failure = failure))

    override fun removeWebAuthnDevice(
        authToken: AuthToken,
        deviceId: String,
        successWithNoContent: SuccessWithNoContent<Unit>,
        failure: Failure<ReachFiveError>
    ) =
        reachFiveApi
            .deleteWebAuthnRegistration(
                authToken.authHeader,
                deviceId,
                SdkInfos.getQueries()
            )
            .enqueue(
                ReachFiveApiCallback(
                    successWithNoContent = successWithNoContent,
                    failure = failure
                )
            )

    override fun startFIDO2RegisterTask(
        registrationOptions: RegistrationOptions,
        requestCode: Int
    ) {
        val fido2ApiClient = Fido.getFido2ApiClient(activity)
        val fido2PendingIntentTask =
            fido2ApiClient.getRegisterPendingIntent(registrationOptions.toFido2Model())

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
            throw ReachFiveError("FAILURE Launching Fido2 Pending Intent")
        }
    }

    private companion object {
        const val TAG = "Reach5"

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
                message = "Unexpected error during FIDO2 authentication",
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
    var defaultScope: Set<String>

    fun signupWithWebAuthn(
        profile: ProfileWebAuthnSignupRequest,
        origin: String,
        friendlyName: String?,
        signupRequestCode: Int,
        successWithWebAuthnId: Success<String>,
        failure: Failure<ReachFiveError>
    )

    fun onSignupWithWebAuthnResult(
        intent: Intent,
        webAuthnId: String,
        scope: Collection<String> = defaultScope,
        failure: Failure<ReachFiveError>
    )

    fun addNewWebAuthnDevice(
        authToken: AuthToken,
        origin: String,
        friendlyName: String?,
        registerRequestCode: Int,
        failure: Failure<ReachFiveError>
    )

    fun onAddNewWebAuthnDeviceResult(
        authToken: AuthToken,
        intent: Intent,
        successWithNoContent: SuccessWithNoContent<Unit>,
        failure: Failure<ReachFiveError>
    )

    fun loginWithWebAuthn(
        loginRequest: WebAuthnLoginRequest,
        loginRequestCode: Int,
        failure: Failure<ReachFiveError>
    )

    fun onLoginWithWebAuthnResult(
        intent: Intent,
        scope: Collection<String> = defaultScope,
        failure: Failure<ReachFiveError>
    )

    fun listWebAuthnDevices(
        authToken: AuthToken,
        success: Success<List<DeviceCredential>>,
        failure: Failure<ReachFiveError>
    )

    fun removeWebAuthnDevice(
        authToken: AuthToken,
        deviceId: String,
        successWithNoContent: SuccessWithNoContent<Unit>,
        failure: Failure<ReachFiveError>
    )

    fun startFIDO2RegisterTask(
        registrationOptions: RegistrationOptions,
        requestCode: Int
    )
}