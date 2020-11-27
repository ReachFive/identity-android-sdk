package com.reach5.identity.sdk.core.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue

@Parcelize
data class Profile(
    val uid: String? = null,
    @SerializedName("given_name")
    val givenName: String? = null,
    @SerializedName("middle_name")
    val middleName: String? = null,
    @SerializedName("family_name")
    val familyName: String? = null,
    val name: String? = null,
    val nickname: String? = null,
    val birthdate: String? = null,
    @SerializedName("profile_url")
    val profileURL: String? = null,
    val picture: String? = null,
    @SerializedName("external_id")
    val externalId: String? = null,
    @SerializedName("auth_types")
    val authTypes: List<String>? = null,
    @SerializedName("login_summary")
    val loginSummary: LoginSummary? = null,
    val username: String? = null,
    val gender: String? = null,
    val email: String? = null,
    @SerializedName("email_verified")
    val emailVerified: Boolean? = null,
    val emails: Emails? = null,
    @SerializedName("phone_number")
    val phoneNumber: String? = null,
    @SerializedName("phone_number_verified")
    val phoneNumberVerified: Boolean? = null,
    val addresses: List<ProfileAddress>? = null,
    val locale: String? = null,
    val bio: String? = null,
    @SerializedName("custom_fields")
    val customFields: Map<String, @RawValue Any>? = null,
    val consents: Map<String, Consent>? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null,
    @SerializedName("lite_only")
    val liteOnly: Boolean? = null,
    val company: String? = null
) : Parcelable

@Parcelize
data class LoginSummary(
    @SerializedName("first_login")
    val firstLogin: Long?,
    @SerializedName("last_login")
    val lastLogin: Long?,
    val total: Int?,
    val origins: List<String>?,
    val devices: List<String>?,
    @SerializedName("last_provider")
    val lastProvider: String?
) : Parcelable

@Parcelize
data class Emails(
    val verified: List<String>?,
    val unverified: List<String>?
) : Parcelable
