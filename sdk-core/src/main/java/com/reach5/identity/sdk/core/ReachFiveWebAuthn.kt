package com.reach5.identity.sdk.core

import android.app.Activity
import android.content.Intent
import android.util.Log
import com.google.android.gms.fido.Fido
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAttestationResponse
import com.google.android.gms.fido.fido2.api.common.AuthenticatorErrorResponse
import com.reach5.identity.sdk.core.models.ReachFiveError
import com.reach5.identity.sdk.core.models.requests.webAuthn.RegistrationPublicKeyCredential
import com.reach5.identity.sdk.core.models.requests.webAuthn.WebAuthnRegistration
import com.reach5.identity.sdk.core.models.responses.webAuthn.RegistrationOptions
import com.reach5.identity.sdk.core.utils.Failure

class ReachFiveWebAuthn(val activity: Activity) {
    companion object {
        private const val TAG = "Reach5"
    }

    fun startFIDO2RegisterTask(
        registrationOptions: RegistrationOptions,
        requestCode: Int
    ) {
        val fido2ApiClient = Fido.getFido2ApiClient(activity)
        val fido2PendingIntentTask = fido2ApiClient.getRegisterPendingIntent(registrationOptions.toFido2Model())

        fido2PendingIntentTask.addOnSuccessListener { fido2PendingIntent ->
            if (fido2PendingIntent != null) {
                Log.d(TAG, "Launching Fido2 Pending Intent")
                activity.startIntentSenderForResult(fido2PendingIntent.intentSender, requestCode, null, 0, 0, 0)
            }
        }

        fido2PendingIntentTask.addOnFailureListener {
            throw ReachFiveError("FAILURE Launching Fido2 Pending Intent")
        }
    }

    fun extractFIDO2Error(intent: Intent, failure: Failure<ReachFiveError>) {
        val errorBytes = intent.getByteArrayExtra(Fido.FIDO2_KEY_ERROR_EXTRA)
        val authenticatorErrorResponse = AuthenticatorErrorResponse.deserializeFromBytes(errorBytes)
        val reachFiveError = ReachFiveError(
            message = authenticatorErrorResponse.errorMessage ?: "Unexpected error during FIDO2 registration",
            code = authenticatorErrorResponse.errorCodeAsInt
        )

        return failure(reachFiveError)
    }

    fun extractRegistrationPublicKeyCredential(intent: Intent): RegistrationPublicKeyCredential {
        val fido2Response = intent.getByteArrayExtra(Fido.FIDO2_KEY_RESPONSE_EXTRA)
        val authenticatorAttestationResponse = AuthenticatorAttestationResponse.deserializeFromBytes(fido2Response)

        return WebAuthnRegistration.createRegistrationPublicKeyCredential(
            authenticatorAttestationResponse
        )
    }
}