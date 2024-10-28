package co.reachfive.identity.sdk.core.models.requests

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class RevokeRequest (
    @SerializedName("client_id")
    val clientId: String,
    val token: String,
    @SerializedName("token_type_hint")
    val tokenTypeHint: String,
): Parcelable
