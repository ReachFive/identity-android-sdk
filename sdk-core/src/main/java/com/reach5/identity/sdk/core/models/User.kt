package com.reach5.identity.sdk.core.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class User(
    // Correspond au champ "sub" dans le JSON
    @SerializedName("sub")
    val id: String?,
    @SerializedName("preferred_username")
    val preferredUsername: String?,
    val name: String?,
    @SerializedName("given_name")
    val givenName: String?,
    @SerializedName("family_name")
    val familyName: String?,
    @SerializedName("middle_name")
    val middleName: String?,
    val nickname: String?,
    val picture: String?,
    val website: String?,
    val email: String?,
    @SerializedName("email_verified")
    val emailVerified: Boolean?,
    val gender: String?,
    val zoneinfo: String?,
    val locale: String?,
    @SerializedName("phone_number")
    val phoneNumber: String?,
    @SerializedName("phone_number_verified")
    val phoneNumberVerified: Boolean?,
    val address: Address?,
    // Date de naissance au format ISO­8601 (par ex. 1965­12­31)
    var birthdate: String?
) : Parcelable

@Parcelize
data class Address(
    val formatted: String,
    val streetAddress: String,
    val locality: String,
    val region: String,
    val postalCode: String,
    val country: String
) : Parcelable
