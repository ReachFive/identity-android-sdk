package com.reach5.identity.sdk.core.models

import android.os.Parcel
import android.os.Parcelable

data class SdkConfig(
    val domain: String,
    val clientId: String
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(domain)
        parcel.writeString(clientId)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<SdkConfig> {
        override fun createFromParcel(parcel: Parcel): SdkConfig {
            return SdkConfig(parcel)
        }

        override fun newArray(size: Int): Array<SdkConfig?> {
            return arrayOfNulls(size)
        }
    }
}
