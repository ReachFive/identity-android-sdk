package co.reachfive.identity.sdk.core.models.responses

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class StartMfaPasswordlessResponse(
    @SerializedName("challenge_id")
    val challengeId: String,
) : Parcelable

@Parcelize
data class VerifyMfaPassordlessResponse(
    @SerializedName("code")
    val authCode: String
) : Parcelable