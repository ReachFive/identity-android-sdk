package com.reach5.identity.sdk.core

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.util.Log
import com.google.android.gms.fido.Fido
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAssertionResponse
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAttestationResponse
import com.google.android.gms.fido.fido2.api.common.AuthenticatorErrorResponse
import com.reach5.identity.sdk.core.api.ReachFiveApi
import com.reach5.identity.sdk.core.api.ReachFiveApiCallback
import com.reach5.identity.sdk.core.models.*
import com.reach5.identity.sdk.core.models.requests.*
import com.reach5.identity.sdk.core.models.requests.UpdatePasswordRequest.Companion.enrichWithClientId
import com.reach5.identity.sdk.core.models.requests.UpdatePasswordRequest.Companion.getAccessToken
import com.reach5.identity.sdk.core.models.requests.webAuthn.AuthenticationPublicKeyCredential
import com.reach5.identity.sdk.core.models.requests.webAuthn.WebAuthnAuthentication.createAuthenticationPublicKeyCredential
import com.reach5.identity.sdk.core.models.requests.webAuthn.WebAuthnLoginRequest
import com.reach5.identity.sdk.core.models.requests.webAuthn.WebAuthnRegistration.createRegistrationPublicKeyCredential
import com.reach5.identity.sdk.core.models.requests.webAuthn.WebAuthnRegistrationRequest
import com.reach5.identity.sdk.core.models.responses.AuthToken
import com.reach5.identity.sdk.core.models.responses.ClientConfigResponse
import com.reach5.identity.sdk.core.models.responses.ReachFiveToken
import com.reach5.identity.sdk.core.models.responses.webAuthn.DeviceCredential
import com.reach5.identity.sdk.core.utils.*

class ReachFive (
    val activity: Activity,
    val sdkConfig: SdkConfig,
    val providersCreators: List<ProviderCreator>
) {

    companion object {
        private const val TAG = "Reach5"

        private const val codeResponseType = "code"
        private const val tokenResponseType = "token"
    }

    private val reachFiveApi: ReachFiveApi = ReachFiveApi.create(sdkConfig)

    private var scope: Set<String> = emptySet()

    private var providers: List<Provider> = emptyList()

    fun initialize(
        success: Success<List<Provider>> = {},
        failure: Failure<ReachFiveError> = {}
    ): ReachFive {
        reachFiveApi
            .clientConfig(mapOf("client_id" to sdkConfig.clientId))
            .enqueue(
                ReachFiveApiCallback<ClientConfigResponse>(
                    success = { clientConfig ->
                        scope = clientConfig.scope.split(" ").toSet()
                        providersConfigs(success, failure)
                    },
                    failure = failure
                )
            )

        return this
    }

    private fun providersConfigs(
        success: Success<List<Provider>>,
        failure: Failure<ReachFiveError>
    ) {
        reachFiveApi
            .providersConfigs(SdkInfos.getQueries())
            .enqueue(ReachFiveApiCallback<ProvidersConfigsResult>({
                providers = createProviders(it)
                success(providers)
            }, failure = failure))
    }

    private fun createProviders(providersConfigsResult: ProvidersConfigsResult): List<Provider> {
        val webViewProvider = providersCreators.find { it.name == "webview" }
        return providersConfigsResult.items?.mapNotNull { config ->
            val nativeProvider = providersCreators.find { it.name == config.provider }
            when {
                nativeProvider != null -> nativeProvider.create(
                    config,
                    sdkConfig,
                    reachFiveApi,
                    activity
                )
                webViewProvider != null -> webViewProvider.create(
                    config,
                    sdkConfig,
                    reachFiveApi,
                    activity
                )
                else -> {
                    Log.w(
                        TAG,
                        "Non supported provider found, please add webview or native provider"
                    )
                    null
                }
            }
        } ?: listOf()
    }

    fun getProvider(name: String): Provider? {
        return providers.find { p -> p.name == name }
    }

    fun getProviders(): List<Provider> {
        return providers
    }

    fun loginWithProvider(name: String, scope: Collection<String> = emptySet(), origin: String, activity: Activity) {
        getProvider(name)?.login(origin, scope, activity)
    }

    fun signup(
        profile: ProfileSignupRequest,
        scope: Collection<String> = this.scope,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>
    ) {
        val signupRequest = SignupRequest(
            clientId = sdkConfig.clientId,
            data = profile,
            scope = formatScope(scope)
        )
        reachFiveApi
            .signup(signupRequest, SdkInfos.getQueries())
            .enqueue(
                ReachFiveApiCallback(
                    success = { it.toAuthToken().fold(success, failure) },
                    failure = failure
                )
            )
    }

    /**
     * @param username You can use email or phone number
     */
    fun loginWithPassword(
        username: String,
        password: String,
        scope: Collection<String> = this.scope,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>
    ) {
        val loginRequest = LoginRequest(
            clientId = sdkConfig.clientId,
            grantType = "password",
            username = username,
            password = password,
            scope = formatScope(scope)
        )
        reachFiveApi
            .loginWithPassword(loginRequest, SdkInfos.getQueries())
            .enqueue(
                ReachFiveApiCallback(
                    success = { it.toAuthToken().fold(success, failure) },
                    failure = failure
                )
            )
    }

    fun logout(
        successWithNoContent: SuccessWithNoContent<Unit>,
        failure: Failure<ReachFiveError>
    ) {
        providers.forEach { it.logout() }

        reachFiveApi
            .logout(SdkInfos.getQueries())
            .enqueue(
                ReachFiveApiCallback(
                    successWithNoContent = successWithNoContent,
                    failure = failure
                )
            )
    }

    fun refreshAccessToken(
        authToken: AuthToken,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>
    ) {
        val refreshRequest = RefreshRequest(
            clientId = sdkConfig.clientId,
            refreshToken = authToken.refreshToken ?: "",
            redirectUri = sdkConfig.scheme
        )

        reachFiveApi
            .refreshAccessToken(refreshRequest, SdkInfos.getQueries())
            .enqueue(
                ReachFiveApiCallback(
                    success = { it.toAuthToken().fold(success, failure) },
                    failure = failure
                )
            )
    }

    fun getProfile(
        authToken: AuthToken,
        success: Success<Profile>,
        failure: Failure<ReachFiveError>
    ) {
        val fields = arrayOf(
            "addresses",
            "auth_types",
            "bio",
            "birthdate",
            "company",
            "consents",
            "created_at",
            "custom_fields",
            "devices",
            "email",
            "emails",
            "email_verified",
            "external_id",
            "family_name",
            "first_login",
            "first_name",
            "full_name",
            "gender",
            "given_name",
            "has_managed_profile",
            "has_password",
            "id",
            "identities",
            "last_login",
            "last_login_provider",
            "last_login_type",
            "last_name",
            "likes_friends_ratio",
            "lite_only",
            "locale",
            "local_friends_count",
            "login_summary",
            "logins_count",
            "middle_name",
            "name",
            "nickname",
            "origins",
            "phone_number",
            "phone_number_verified",
            "picture",
            "provider_details",
            "providers",
            "social_identities",
            "sub",
            "tos_accepted_at",
            "uid",
            "updated_at"
        )
        reachFiveApi
            .getProfile(formatAuthorization(authToken), SdkInfos.getQueries().plus(Pair("fields", fields.joinToString(","))))
            .enqueue(ReachFiveApiCallback(success = success, failure = failure))
    }

    fun verifyPhoneNumber(
        authToken: AuthToken,
        phoneNumber: String,
        verificationCode: String,
        successWithNoContent: SuccessWithNoContent<Unit>,
        failure: Failure<ReachFiveError>
    ) {
        reachFiveApi
            .verifyPhoneNumber(
                formatAuthorization(authToken),
                VerifyPhoneNumberRequest(phoneNumber, verificationCode),
                SdkInfos.getQueries()
            )
            .enqueue(
                ReachFiveApiCallback(
                    successWithNoContent = successWithNoContent,
                    failure = failure
                )
            )
    }

    fun updateEmail(
        authToken: AuthToken,
        email: String,
        redirectUrl: String? = null,
        success: Success<Profile>,
        failure: Failure<ReachFiveError>
    ) {
        reachFiveApi
            .updateEmail(
                formatAuthorization(authToken),
                UpdateEmailRequest(email, redirectUrl), SdkInfos.getQueries()
            )
            .enqueue(ReachFiveApiCallback(success = success, failure = failure))
    }

    fun updatePhoneNumber(
        authToken: AuthToken,
        phoneNumber: String,
        success: Success<Profile>,
        failure: Failure<ReachFiveError>
    ) {
        reachFiveApi
            .updatePhoneNumber(
                formatAuthorization(authToken),
                UpdatePhoneNumberRequest(phoneNumber),
                SdkInfos.getQueries()
            )
            .enqueue(ReachFiveApiCallback(success = success, failure = failure))
    }

    fun updateProfile(
        authToken: AuthToken,
        profile: Profile,
        success: Success<Profile>,
        failure: Failure<ReachFiveError>
    ) {
        reachFiveApi
            .updateProfile(formatAuthorization(authToken), profile, SdkInfos.getQueries())
            .enqueue(ReachFiveApiCallback(success = success, failure = failure))
    }

    fun updatePassword(
        updatePasswordRequest: UpdatePasswordRequest,
        successWithNoContent: SuccessWithNoContent<Unit>,
        failure: Failure<ReachFiveError>
    ) {
        val headers = getAccessToken(updatePasswordRequest)
            ?.let { mapOf("Authorization" to formatAuthorization(it)) }
            ?: emptyMap()

        reachFiveApi
            .updatePassword(
                headers,
                enrichWithClientId(updatePasswordRequest, sdkConfig.clientId),
                SdkInfos.getQueries()
            )
            .enqueue(
                ReachFiveApiCallback(
                    successWithNoContent = successWithNoContent,
                    failure = failure
                )
            )
    }

    fun requestPasswordReset(
        email: String? = null,
        redirectUrl: String? = null,
        phoneNumber: String? = null,
        successWithNoContent: SuccessWithNoContent<Unit>,
        failure: Failure<ReachFiveError>
    ) {
        reachFiveApi
            .requestPasswordReset(
                RequestPasswordResetRequest(
                    sdkConfig.clientId,
                    email,
                    redirectUrl,
                    phoneNumber
                ),
                SdkInfos.getQueries()
            )
            .enqueue(
                ReachFiveApiCallback(
                    successWithNoContent = successWithNoContent,
                    failure = failure
                )
            )
    }

    fun exchangeCodeForToken(
        authorizationCode: String,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>
    ) {
        val codeVerifier = Pkce.readCodeVerifier(activity)
        return if (codeVerifier != null) {
            val authCodeRequest = AuthCodeRequest(
                sdkConfig.clientId,
                authorizationCode,
                sdkConfig.scheme,
                codeVerifier
            )
            reachFiveApi
                .authenticateWithCode(authCodeRequest, SdkInfos.getQueries())
                .enqueue(ReachFiveApiCallback(success = { it.toAuthToken().fold(success, failure) }, failure = failure))
        } else {
            failure(ReachFiveError.from("Empty PKCE or Authorization Code"))
        }
    }

    fun startPasswordless(
        email: String? = null,
        phoneNumber: String? = null,
        redirectUrl: String = sdkConfig.scheme,
        successWithNoContent: SuccessWithNoContent<Unit>,
        failure: Failure<ReachFiveError>
    ) =
        Pkce.generate().let { pkce ->
            Pkce.storeCodeVerifier(pkce, activity)
            reachFiveApi.requestPasswordlessStart(
                PasswordlessStartRequest(
                    clientId = sdkConfig.clientId,
                    email = email,
                    phoneNumber = phoneNumber,
                    authType = if (email != null) PasswordlessAuthType.MAGIC_LINK else PasswordlessAuthType.SMS,
                    codeChallenge = pkce.codeChallenge,
                    codeChallengeMethod = pkce.codeChallengeMethod,
                    responseType = codeResponseType,
                    redirectUri = redirectUrl
                ),
                SdkInfos.getQueries()
            ).enqueue(
                ReachFiveApiCallback(
                    successWithNoContent = successWithNoContent,
                    failure = failure
                )
            )
        }

    fun verifyPasswordless(
        phoneNumber: String,
        verificationCode: String,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>
    ) =
        reachFiveApi.requestPasswordlessCodeVerification(
            PasswordlessCodeVerificationRequest(
                sdkConfig.clientId,
                phoneNumber,
                verificationCode,
                PasswordlessAuthType.SMS
            ),
            SdkInfos.getQueries()
        ).enqueue(
            ReachFiveApiCallback(
                successWithNoContent = {
                    reachFiveApi.requestPasswordlessVerification(
                        PasswordlessAuthorizationCodeRequest(
                            clientId = sdkConfig.clientId,
                            phoneNumber = phoneNumber,
                            verificationCode = verificationCode,
                            codeVerifier = Pkce.readCodeVerifier(activity).orEmpty(),
                            responseType = tokenResponseType
                        ),
                        SdkInfos.getQueries()
                    ).enqueue(
                        ReachFiveApiCallback(
                            success = { it.toAuthToken().fold(success, failure) },
                            failure = failure
                        )
                    )
                },
                failure = failure
            )
        )

    fun addNewWebAuthnDevice(
        authToken: AuthToken,
        origin: String,
        friendlyName: String?,
        registerRequestCode: Int,
        failure: Failure<ReachFiveError>
    ) {
        val newFriendlyName = if (friendlyName.isNullOrEmpty()) android.os.Build.MODEL else friendlyName

        reachFiveApi
            .createWebAuthnRegistrationOptions(
                formatAuthorization(authToken),
                WebAuthnRegistrationRequest(origin, newFriendlyName)
            )
            .enqueue(ReachFiveApiCallback(
                success = {
                    val fido2ApiClient = Fido.getFido2ApiClient(activity)
                    val fido2PendingIntentTask = fido2ApiClient.getRegisterPendingIntent(it.toFido2Model())
                    fido2PendingIntentTask.addOnSuccessListener { fido2PendingIntent ->
                        if (fido2PendingIntent != null) {
                            Log.d(TAG, "Launching Fido2 Pending Intent")
                            activity.startIntentSenderForResult(fido2PendingIntent.intentSender, registerRequestCode, null, 0, 0, 0)
                        }
                    }
                  fido2PendingIntentTask.addOnFailureListener {
                      throw ReachFiveError("FAILURE Launching Fido2 Pending Intent")
                  }
                },
                failure = failure
            ))
    }

    fun onAddNewWebAuthnDeviceResult(
        authToken: AuthToken,
        intent: Intent,
        successWithNoContent: SuccessWithNoContent<Unit>,
        failure: Failure<ReachFiveError>
    ) {
        if (intent.hasExtra(Fido.FIDO2_KEY_ERROR_EXTRA)) {
            val errorBytes = intent.getByteArrayExtra(Fido.FIDO2_KEY_ERROR_EXTRA)
            val authenticatorErrorResponse = AuthenticatorErrorResponse.deserializeFromBytes(errorBytes)
            val reachFiveError = ReachFiveError(
                message = authenticatorErrorResponse.errorMessage ?: "Unexpected error during FIDO2 registration",
                code = authenticatorErrorResponse.errorCodeAsInt
            )

            failure(reachFiveError)
        }
        else if (intent.hasExtra(Fido.FIDO2_KEY_RESPONSE_EXTRA)) {
            val fido2Response = intent.getByteArrayExtra(Fido.FIDO2_KEY_RESPONSE_EXTRA)
            val authenticatorAttestationResponse = AuthenticatorAttestationResponse.deserializeFromBytes(fido2Response)
            val registrationPublicKeyCredential = createRegistrationPublicKeyCredential(authenticatorAttestationResponse)

            return reachFiveApi
                .registerWithWebAuthn(formatAuthorization(authToken), registrationPublicKeyCredential)
                .enqueue(ReachFiveApiCallback(
                    successWithNoContent = successWithNoContent,
                    failure = failure
                ))
        }
    }

    fun loginWithWebAuthn(
        loginRequest: WebAuthnLoginRequest,
        loginRequestCode: Int,
        failure: Failure<ReachFiveError>
    ) =
        reachFiveApi
            .createWebAuthnAuthenticationOptions(WebAuthnLoginRequest.enrichWithClientId(loginRequest, sdkConfig.clientId))
            .enqueue(ReachFiveApiCallback(
                success = {
                    val fido2ApiClient = Fido.getFido2ApiClient(activity)
                    val fido2PendingIntentTask = fido2ApiClient.getSignPendingIntent(it.toFido2Model())
                    fido2PendingIntentTask.addOnSuccessListener { fido2PendingIntent ->
                        if (fido2PendingIntent != null) {
                            Log.d(TAG, "Launching Fido2 Pending Intent")
                            activity.startIntentSenderForResult(fido2PendingIntent.intentSender, loginRequestCode, null, 0, 0, 0)
                        }
                    }
                    fido2PendingIntentTask.addOnFailureListener {
                        throw ReachFiveError("FAILURE Launching Fido2 Pending Intent")
                    }
                },
                failure = failure
            ))

    fun onLoginWithWebAuthnResult(
        fido2Response: ByteArray,
        success: Success<ReachFiveToken>,
        failure: Failure<ReachFiveError>
    ) {
        val authenticatorAssertionResponse: AuthenticatorAssertionResponse = AuthenticatorAssertionResponse.deserializeFromBytes(fido2Response)
        val authenticationPublicKeyCredential: AuthenticationPublicKeyCredential = createAuthenticationPublicKeyCredential(authenticatorAssertionResponse)

        return reachFiveApi
            .authenticateWithWebAuthn(authenticationPublicKeyCredential)
            .enqueue(ReachFiveApiCallback(success = success, failure = failure))
    }

    fun listWebAuthnDevices(
        authToken: AuthToken,
        success: Success<List<DeviceCredential>>,
        failure: Failure<ReachFiveError>
    ) =
        reachFiveApi
            .getWebAuthnRegistrations(formatAuthorization(authToken), SdkInfos.getQueries())
            .enqueue(ReachFiveApiCallback(success = success, failure = failure))

    fun removeWebAuthnDevice(
        authToken: AuthToken,
        deviceId: String,
        successWithNoContent: SuccessWithNoContent<Unit>,
        failure: Failure<ReachFiveError>
    ) =
        reachFiveApi
            .deleteWebAuthnRegistration(formatAuthorization(authToken), deviceId, SdkInfos.getQueries())
            .enqueue(ReachFiveApiCallback(successWithNoContent = successWithNoContent, failure = failure))

    fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>
    ) {
        val provider = providers.find { p -> p.requestCode == requestCode }
        if (provider != null) {
            provider.onActivityResult(requestCode, resultCode, data, success, failure)
        } else {
            failure(ReachFiveError.from("No provider found for this requestCode: $requestCode"))
        }
    }

    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
        failure: Failure<ReachFiveError>
    ) {
        val provider = providers.find { p -> p.requestCode == requestCode }
        provider?.onRequestPermissionsResult(requestCode, permissions, grantResults, failure)
    }

    fun onStop() {
        providers.forEach { it.onStop() }
    }

    private fun formatAuthorization(authToken: AuthToken): String {
        return "${authToken.tokenType} ${authToken.accessToken}"
    }
}
