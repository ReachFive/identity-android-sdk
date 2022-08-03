package co.reachfive.identity.sdk.core.models.requests

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class LoginRequest(
    val email: String?,
    @SerializedName("phone_number")
    val phoneNumber: String?,
    val password: String,
    @SerializedName("client_id")
    val clientId: String,
    val scope: String
) : Parcelable