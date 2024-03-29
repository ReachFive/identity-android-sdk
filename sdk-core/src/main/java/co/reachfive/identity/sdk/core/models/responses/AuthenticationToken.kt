package co.reachfive.identity.sdk.core.models.responses

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AuthenticationToken(
    val tkn: String
) : Parcelable