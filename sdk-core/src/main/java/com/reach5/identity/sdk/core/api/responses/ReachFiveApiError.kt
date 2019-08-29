package com.reach5.identity.sdk.core.api.responses

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ReachFiveApiError(
    val error: String,

    @SerializedName("error_id")
    val errorId: String?,

    @SerializedName("error_user_msg")
    val errorUserMsg: String?,

    @SerializedName("error_description")
    val errorDescription: String? ,

    @SerializedName("error_details")
    val errorDetails: List<ReachFiveApiErrorDetail>?
) : Parcelable

@Parcelize
data class ReachFiveApiErrorDetail(
    val field: String,
    val message: String
) : Parcelable
