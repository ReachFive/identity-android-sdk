package co.reachfive.identity.sdk.core.models.responses

import android.os.Parcelable
import co.reachfive.identity.sdk.core.models.CredentialMfaType
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class StepUpResponse(
    val amr: List<String>,
    val token: String
): Parcelable

@Parcelize
data class StartMfaPasswordlessResponse(
    @SerializedName("challenge_id")
    val challengeId: String,
    @SerializedName("credential_type")
    val credentialType: CredentialMfaType
): Parcelable

@Parcelize
data class VerifyMfaPassordlessResponse(
    @SerializedName("code")
    val authCode: String
): Parcelable