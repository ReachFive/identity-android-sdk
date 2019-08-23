package com.reach5.identity.sdk.core.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ProfileAddress(
    val title: String?,
    @SerializedName("default")
    val isDefault: Boolean?,
    @SerializedName("address_type")
    val addressType: String?,
    @SerializedName("street_address")
    val streetAddress: String?,
    val locality: String?,
    val region: String?,
    @SerializedName("postal_code")
    val postalCode: String?,
    val country: String?,
    val raw: String?,
    @SerializedName("delivery_note")
    val deliveryNote: String?,
    val recipient: String?,
    val company: String?,
    @SerializedName("phone_number")
    val phoneNumber: String?
) : Parcelable