package co.reachfive.identity.sdk.core.api

import co.reachfive.identity.sdk.core.models.ReachFiveError
import co.reachfive.identity.sdk.core.utils.Failure
import co.reachfive.identity.sdk.core.utils.Success
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

sealed interface ReachFiveApiCallback<T> : Callback<T> {
    val failure: Failure<ReachFiveError>

    override fun onFailure(call: Call<T>, t: Throwable) {
        failure(ReachFiveError.from(t))
    }

    override fun onResponse(call: Call<T>, response: Response<T>)

    companion object {

        fun noContent(
            success: Success<Unit>,
            failure: Failure<ReachFiveError>
        ) = UnitApiCallback(success, failure)

        fun <T> withContent(
            success: Success<T>,
            failure: Failure<ReachFiveError>
        ) = ValueApiCallback<T>(success, failure)
    }
}

class UnitApiCallback(
    val success: Success<Unit>,
    override val failure: Failure<ReachFiveError>
) : ReachFiveApiCallback<Unit> {

    override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
        if (response.isSuccessful) {
            success(Unit)
        } else {
            val reachFiveError = ReachFiveError.fromHttpResponse(response)
            failure(reachFiveError)
        }
    }
}

class ValueApiCallback<T>(
    val success: Success<T> = {},
    override val failure: Failure<ReachFiveError>
) : ReachFiveApiCallback<T> {
    override fun onFailure(call: Call<T>, t: Throwable) {
        failure(ReachFiveError.from(t))
    }

    override fun onResponse(call: Call<T>, response: Response<T>) {
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) success(body)
            else failure(ReachFiveError.from(NullPointerException("Expected response body.")))
        } else {
            val reachFiveError = ReachFiveError.fromHttpResponse(response)
            failure(reachFiveError)
        }
    }
}
