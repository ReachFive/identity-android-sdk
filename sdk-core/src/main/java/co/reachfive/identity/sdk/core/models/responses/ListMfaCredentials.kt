package co.reachfive.identity.sdk.core.models.responses

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

enum class Type(val value: String){
    SMS("sms"),
    EMAIL("email")
}

@Parcelize
data class ListMfaCredentials(
    val credentials: List<MfaCredential>
): Parcelable

@Parcelize
data class MfaCredential(
    @SerializedName("created_at")
    val createdAt: String?,
    @SerializedName("type")
    val type: Type,
    @SerializedName("friendly_name")
    val friendlyName: String,
    val email: String?,
    @SerializedName("phone_number")
    val phoneNumber: String?
): Parcelable