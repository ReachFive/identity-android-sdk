package co.reachfive.identity.sdk.core.models.requests

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class UpdateEmailRequest(
    val email: String,
    @SerializedName("redirect_url")
    val redirectUrl: String? = null,
    @SerializedName("captcha_token")
    val captchaToken: String? = null,
    @SerializedName("captcha_provider")
    val captchaProvider: String? = null
) : Parcelable
