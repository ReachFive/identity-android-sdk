package co.reachfive.identity.sdk.core.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

/**
 * The user's information contained in the ID token.
 */
@Parcelize
data class OpenIdUser(
    /**
     * The identifier of the user. Corresponds to the `sub` claim.
     */
    @SerializedName("sub")
    val id: String?,
    /**
     * The full name of the user in displayable form.
     */
    val name: String?,
    /**
     * The shorthand name by which the user wishes to be referred to.
     */
    @SerializedName("preferred_username")
    val preferredUsername: String?,
    /**
     * The given name or first name of the user.
     */
    @SerializedName("given_name")
    val givenName: String?,
    /**
     * The surname or last name of the user.
     */
    @SerializedName("family_name")
    val familyName: String?,
    /**
     * The middle name of the user.
     */
    @SerializedName("middle_name")
    val middleName: String?,
    /**
     * The casual name of the user, which may differ from the given name.
     */
    val nickname: String?,
    /**
     * The URL of the user's profile picture.
     */
    val picture: String?,
    /**
     * The URL of the user's web page or blog.
     */
    val website: String?,
    /**
     * The user's preferred email address.
     */
    val email: String?,
    /**
     * `true` if the user's email address has been verified.
     */
    @SerializedName("email_verified")
    val emailVerified: Boolean?,
    /**
     * The user's gender.
     */
    val gender: String?,
    /**
     * The user's time zone from the zoneinfo database.
     */
    val zoneinfo: String?,
    /**
     * The user's locale (language and country code).
     */
    val locale: String?,
    /**
     * The user's preferred telephone number.
     */
    @SerializedName("phone_number")
    val phoneNumber: String?,
    /**
     * `true` if the user's phone number has been verified.
     */
    @SerializedName("phone_number_verified")
    val phoneNumberVerified: Boolean?,
    /**
     * The user's preferred postal address.
     */
    val address: Address?,
    /**
     * Date of birth in ISO-8601 format (for example `1965-12-31`).
     */
    var birthdate: String?,
    /**
     * An identifier for the user from an external system.
     */
    @SerializedName("external_id")
    val externalId: String?
) : Parcelable

@Parcelize
data class Address(
    val formatted: String?,
    val streetAddress: String?,
    val locality: String?,
    val region: String?,
    val postalCode: String?,
    val country: String?
) : Parcelable
