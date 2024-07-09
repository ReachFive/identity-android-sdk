package co.reachfive.identity.sdk.core.models.responses

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class AuthenticationToken(
    val tkn: String,
    @SerializedName("mfa_required")
    val mfaRequired: Boolean? = null
) : Parcelable