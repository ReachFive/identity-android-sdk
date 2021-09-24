package co.reachfive.identity.sdk.core.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SdkConfig(
    val domain: String,
    val clientId: String,
    val scheme: String
) : Parcelable
