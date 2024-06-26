package co.reachfive.identity.sdk.core.models.responses.webAuthn

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class DeviceCredential(
    @SerializedName("friendly_name")
    val friendlyName: String,
    val id: String,
    @SerializedName("created_at")
    val createdAt: String?,
    @SerializedName("last_used_at")
    val lastUsedAt: String?,
    val aaguid: String?
) : Parcelable