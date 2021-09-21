package co.reachfive.identity.sdk.core.models.responses

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ClientConfigResponse(
    val scope: String
) : Parcelable
