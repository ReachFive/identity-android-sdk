package com.reach5.identity.sdk.core

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class SdkConfig(
    val domain: String,
    val clientId: String
) : Parcelable {
    companion object {
        val REDIRECT_URI: String = "reachfive://callback"
    }
}
