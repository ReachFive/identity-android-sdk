package co.reachfive.identity.sdk.core.models.responses

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class ListSessionDevices(
    val sessionDevices: List<SessionDevice>
): Parcelable

@Parcelize
data class SessionDevice(
    val id: String,
    val ip: String?,
    val country: String?,
    val city: String?,
    @SerializedName("operating_system")
    val operatingSystem: String?,
    @SerializedName("user_agent_name")
    val userAgentName: String?,
    @SerializedName("device_class")
    val deviceClass: String?,
    @SerializedName("device_name")
    val deviceName: String?,
    @SerializedName("first_connection")
    val firstConnection: String,
    @SerializedName("last_connection")
    val lastConnection: String
): Parcelable