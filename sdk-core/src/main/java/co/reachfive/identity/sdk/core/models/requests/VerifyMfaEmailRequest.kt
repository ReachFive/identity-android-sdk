package co.reachfive.identity.sdk.core.models.requests

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class VerifyMfaEmailRequest(
    @SerializedName("verification_code")
    val verificationCode: String
) : Parcelable

@Parcelize
data class VerifyEmailRequest(
    @SerializedName("verification_code")
    val verificationCode: String,
    val email: String
) : Parcelable

@Parcelize
data class SendVerificationEmailRequest(
    @SerializedName("redirect_url")
    val redirectUrl: String?,
    @SerializedName("redirect_to_after_email_confirmation")
    val redirectToAfterEmailConfirmation: String?
): Parcelable

