package com.reach5.identity.sdk.core.models

import com.google.gson.annotations.SerializedName

data class Profile(
    val email: String? = null,
    val password: String,
    @SerializedName("phone_number")
    val phoneNumber: String? = null,
    val gender: String? = null,
    val name: String? = null,
    @SerializedName("given_name")
    val givenName: String? = null,
    @SerializedName("middle_name")
    val middleName: String? = null,
    @SerializedName("family_name")
    val familyName: String? = null,
    val nickname: String? = null,
    val username: String? = null,
    val birthdate: String? = null,
    val picture: String? = null,
    val company: String? = null,
    val locale: String? = null,
    // TODO better type for address
    val addresses: List<ProfileAddress>? = null,
    @SerializedName("custom_fields")
    val customFields: Map<String, Any>? = null
) {
    constructor(email: String, password: String): this(email, password, null)
}

data class ProfileAddress(
    val country: String
)

data class SignupRequest(
    @SerializedName("client_id")
    val clientId: String,
    val data: Profile,
    val scope: String,
    @SerializedName("accept_tos")
    val acceptTos: Boolean? = null
)
