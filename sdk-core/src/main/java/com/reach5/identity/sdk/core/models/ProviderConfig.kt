package com.reach5.identity.sdk.core.models

import android.os.Parcel
import android.os.Parcelable

data class ProviderConfig(
    val provider: String,
    val clientId: String,
    val clientSecret: String?,
    val scope: Set<String>?
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString(),
        parcel.createStringArrayList()?.toSet() ?: setOf<String>()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(provider)
        parcel.writeString(clientId)
        parcel.writeString(clientSecret)
        parcel.writeStringList(scope?.toList())
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ProviderConfig> {
        override fun createFromParcel(parcel: Parcel): ProviderConfig {
            return ProviderConfig(parcel)
        }

        override fun newArray(size: Int): Array<ProviderConfig?> {
            return arrayOfNulls(size)
        }
    }
}

data class ProvidersConfigsResult(
    val items: Array<ProviderConfig>?,
    val status: String
)
