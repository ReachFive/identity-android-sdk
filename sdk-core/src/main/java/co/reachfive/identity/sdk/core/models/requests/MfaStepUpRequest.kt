package co.reachfive.identity.sdk.core.models.requests

import android.os.Parcelable
import co.reachfive.identity.sdk.core.models.CredentialMfaType
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

data class StartStepUpRequest(
    @SerializedName("response_type")
    val responseType: String = "code",
    @SerializedName("client_id")
    val clientId: String,
    @SerializedName("redirect_uri")
    val redirectUri: String,
    @SerializedName("code_challenge")
    val codeChallenge: String,
    @SerializedName("code_challenge_method")
    val codeChallengeMethod: String,
    val scope: String
)

@Parcelize
data class StartMfaPasswordlessRequest(
    @SerializedName("response_type")
    val responseType: String = "code",
    @SerializedName("redirect_uri")
    val redirectUri: String,
    @SerializedName("client_id")
    val clientId: String,
    @SerializedName("auth_type")
    val authType: CredentialMfaType,
    @SerializedName("step_up")
    val stepUp: String,
    val origin: String?,
) : Parcelable

@Parcelize
data class VerifyMfaPasswordlessRequest(
    @SerializedName("challenge_id")
    val challengeId: String,
    @SerializedName("verification_code")
    val verificationCode: String,
    @SerializedName("trust_device")
    val trustDevice: Boolean?
) : Parcelable