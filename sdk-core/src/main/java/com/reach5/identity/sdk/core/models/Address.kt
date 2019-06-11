package com.reach5.identity.sdk.core.models

import android.os.Parcel
import android.os.Parcelable

data class Address(
    val formatted: String,
    val streetAddress: String,
    val locality: String,
    val region: String,
    val postalCode: String,
    val country: String
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(formatted)
        parcel.writeString(streetAddress)
        parcel.writeString(locality)
        parcel.writeString(region)
        parcel.writeString(postalCode)
        parcel.writeString(country)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Address> {
        override fun createFromParcel(parcel: Parcel): Address {
            return Address(parcel)
        }

        override fun newArray(size: Int): Array<Address?> {
            return arrayOfNulls(size)
        }
    }
}
