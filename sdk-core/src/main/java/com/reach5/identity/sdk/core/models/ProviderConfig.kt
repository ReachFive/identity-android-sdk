package com.reach5.identity.sdk.core.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ProviderConfig(
    val provider: String,
    val clientId: String,
    val scope: Set<String> = emptySet()
) : Parcelable

@Parcelize
data class ProvidersConfigsResult(
    val items: Array<ProviderConfig>?,
    val status: String
) : Parcelable
