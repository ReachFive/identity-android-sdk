package co.reachfive.identity.sdk.core

import android.app.Activity
import android.content.Intent
import android.util.Log
import co.reachfive.identity.sdk.core.RedirectionActivity.Companion.ABORT_RESULT_CODE
import co.reachfive.identity.sdk.core.RedirectionActivity.Companion.CODE_KEY
import co.reachfive.identity.sdk.core.RedirectionActivity.Companion.CODE_VERIFIER_KEY
import co.reachfive.identity.sdk.core.RedirectionActivity.Companion.NO_AUTH_ERROR_RESULT_CODE
import co.reachfive.identity.sdk.core.RedirectionActivity.Companion.URL_KEY
import co.reachfive.identity.sdk.core.api.ReachFiveApi
import co.reachfive.identity.sdk.core.api.ReachFiveApiCallback
import co.reachfive.identity.sdk.core.models.*
import co.reachfive.identity.sdk.core.models.requests.*
import co.reachfive.identity.sdk.core.models.requests.UpdatePasswordRequest.Companion.enrichWithClientId
import co.reachfive.identity.sdk.core.models.requests.UpdatePasswordRequest.Companion.getAccessToken
import co.reachfive.identity.sdk.core.models.requests.webAuthn.AuthenticationPublicKeyCredential
import co.reachfive.identity.sdk.core.models.requests.webAuthn.WebAuthnAuthentication.createAuthenticationPublicKeyCredential
import co.reachfive.identity.sdk.core.models.requests.webAuthn.WebAuthnLoginRequest
import co.reachfive.identity.sdk.core.models.requests.webAuthn.WebAuthnRegistrationRequest
import co.reachfive.identity.sdk.core.models.requests.webAuthn.WebauthnSignupCredential
import co.reachfive.identity.sdk.core.models.responses.ClientConfigResponse
import co.reachfive.identity.sdk.core.models.responses.webAuthn.DeviceCredential
import co.reachfive.identity.sdk.core.utils.*
import com.google.android.gms.fido.Fido
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAssertionResponse

class ReachFive(
    val activity: Activity,
    val sdkConfig: SdkConfig,
    val providersCreators: List<ProviderCreator>
) {

    companion object {
        private const val TAG = "Reach5"

        const val codeResponseType = "code"
    }

    private val reachFiveApi: ReachFiveApi = ReachFiveApi.create(sdkConfig)
    private val reachFiveWebAuthn = ReachFiveWebAuthn(activity)
    private val redirectionActivityLauncher = RedirectionActivityLauncher(sdkConfig, reachFiveApi)

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

    fun loginWithProvider(
        name: String,
        scope: Collection<String> = this.scope,
        origin: String,
        activity: Activity
    ) {
        getProvider(name)?.login(origin, scope, activity)
    }

    fun signup(
        profile: ProfileSignupRequest,
        scope: Collection<String> = this.scope,
        redirectUrl: String? = null,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>
    ) {
        val signupRequest = SignupRequest(
            clientId = sdkConfig.clientId,
            data = profile,
            redirectUrl = redirectUrl,
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
            "uid",
            "updated_at"
        )
        reachFiveApi
            .getProfile(
                formatAuthorization(authToken),
                SdkInfos.getQueries().plus(Pair("fields", fields.joinToString(",")))
            )
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
        val authCodeFlow = PkceAuthCodeFlow.readAuthCodeFlow(activity)
        return if (authCodeFlow != null) {
            val authCodeRequest = AuthCodeRequest(
                sdkConfig.clientId,
                authorizationCode,
                authCodeFlow.redirectUri,
                authCodeFlow.codeVerifier
            )
            reachFiveApi
                .authenticateWithCode(authCodeRequest, SdkInfos.getQueries())
                .enqueue(
                    ReachFiveApiCallback(
                        success = { it.toAuthToken().fold(success, failure) },
                        failure = failure
                    )
                )
        } else {
            failure(ReachFiveError.from("No PKCE challenge found in memory."))
        }
    }

    fun loginCallback(
        tkn: String,
        scope: Collection<String>
    ) {
        redirectionActivityLauncher.loginCallback(activity, scope, tkn)
    }

    fun loginWithWeb(
        scope: Collection<String> = this.scope,
        state: String? = null,
        nonce: String? = null,
        origin: String? = null,
    ) {
        Log.d("SDK CORE", "ENTER LOGIN WITH WEB")

        redirectionActivityLauncher.loginWithWeb(activity, scope, state, nonce, origin)
    }

    fun onLoginCallbackResult(
        intent: Intent,
        resultCode: Int,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>
    ) {
        when (resultCode) {
            RedirectionActivity.SUCCESS_RESULT_CODE -> {
                val code = intent.getStringExtra(CODE_KEY)!!
                val codeVerifier = intent.getStringExtra(CODE_VERIFIER_KEY)!!

                val authCodeRequest =
                    AuthCodeRequest(sdkConfig.clientId, code, sdkConfig.scheme, codeVerifier)

                reachFiveApi
                    .authenticateWithCode(authCodeRequest, SdkInfos.getQueries())
                    .enqueue(
                        ReachFiveApiCallback(
                            success = { it.toAuthToken().fold(success, failure) },
                            failure = failure
                        )
                    )
            }
            NO_AUTH_ERROR_RESULT_CODE -> {
                failure(ReachFiveError("No authorization code found in activity result."))
            }
            ABORT_RESULT_CODE -> {
                Log.d(TAG, "The custom tab has been closed.")
                Unit
            }
            else -> {
                Log.e(TAG, "Unexpected event.")
                Unit
            }
        }
    }

    fun startPasswordless(
        email: String? = null,
        phoneNumber: String? = null,
        redirectUrl: String = sdkConfig.scheme,
        successWithNoContent: SuccessWithNoContent<Unit>,
        failure: Failure<ReachFiveError>
    ) =
        PkceAuthCodeFlow.generate(redirectUrl).let { pkce ->
            PkceAuthCodeFlow.storeAuthCodeFlow(pkce, activity)
            reachFiveApi.requestPasswordlessStart(
                PasswordlessStartRequest(
                    clientId = sdkConfig.clientId,
                    email = email,
                    phoneNumber = phoneNumber,
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
        reachFiveApi.requestPasswordlessVerification(
            PasswordlessVerificationRequest(phoneNumber, verificationCode),
            SdkInfos.getQueries()
        ).enqueue(
            ReachFiveApiCallback(
                success = { verificationResponse ->
                    val authCodeFlow = PkceAuthCodeFlow.readAuthCodeFlow(activity)
                    if (authCodeFlow != null) {
                        val authCodeRequest = AuthCodeRequest(
                            sdkConfig.clientId,
                            verificationResponse.authCode,
                            redirectUri = authCodeFlow.redirectUri,
                            codeVerifier = authCodeFlow.codeVerifier
                        )

                        reachFiveApi
                            .authenticateWithCode(authCodeRequest, SdkInfos.getQueries())
                            .enqueue(
                                ReachFiveApiCallback(
                                    success = { tokenResponse -> tokenResponse.toAuthToken().fold(success, failure) },
                                    failure = failure
                                )
                            )
                    } else failure(ReachFiveError.from("No PKCE challenge found in memory."))

                },
                failure = failure
            )
        )

    fun signupWithWebAuthn(
        profile: ProfileWebAuthnSignupRequest,
        origin: String,
        friendlyName: String?,
        signupRequestCode: Int,
        successWithWebAuthnId: Success<String>,
        failure: Failure<ReachFiveError>
    ) {
        val newFriendlyName = formatFriendlyName(friendlyName)

        reachFiveApi
            .createWebAuthnSignupOptions(
                WebAuthnRegistrationRequest(origin, newFriendlyName, profile, sdkConfig.clientId),
                SdkInfos.getQueries()
            )
            .enqueue(
                ReachFiveApiCallback(
                    success = {
                        reachFiveWebAuthn.startFIDO2RegisterTask(it, signupRequestCode)
                        successWithWebAuthnId(it.options.publicKey.user.id)
                    },
                    failure = failure
                )
            )
    }

    fun onSignupWithWebAuthnResult(
        intent: Intent,
        webAuthnId: String,
        scope: Collection<String> = this.scope,
        failure: Failure<ReachFiveError>
    ) {
        if (intent.hasExtra(Fido.FIDO2_KEY_ERROR_EXTRA)) {
            failure(ReachFiveWebAuthn.extractFIDO2Error(intent))
        } else if (intent.hasExtra(Fido.FIDO2_KEY_RESPONSE_EXTRA)) {
            ReachFiveWebAuthn
                .extractRegistrationPublicKeyCredential(intent)
                ?.let { registrationPublicKeyCredential ->
                    reachFiveApi
                        .signupWithWebAuthn(
                            WebauthnSignupCredential(
                                webauthnId = webAuthnId,
                                publicKeyCredential = registrationPublicKeyCredential
                            )
                        )
                        .enqueue(
                            ReachFiveApiCallback(
                                success = { loginCallback(it.tkn, scope) },
                                failure = failure
                            )
                        )
                }
        }
    }

    fun addNewWebAuthnDevice(
        authToken: AuthToken,
        origin: String,
        friendlyName: String?,
        registerRequestCode: Int,
        failure: Failure<ReachFiveError>
    ) {
        val newFriendlyName = formatFriendlyName(friendlyName)

        reachFiveApi
            .createWebAuthnRegistrationOptions(
                formatAuthorization(authToken),
                WebAuthnRegistrationRequest(origin, newFriendlyName)
            )
            .enqueue(
                ReachFiveApiCallback(
                    success = { reachFiveWebAuthn.startFIDO2RegisterTask(it, registerRequestCode) },
                    failure = failure
                )
            )
    }

    fun onAddNewWebAuthnDeviceResult(
        authToken: AuthToken,
        intent: Intent,
        successWithNoContent: SuccessWithNoContent<Unit>,
        failure: Failure<ReachFiveError>
    ) {
        if (intent.hasExtra(Fido.FIDO2_KEY_ERROR_EXTRA)) {
            failure(ReachFiveWebAuthn.extractFIDO2Error(intent))
        } else if (intent.hasExtra(Fido.FIDO2_KEY_RESPONSE_EXTRA)) {
            ReachFiveWebAuthn
                .extractRegistrationPublicKeyCredential(intent)
                ?.let { registrationPublicKeyCredential ->
                    reachFiveApi
                        .registerWithWebAuthn(
                            formatAuthorization(authToken),
                            registrationPublicKeyCredential
                        )
                        .enqueue(
                            ReachFiveApiCallback(
                                successWithNoContent = successWithNoContent,
                                failure = failure
                            )
                        )
                }
        }
    }

    fun loginWithWebAuthn(
        loginRequest: WebAuthnLoginRequest,
        loginRequestCode: Int,
        failure: Failure<ReachFiveError>
    ) =
        reachFiveApi
            .createWebAuthnAuthenticationOptions(
                WebAuthnLoginRequest.enrichWithClientId(
                    loginRequest,
                    sdkConfig.clientId
                )
            )
            .enqueue(
                ReachFiveApiCallback(
                    success = {
                        val fido2ApiClient = Fido.getFido2ApiClient(activity)
                        val fido2PendingIntentTask =
                            fido2ApiClient.getSignPendingIntent(it.toFido2Model())
                        fido2PendingIntentTask.addOnSuccessListener { fido2PendingIntent ->
                            if (fido2PendingIntent != null) {
                                Log.d(TAG, "Launching Fido2 Pending Intent")
                                activity.startIntentSenderForResult(
                                    fido2PendingIntent.intentSender,
                                    loginRequestCode,
                                    null,
                                    0,
                                    0,
                                    0
                                )
                            }
                        }
                        fido2PendingIntentTask.addOnFailureListener {
                            throw ReachFiveError("FAILURE Launching Fido2 Pending Intent")
                        }
                    },
                    failure = failure
                )
            )

    fun onLoginWithWebAuthnResult(
        intent: Intent,
        scope: Collection<String> = this.scope,
        failure: Failure<ReachFiveError>
    ) {
        if (intent.hasExtra(Fido.FIDO2_KEY_ERROR_EXTRA)) {
            ReachFiveWebAuthn
                .extractFIDO2Error(intent)
                .let { failure(it) }
        } else if (intent.hasExtra(Fido.FIDO2_KEY_RESPONSE_EXTRA)) {
            val fido2Response = intent.getByteArrayExtra(Fido.FIDO2_KEY_RESPONSE_EXTRA)
            val authenticatorAssertionResponse: AuthenticatorAssertionResponse =
                AuthenticatorAssertionResponse.deserializeFromBytes(fido2Response)
            val authenticationPublicKeyCredential: AuthenticationPublicKeyCredential =
                createAuthenticationPublicKeyCredential(authenticatorAssertionResponse)

            return reachFiveApi
                .authenticateWithWebAuthn(authenticationPublicKeyCredential)
                .enqueue(
                    ReachFiveApiCallback(
                        success = { loginCallback(it.tkn, scope) },
                        failure = failure
                    )
                )
        }
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
            .deleteWebAuthnRegistration(
                formatAuthorization(authToken),
                deviceId,
                SdkInfos.getQueries()
            )
            .enqueue(
                ReachFiveApiCallback(
                    successWithNoContent = successWithNoContent,
                    failure = failure
                )
            )

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

    private fun formatFriendlyName(friendlyName: String?): String {
        return if (friendlyName.isNullOrEmpty()) android.os.Build.MODEL else friendlyName
    }
}
