package co.reachfive.identity.sdk.core.models.requests

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class RefreshRequest(
    @SerializedName("client_id")
    val clientId: String,
    @SerializedName("refresh_token")
    val refreshToken: String,
    @SerializedName("redirect_uri")
    val redirectUri: String
) : Parcelable {
    @SerializedName("grant_type")
    val grantType: String = "refresh_token"
}
