package co.reachfive.identity.sdk.core.models.responses

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class SessionDevicesResponse(
    @SerializedName("session_devices")
    val sessionDevices: List<SessionDevice>
): Parcelable

enum class TokenType {
    RT,
    ST
}

@Parcelize
data class SessionDevice(
    val id: String,
    @SerializedName("token_type")
    val tokenType: TokenType,
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
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("last_connection")
    val lastConnection: String,
    @SerializedName("expires_at")
    val expiresAt: String,
): Parcelable