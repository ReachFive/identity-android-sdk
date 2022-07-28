package co.reachfive.identity.sdk.core.models

import android.net.Uri
import android.os.Parcelable
import com.google.gson.annotations.SerializedName
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
        // TODO from query string
        fun resolveFrom(data: Uri): ReachFiveApiError? {
            val error: String
            val errorMessageKey: String?
            val errorDescription: String

            return ReachFiveApiError(
                error = "",
                errorMessageKey = null,
                errorDescription = null,
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
        const val INTENT_EXTRA_KEY = "R5_ERROR"

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
            val apiError = ReachFiveApiError.resolveFrom(uri)
            return ReachFiveError(
                code = 303,
                message = apiError?.error ?: "ReachFive API response error",
                data = apiError
            )
        }

        val NoIntent: ReachFiveError = from("Intent is null")

        // TODO
        val Unexpected: ReachFiveError = from("Unexpected error")

        enum class Code(val code: Int) {
            /*
            API error codes
             */
            BadRequest(400),
            Unauthorized(401),
            Forbidden(403),
            NotFound(404),
            Conflict(409),
            TooManyRequests(429),
            InternalServerError(500),
            NotImplemented(501),
            BadGateway(502),
            ServiceUnavailable(503),
            GatewayTimeout(504),

            /*
            SDK error codes
             */
            // TODO
        }
    }
}
