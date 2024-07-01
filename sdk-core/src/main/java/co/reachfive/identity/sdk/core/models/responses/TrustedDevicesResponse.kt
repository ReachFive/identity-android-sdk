package co.reachfive.identity.sdk.core.models.responses

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class ListMfaTrustedDevices(
    @SerializedName("trusted_devices")
    val trustedDevices: List<TrustedDevice>
): Parcelable

@Parcelize
data class TrustedDevice(
    val id: String,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("created_at")
    val createdAt: String,
    val metadata: DeviceMetadata
): Parcelable

@Parcelize
data class DeviceMetadata(
    val ip: String?,
    @SerializedName("operating_system")
    val operatingSystem: String?,
    @SerializedName("user_agent")
    val userAgent: String?,
    @SerializedName("device_class")
    val deviceClass: String?,
    @SerializedName("device_name")
    val deviceName: String?
): Parcelable