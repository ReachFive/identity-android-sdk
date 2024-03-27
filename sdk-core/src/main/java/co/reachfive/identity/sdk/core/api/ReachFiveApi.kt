package co.reachfive.identity.sdk.core.api

import co.reachfive.identity.sdk.core.models.Profile
import co.reachfive.identity.sdk.core.models.ProvidersConfigsResult
import co.reachfive.identity.sdk.core.models.SdkConfig
import co.reachfive.identity.sdk.core.models.requests.*
import co.reachfive.identity.sdk.core.models.requests.webAuthn.*
import co.reachfive.identity.sdk.core.models.responses.AuthenticationToken
import co.reachfive.identity.sdk.core.models.responses.ClientConfigResponse
import co.reachfive.identity.sdk.core.models.responses.ListMfaCredentials
import co.reachfive.identity.sdk.core.models.responses.PasswordlessVerificationResponse
import co.reachfive.identity.sdk.core.models.responses.TokenEndpointResponse
import co.reachfive.identity.sdk.core.models.responses.webAuthn.AuthenticationOptions
import co.reachfive.identity.sdk.core.models.responses.webAuthn.DeviceCredential
import co.reachfive.identity.sdk.core.models.responses.webAuthn.RegistrationOptions
import com.google.gson.FieldNamingStrategy
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.lang.reflect.Field

interface ReachFiveApi {
    @GET("/identity/v1/config")
    fun clientConfig(@QueryMap options: Map<String, String>): Call<ClientConfigResponse>

    @GET("/api/v1/providers")
    fun providersConfigs(@QueryMap options: Map<String, String>): Call<ProvidersConfigsResult>

    @POST("/identity/v1/signup-token")
    fun signup(
        @Body signupRequest: SignupRequest,
        @QueryMap options: Map<String, String>
    ): Call<TokenEndpointResponse>

    @POST("/identity/v1/oauth/provider/token")
    fun loginWithProvider(
        @Body loginProviderRequest: LoginProviderRequest,
        @QueryMap options: Map<String, String>
    ): Call<TokenEndpointResponse>

    @POST("/identity/v1/password/login")
    fun loginWithPassword(
        @Body loginRequest: LoginRequest,
        @QueryMap options: Map<String, String>
    ): Call<AuthenticationToken>

    @POST("/oauth/token")
    fun authenticateWithCode(
        @Body authCodeRequest: AuthCodeRequest,
        @QueryMap options: Map<String, String>
    ): Call<TokenEndpointResponse>

    @POST("/oauth/token")
    fun refreshAccessToken(
        @Body authCodeRequest: RefreshRequest,
        @QueryMap options: Map<String, String>
    ): Call<TokenEndpointResponse>

    @GET("/oauth/authorize")
    fun authorize(@QueryMap options: Map<String, String>): Call<Unit>

    @GET("/identity/v1/logout")
    fun logout(@QueryMap options: Map<String, String>): Call<Unit>

    @GET("/identity/v1/userinfo")
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

    @POST("/identity/v1/passwordless/verify")
    fun requestPasswordlessVerification(
        @Body verificationRequest: PasswordlessVerificationRequest,
        @QueryMap options: Map<String, String>
    ): Call<PasswordlessVerificationResponse>

    @POST("/identity/v1/webauthn/signup-options")
    fun createWebAuthnSignupOptions(
        @Body webAuthnRegistrationRequest: WebAuthnRegistrationRequest,
        @QueryMap options: Map<String, String>
    ): Call<RegistrationOptions>

    @POST("/identity/v1/webauthn/signup")
    fun signupWithWebAuthn(
        @Body registrationPublicKeyCredential: WebauthnSignupCredential,
        @QueryMap options: Map<String, String>
    ): Call<AuthenticationToken>

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
        @Body webAuthnLoginRequest: WebAuthnLoginRequest,
        @QueryMap options: Map<String, String>
    ): Call<AuthenticationOptions>

    @POST("/identity/v1/webauthn/authentication")
    fun authenticateWithWebAuthn(
        @Body authenticationPublicKeyCredential: AuthenticationPublicKeyCredential,
        @QueryMap options: Map<String, String>
    ): Call<AuthenticationToken>

    @POST("/identity/v1/mfa/credentials/phone-numbers")
    fun startMfaPhoneNumberRegistration(
        @Header("Authorization") authorization: String,
        @Body startMfaPhoneNumberRegistration: MfaCredentialsStartPhoneRegisteringRequest
    ): Call<Unit>

    @POST("/identity/v1/mfa/credentials/emails")
    fun startMfaEmailRegistration(
        @Header("Authorization") authorization: String,
        @Body startEmailRegistration: MfaCredentialsStartEmailRegisteringRequest
    ): Call<Unit>

    @POST("/identity/v1/mfa/credentials/phone-numbers/verify")
    fun verifyMfaPhoneNumberRegistration(
        @Header("Authorization") authorization: String,
        @Body verifyPhoneNumberRequest: MfaCredentialsVerifyPhoneRegisteringRequest,
    ): Call<Unit>

    @POST("/identity/v1/mfa/credentials/emails/verify")
    fun verifyMfaEmailRegistration(
        @Header("Authorization") authorization: String,
        @Body verifyEmailRequest: VerifyEmailRequest
    ): Call<Unit>

    @GET("/identity/v1/mfa/credentials")
    fun listMfaCredentials(
        @Header("Authorization") authorization: String
    ): Call<ListMfaCredentials>

    companion object {
        fun create(config: SdkConfig): ReachFiveApi {
            val logging = HttpLoggingInterceptor()
            logging.apply { logging.level = HttpLoggingInterceptor.Level.BASIC }

            val client = OkHttpClient.Builder().addInterceptor(logging)
                .addNetworkInterceptor(AcceptLanguageInterceptor())
                .build()

            val gson = GsonBuilder()
                .registerTypeAdapter(
                    UpdatePasswordRequest::class.java,
                    UpdatePasswordRequestSerializer()
                )
                .registerTypeAdapter(
                    WebAuthnLoginRequest::class.java,
                    WebAuthnLoginRequestSerializer()
                ).setFieldNamingStrategy(object : FieldNamingStrategy {
                    // TODO/CA-3469 Better handling of ser/de.
                    override fun translateName(f: Field): String {
                        return when(f.name) {
                            "displayName" -> "display_name"
                            "clientDataJSON" -> "client_data_json"
                            "rawId" -> "raw_id"
                            "attestationObject" -> "attestation_object"
                            "rpId" -> "rp_id"
                            "allowCredentials" -> "allow_credentials"
                            "userVerification" -> "user_verification"
                            "authenticatorData" -> "authenticator_data"
                            "userHandle" -> "user_handle"
                            else -> f.name
                        }
                    }
                })
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
