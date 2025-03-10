package co.reachfive.identity.sdk.core.models.responses

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class EmailVerification (
    @SerializedName("verification_email_sent")
    val verificationEmailSent: Boolean
): Parcelable