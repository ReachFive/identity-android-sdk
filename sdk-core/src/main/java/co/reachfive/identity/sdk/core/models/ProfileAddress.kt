package co.reachfive.identity.sdk.core.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

enum class ProfileAddressType {
    billing, delivery
}

@Parcelize
data class ProfileAddress(
    val title: String? = null,
    @SerializedName("default")
    val isDefault: Boolean?,
    @SerializedName("address_type")
    val addressType: ProfileAddressType? = null,
    @SerializedName("street_address")
    val streetAddress: String? = null,
    @SerializedName("address_complement")
    val addressComplement: String? = null,
    val locality: String? = null,
    val region: String? = null,
    @SerializedName("postal_code")
    val postalCode: String? = null,
    val country: String? = null,
    val raw: String? = null,
    @SerializedName("delivery_note")
    val deliveryNote: String? = null,
    val recipient: String? = null,
    val company: String? = null,
    @SerializedName("phone_number")
    val phoneNumber: String? = null
) : Parcelable