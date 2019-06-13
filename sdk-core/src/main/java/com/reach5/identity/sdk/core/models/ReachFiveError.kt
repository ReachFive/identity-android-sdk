package com.reach5.identity.sdk.core.models

import com.google.gson.annotations.SerializedName

data class ReachFiveApiError(
    val error: String,
    @SerializedName("error_description")
    val errorDescription: String?,

    @SerializedName("error_details")
    val errorDetails: List<ReachFiveApiErrorDetail>?
)

data class ReachFiveApiErrorDetail(
    val field: String,
    val message: String
)

data class ReachFiveError(
    override val message: String,
    val exception: Exception? = null,
    val data: ReachFiveApiError? = null
): java.lang.Exception(message) {
    companion object {
        @JvmStatic
        fun from(error: Exception): ReachFiveError {
            return ReachFiveError(
                message = error.message ?: error.toString(),
                exception = error
            )
        }

        @JvmStatic
        fun from(message: String): ReachFiveError {
            return ReachFiveError(
                message = message
            )
        }
    }
}
