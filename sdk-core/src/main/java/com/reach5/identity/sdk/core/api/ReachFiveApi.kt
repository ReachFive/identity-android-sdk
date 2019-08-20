package com.reach5.identity.sdk.core.api

import com.google.gson.GsonBuilder
import com.reach5.identity.sdk.core.SdkConfig
import com.reach5.identity.sdk.core.api.requests.*
import com.reach5.identity.sdk.core.api.responses.AuthTokenResponse
import com.reach5.identity.sdk.core.api.responses.ClientConfigResponse
import com.reach5.identity.sdk.core.api.responses.ProvidersConfigsResponse
import com.reach5.identity.sdk.core.models.Profile
import com.reach5.identity.sdk.core.models.UpdatePasswordRequest
import com.reach5.identity.sdk.core.models.UpdatePasswordRequestSerializer
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*


interface ReachFiveApi {
    @GET("/identity/v1/config")
    fun clientConfig(@QueryMap options: Map<String, String>): Call<ClientConfigResponse>

    @GET("/api/v1/providers")
    fun providersConfigs(@QueryMap options: Map<String, String>): Call<ProvidersConfigsResponse>

    @POST("/identity/v1/signup-token")
    fun signup(@Body signupRequest: SignupRequest, @QueryMap options: Map<String, String>): Call<AuthTokenResponse>

    @POST("/identity/v1/oauth/provider/token")
    fun loginWithProvider(@Body loginProviderRequest: LoginProviderRequest, @QueryMap options: Map<String, String>): Call<AuthTokenResponse>

    @POST("/oauth/token")
    fun loginWithPassword(@Body loginRequest: LoginRequest, @QueryMap options: Map<String, String>): Call<AuthTokenResponse>

    @POST("/oauth/token")
    fun authenticateWithCode(@Body authCodeRequest: AuthCodeRequest, @QueryMap options: Map<String, String>): Call<AuthTokenResponse>

    @POST("/oauth/token")
    fun refreshAccessToken(@Body authCodeRequest: RefreshRequest, @QueryMap options: Map<String, String>): Call<AuthTokenResponse>

    @GET("/identity/v1/logout")
    fun logout(@QueryMap options: Map<String, String>): Call<Unit>

    @GET("/identity/v1/me")
    fun getProfile(
        @Header("Authorization") authorization: String,
        @QueryMap options: Map<String, String>
    ): Call<Profile>

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
        @HeaderMap headers: Map<String, String>,
        @Body updatePhoneNumberRequest: UpdatePasswordRequest,
        @QueryMap options: Map<String, String>
    ): Call<Unit>

    @POST("/identity/v1/forgot-password")
    fun requestPasswordReset(
        @Body requestPasswordResetRequest: RequestPasswordResetRequest,
        @QueryMap options: Map<String, String>
    ): Call<Unit>

    companion object {
        fun create(config: SdkConfig): ReachFiveApi {
            val logging = HttpLoggingInterceptor()
            logging.apply { logging.level = HttpLoggingInterceptor.Level.BASIC }
            val client = OkHttpClient.Builder().addInterceptor(logging).build()

            val gson = GsonBuilder()
                .registerTypeAdapter(
                    UpdatePasswordRequest::class.java,
                    UpdatePasswordRequestSerializer()
                )
                .create()

            val retrofit = Retrofit.Builder()
                .baseUrl("https://${config.domain}")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(client)
                .build()

            return retrofit.create(ReachFiveApi::class.java)
        }
    }
}


