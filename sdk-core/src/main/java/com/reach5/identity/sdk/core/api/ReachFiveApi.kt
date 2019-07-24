package com.reach5.identity.sdk.core.api

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.reach5.identity.sdk.core.models.*
import com.reach5.identity.sdk.core.utils.Failure
import com.reach5.identity.sdk.core.utils.Success
import com.reach5.identity.sdk.core.utils.SuccessWithNoContent
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*


interface ReachFiveApi {
    @GET("/identity/v1/config")
    fun clientConfig(@QueryMap options: Map<String, String>): Call<ClientConfigResponse>

    @GET("/api/v1/providers")
    fun providersConfigs(@QueryMap options: Map<String, String>): Call<ProvidersConfigsResult>

    @POST("/identity/v1/signup-token")
    fun signup(@Body signupRequest: SignupRequest, @QueryMap options: Map<String, String>): Call<AuthTokenResponse>

    @POST("/identity/v1/oauth/provider/token")
    fun loginWithProvider(@Body loginProviderRequest: LoginProviderRequest, @QueryMap options: Map<String, String>): Call<AuthTokenResponse>

    @POST("/oauth/token")
    fun loginWithPassword(@Body loginRequest: LoginRequest, @QueryMap options: Map<String, String>): Call<AuthTokenResponse>

    @POST("/oauth/token")
    fun authenticateWithCode(@Body authCodeRequest: AuthCodeRequest, @QueryMap options: Map<String, String>): Call<AuthTokenResponse>

    @GET("/identity/v1/logout")
    fun logout(@QueryMap options: Map<String, String>): Call<Unit>

    @POST("/identity/v1/verify-phone-number")
    fun verifyPhoneNumber(
        @Header("Authorization") authorization: String,
        @Body verifyPhoneNumberRequest: VerifyPhoneNumberRequest,
        @QueryMap options: Map<String, String>
    ): Call<Unit>

    @POST("/identity/v1/update-email")
    fun updateEmail(
        @Header("Authorization") authorization: String,
        @Body updateEmailRequest: UpdateEmailRequest,
        @QueryMap options: Map<String, String>
    ): Call<Profile>

    @POST("/identity/v1/update-phone-number")
    fun updatePhoneNumber(
        @Header("Authorization") authorization: String,
        @Body updatePhoneNumberRequest: UpdatePhoneNumberRequest,
        @QueryMap options: Map<String, String>
    ): Call<Profile>

    @POST("/identity/v1/update-profile")
    fun updateProfile(
        @Header("Authorization") authorization: String,
        @Body updatedProfile: Profile,
        @QueryMap options: Map<String, String>
    ): Call<Profile>

    @POST("/identity/v1/update-password")
    fun updatePassword(
        @Header("Authorization") authorization: String,
        @Body updatePhoneNumberRequest: UpdatePasswordRequest,
        @QueryMap options: Map<String, String>
    ): Call<Unit>

    @POST("/identity/v1/forgot-password")
    fun requestPasswordReset(
        @Header("Authorization") authorization: String,
        @Body requestPasswordResetRequest: RequestPasswordResetRequest,
        @QueryMap options: Map<String, String>
    ): Call<Unit>

    companion object {
        fun create(config: SdkConfig): ReachFiveApi {
            val gson = GsonBuilder()
                .registerTypeAdapter(UpdatePasswordRequest::class.java, UpdatePasswordRequestSerializer())
                .create()

            val retrofit = Retrofit.Builder()
                .baseUrl("https://${config.domain}")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()

            return retrofit.create(ReachFiveApi::class.java)
        }
    }
}

class ReachFiveApiCallback<T>(
    val success: Success<T> = { Unit },
    val successWithNoContent: SuccessWithNoContent<Unit> = { Unit },
    val failure: Failure<ReachFiveError>
) : Callback<T> {
    override fun onFailure(call: Call<T>, t: Throwable) {
        failure(ReachFiveError.from(t))
    }

    override fun onResponse(call: Call<T>, response: Response<T>) {
        val status = response.code()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) success(body)
            else successWithNoContent(Unit)
        } else if (status in 300..400) {
            failure(ReachFiveError(
                message = "Bad Request",
                data = tryOrNull { parseErrorBody(response) }
            ))
        } else if (status in 400..600) {
            failure(ReachFiveError(
                message = "Technical Error",
                data = tryOrNull { parseErrorBody(response) }
            ))
        }
    }

    private fun <T> tryOrNull(callback: () -> T): T? {
        return try {
            callback()
        } catch (e: Exception) {
            null
        }
    }

    private fun <T> parseErrorBody(response: Response<T>): ReachFiveApiError {
        return Gson().fromJson(response.errorBody()?.string(), ReachFiveApiError::class.java)
    }
}
