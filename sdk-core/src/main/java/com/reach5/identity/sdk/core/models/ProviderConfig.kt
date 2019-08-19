package com.reach5.identity.sdk.core.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ProviderConfig(
    val provider: String,
    val clientId: String,
    val clientSecret: String?,
    val scope: Set<String> = emptySet()
) : Parcelable
