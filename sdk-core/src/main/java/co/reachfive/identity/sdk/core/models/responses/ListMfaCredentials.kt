package co.reachfive.identity.sdk.core.models.responses

import android.os.Parcelable
import co.reachfive.identity.sdk.core.models.CredentialMfaType
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
@Parcelize
data class ListMfaCredentials(
    val credentials: List<MfaCredential>
): Parcelable

@Parcelize
data class MfaCredential(
    @SerializedName("created_at")
    val createdAt: String?,
    @SerializedName("type")
    val type: CredentialMfaType,
    @SerializedName("friendly_name")
    val friendlyName: String,
    val email: String?,
    @SerializedName("phone_number")
    val phoneNumber: String?
): Parcelable