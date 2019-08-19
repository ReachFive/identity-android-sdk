package com.reach5.identity.sdk.core.api.responses

import android.os.Parcelable
import com.reach5.identity.sdk.core.models.ProviderConfig
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ProvidersConfigsResponse(
    val items: Array<ProviderConfig>?,
    val status: String
) : Parcelable
