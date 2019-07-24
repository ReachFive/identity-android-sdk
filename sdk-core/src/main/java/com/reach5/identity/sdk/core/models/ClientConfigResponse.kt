package com.reach5.identity.sdk.core.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ClientConfigResponse(
    val scope: String
) : Parcelable
