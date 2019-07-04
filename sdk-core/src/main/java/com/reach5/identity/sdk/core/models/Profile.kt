package com.reach5.identity.sdk.core.models

import com.google.gson.annotations.SerializedName

/**
 * This class is used for sign-up and update, that's why some parameters are optional while there are required for some actions
 * Example: the `password` field is optional while it's required for sign-up
 */
data class Profile(
    val email: String? = null,
    val password: String? = null,
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