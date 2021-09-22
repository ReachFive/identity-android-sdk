package com.reach5.identity.sdk.core.models.requests

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.reach5.identity.sdk.core.models.Consent
import com.reach5.identity.sdk.core.models.ProfileAddress
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Parcelize
data class ProfileSignupRequest(
    val password: String,
    val email: String? = null,
    @SerializedName("phone_number")
    val phoneNumber: String? = null,
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
    val username: String? = null,
    val gender: String? = null,
    val company: String? = null,
    val addresses: List<ProfileAddress>? = null,
    val locale: String? = null,
    val bio: String? = null,
    @SerializedName("custom_fields")
    val customFields: Map<String, @RawValue Any>? = null,
    val consents: Map<String, Consent>? = null,
    @SerializedName("lite_only")
    val liteOnly: Boolean? = null
) : Parcelable {
    constructor(password: String, email: String) : this(password, email, null)
}

// `ProfileWebAuthnSignupRequest` has all `ProfileSignupRequest` properties except the password field
@Parcelize
data class ProfileWebAuthnSignupRequest(
    val email: String? = null,
    @SerializedName("phone_number")
    val phoneNumber: String? = null,
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
    val username: String? = null,
    val gender: String? = null,
    val company: String? = null,
    val addresses: List<ProfileAddress>? = null,
    val locale: String? = null,
    val bio: String? = null,
    @SerializedName("custom_fields")
    val customFields: Map<String, @RawValue Any>? = null,
    val consents: Map<String, Consent>? = null,
    @SerializedName("lite_only")
    val liteOnly: Boolean? = null
) : Parcelable