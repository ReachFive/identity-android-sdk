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
    val errorId: String?,

    @SerializedName("error_user_msg")
    val errorUserMsg: String?,

    @SerializedName("error_message_key")
    val errorMessageKey: String?,

    @SerializedName("error_description")
    val errorDescription: String?,

    @SerializedName("error_details")
    val errorDetails: List<ReachFiveApiErrorDetail>?
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
                    errorDetails = null,
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
    val data: ReachFiveApiError? = null,
) : java.lang.Exception(message), Parcelable {

    fun getErrorCode(): ErrorCode? = code?.let { ErrorCode.valueOf(it.toString()) }

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
                    code = ErrorCode.OAuthAuthorizationError.code,
                    message = apiError.errorDescription ?: "ReachFive API response error",
                    data = apiError
                )
            }
        }

        @JvmStatic
        val NullIntent: ReachFiveError =
            ReachFiveError(
                code = ErrorCode.NullIntent.code,
                message = "Intent is null"
            )

        @JvmStatic
        val WebFlowCanceled = ReachFiveError(
            code = ErrorCode.WebFlowCanceled.code,
            message = "User canceled or closed the web flow.",
        )

        @JvmStatic
        val NoPkce = ReachFiveError(
            code = ErrorCode.NoPkce.code,
            message = "No PKCE code_verifier could be found when expected."
        )
    }
}
