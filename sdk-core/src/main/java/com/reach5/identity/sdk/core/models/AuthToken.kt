package com.reach5.identity.sdk.core.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class AuthToken(
    // If the `openid` scope is not provided, the `idToken` is not returned
    val idToken: String? = null,
    val accessToken: String,
    val refreshToken: String?,
    val tokenType: String?,
    val expiresIn: Int?,
    // The `user` field is optional because if the `openid` scope is not provided, the `user` is not retrieved
    val user: OpenIdUser? = null
) : Parcelable
