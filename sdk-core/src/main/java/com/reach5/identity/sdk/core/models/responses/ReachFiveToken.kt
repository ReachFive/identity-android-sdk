package com.reach5.identity.sdk.core.models.responses

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class AuthenticationToken(
    val tkn: String
) : Parcelable