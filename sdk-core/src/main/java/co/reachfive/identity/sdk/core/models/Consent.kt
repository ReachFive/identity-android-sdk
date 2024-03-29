package co.reachfive.identity.sdk.core.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Consent(
    val granted: Boolean,
    @SerializedName("consent_type")
    val consentType: String?,
    val date: String
) : Parcelable