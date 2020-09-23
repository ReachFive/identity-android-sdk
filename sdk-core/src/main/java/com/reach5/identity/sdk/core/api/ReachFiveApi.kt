package com.reach5.identity.sdk.core.api

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.reach5.identity.sdk.core.models.*
import com.reach5.identity.sdk.core.models.requests.*
import com.reach5.identity.sdk.core.models.requests.webAuthn.*
import com.reach5.identity.sdk.core.models.requests.webAuthn.WebAuthnLoginRequestSerializer
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import com.reach5.identity.sdk.core.models.responses.AuthTokenResponse
import com.reach5.identity.sdk.core.models.responses.ClientConfigResponse
import com.reach5.identity.sdk.core.models.responses.AuthenticationToken
import com.reach5.identity.sdk.core.models.responses.webAuthn.DeviceCredential
import com.reach5.identity.sdk.core.models.responses.webAuthn.AuthenticationOptions
import com.reach5.identity.sdk.core.models.responses.webAuthn.RegistrationOptions
import com.reach5.identity.sdk.core.utils.*


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

    @POST("/oauth/token")
    fun refreshAccessToken(@Body authCodeRequest: RefreshRequest, @QueryMap options: Map<String, String>): Call<AuthTokenResponse>

    @GET("/oauth/authorize")
    fun authorize(@QueryMap options: Map<String, String>): Call<Unit>

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

    @POST("/identity/v1/passwordless/start")
    fun requestPasswordlessStart(
        @Body passwordlessStartRequest: PasswordlessStartRequest,
        @QueryMap options: Map<String, String>
    ): Call<Unit>

    @POST("/identity/v1/verify-auth-code")
    fun requestPasswordlessCodeVerification(
        @Body verificationCodeRequest: PasswordlessCodeVerificationRequest,
        @QueryMap options: Map<String, String>
    ): Call<Unit>

    @POST("/identity/v1/passwordless/verify")
    fun requestPasswordlessVerification(
        @Body passwordlessAuthorizationCodeRequest: PasswordlessAuthorizationCodeRequest,
        @QueryMap options: Map<String, String>
    ): Call<AuthTokenResponse>

    @POST("identity/v1/webauthn/signup-options")
    fun createWebAuthnSignupOptions(
        @Body webAuthnRegistrationRequest: WebAuthnRegistrationRequest,
        @QueryMap options: Map<String, String>
    ): Call<RegistrationOptions>

    @POST("/identity/v1/webauthn/signup")
    fun signupWithWebAuthn(@Body registrationPublicKeyCredential: WebauthnSignupCredential): Call<AuthenticationToken>

    @POST("/identity/v1/webauthn/registration-options")
    fun createWebAuthnRegistrationOptions(
        @Header("Authorization") authorization: String,
        @Body webAuthnRegistrationRequest: WebAuthnRegistrationRequest
    ): Call<RegistrationOptions>

    @POST("/identity/v1/webauthn/registration")
    fun registerWithWebAuthn(
        @Header("Authorization") authorization: String,
        @Body registrationPublicKeyCredential: RegistrationPublicKeyCredential
    ): Call<Unit>

    @GET("/identity/v1/webauthn/registration")
    fun getWebAuthnRegistrations(
        @Header("Authorization") authorization: String,
        @QueryMap options: Map<String, String>
    ): Call<List<DeviceCredential>>

    @DELETE("/identity/v1/webauthn/registration/{id}")
    fun deleteWebAuthnRegistration(
        @Header("Authorization") authorization: String,
        @Path("id") id: String,
        @QueryMap options: Map<String, String>
    ): Call<Unit>

    @POST("/identity/v1/webauthn/authentication-options")
    fun createWebAuthnAuthenticationOptions(
        @Body webAuthnLoginRequest: WebAuthnLoginRequest
    ): Call<AuthenticationOptions>

    @POST("/identity/v1/webauthn/authentication")
    fun authenticateWithWebAuthn(
        @Body authenticationPublicKeyCredential: AuthenticationPublicKeyCredential
    ): Call<AuthenticationToken>

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
                .registerTypeAdapter(
                    WebAuthnLoginRequest::class.java,
                    WebAuthnLoginRequestSerializer()
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

class ReachFiveApiCallback<T>(
    val success: Success<T> = { Unit },
    val successWithNoContent: SuccessWithNoContent<Unit> = { Unit },
    val failure: Failure<ReachFiveError>
) : Callback<T> {
    override fun onFailure(call: Call<T>, t: Throwable) {
        failure(ReachFiveError.from(t))
    }

    override fun onResponse(call: Call<T>, response: Response<T>) {
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) success(body)
            else successWithNoContent(Unit)
        } else {
            val data = tryOrNull { parseErrorBody(response) }
            failure(
                ReachFiveError(
                    message = data?.error ?: "ReachFive API response error",
                    code = response.code(),
                    data = data
                )
            )
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
