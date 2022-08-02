package co.reachfive.identity.sdk.core.models

import android.net.Uri
import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class ReachFiveApiError(
    val error: String,

    @SerializedName("error_id")
    val errorId: String? = null,

    @SerializedName("error_user_msg")
    val errorUserMsg: String? = null,

    @SerializedName("error_message_key")
    val errorMessageKey: String? = null,

    @SerializedName("error_description")
    val errorDescription: String? = null,

    @SerializedName("error_details")
    val errorDetails: List<ReachFiveApiErrorDetail>? = null
) : Parcelable {

    companion object {
        @JvmStatic
        fun resolveFrom(data: Uri): ReachFiveApiError? =
            data.getQueryParameter("error")?.let { error ->
                val errorId = data.getQueryParameter("error_id")
                val errorDescription = data.getQueryParameter("error_description")
                val errorMessageKey = data.getQueryParameter("error_message_key")
                val errorUserMsg = data.getQueryParameter("error_user_msg")

                ReachFiveApiError(
                    error = error,
                    errorId = errorId,
                    errorUserMsg = errorUserMsg,
                    errorMessageKey = errorMessageKey,
                    errorDescription = errorDescription,
                )
            }
    }
}

@Parcelize
data class ReachFiveApiErrorDetail(
    val field: String,
    val message: String
) : Parcelable

@Parcelize
data class ReachFiveError(
    override val message: String,
    val code: Int? = null,
    val exception: Exception? = null,
    val data: ReachFiveApiError? = null
) : java.lang.Exception(message), Parcelable {

    companion object {
        @JvmStatic
        fun from(error: Exception): ReachFiveError {
            return ReachFiveError(
                message = error.message ?: error.toString(),
                exception = error
            )
        }

        @JvmStatic
        fun from(error: Throwable): ReachFiveError {
            return ReachFiveError(
                message = error.message ?: error.toString()
            )
        }

        @JvmStatic
        fun from(message: String): ReachFiveError {
            return ReachFiveError(
                message = message
            )
        }

        @JvmStatic
        fun fromRedirectionResult(uri: Uri): ReachFiveError? {
            return ReachFiveApiError.resolveFrom(uri)?.let { apiError ->
                ReachFiveError(
                    code = 303,
                    message = apiError.errorDescription ?: "ReachFive API response error",
                    data = apiError
                )
            }
        }

        @JvmStatic
        val NoIntent: ReachFiveError =
            ReachFiveError(
                code = 52002,
                message = "Intent is null"
            )

        @JvmStatic
        val UserCanceled = ReachFiveError(
            code = 52001,
            message = "User canceled or closed the web flow.",
        )
    }
}
