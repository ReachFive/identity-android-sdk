package com.reach5.identity.sdk.core.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class SdkConfig(
    val domain: String,
    val clientId: String,
    val scheme: String
) : Parcelable
