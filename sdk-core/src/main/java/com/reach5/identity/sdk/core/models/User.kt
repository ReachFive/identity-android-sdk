package com.reach5.identity.sdk.core.models

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

data class User(
    /**
     * Correspond au champ "sub" dans le JSON
     */
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
    /**
     * Date de Naissance au format ISO­8601 (par ex. 1965­12­31)
     */
    var birthdate: String?
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readValue(Boolean::class.java.classLoader) as? Boolean,
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readValue(Boolean::class.java.classLoader) as? Boolean,
        parcel.readParcelable(Address::class.java.classLoader),
        parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(preferredUsername)
        parcel.writeString(name)
        parcel.writeString(givenName)
        parcel.writeString(familyName)
        parcel.writeString(middleName)
        parcel.writeString(nickname)
        parcel.writeString(picture)
        parcel.writeString(website)
        parcel.writeString(email)
        parcel.writeValue(emailVerified)
        parcel.writeString(gender)
        parcel.writeString(zoneinfo)
        parcel.writeString(locale)
        parcel.writeString(phoneNumber)
        parcel.writeValue(phoneNumberVerified)
        parcel.writeParcelable(address, flags)
        parcel.writeString(birthdate)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<User> {
        override fun createFromParcel(parcel: Parcel): User {
            return User(parcel)
        }

        override fun newArray(size: Int): Array<User?> {
            return arrayOfNulls(size)
        }
    }
}