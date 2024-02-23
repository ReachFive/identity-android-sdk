package co.reachfive.identity.sdk.core

import android.content.Context
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.credentials.*
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialException
import co.reachfive.identity.sdk.core.api.ReachFiveApi
import co.reachfive.identity.sdk.core.api.ReachFiveApiCallback
import co.reachfive.identity.sdk.core.models.AuthToken
import co.reachfive.identity.sdk.core.models.ReachFiveError
import co.reachfive.identity.sdk.core.models.SdkConfig
import co.reachfive.identity.sdk.core.models.SdkInfos
import co.reachfive.identity.sdk.core.models.requests.ProfileWebAuthnSignupRequest
import co.reachfive.identity.sdk.core.models.requests.webAuthn.*
import co.reachfive.identity.sdk.core.models.responses.AuthenticationToken
import co.reachfive.identity.sdk.core.models.responses.webAuthn.AuthenticationOptions
import co.reachfive.identity.sdk.core.models.responses.webAuthn.R5AuthenticatorSelectionCriteria
import co.reachfive.identity.sdk.core.models.responses.webAuthn.R5PublicKeyCredentialCreationOptions
import co.reachfive.identity.sdk.core.models.responses.webAuthn.RegistrationOptions
import co.reachfive.identity.sdk.core.utils.Failure
import co.reachfive.identity.sdk.core.utils.Success
import co.reachfive.identity.sdk.core.utils.formatScope
import com.google.gson.Gson
import com.google.gson.GsonBuilder


internal class CredentialManagerAuthClient(
    private val reachFiveApi: ReachFiveApi,
    private val sdkConfig: SdkConfig,
    private val passwordAuth: PasswordAuthClient,
    private val sessionUtils: SessionUtilsClient,
    private val credentialManager: CredentialManager?,
) : CredentialManagerAuth {
    private var authToken: AuthToken? = null

    override var defaultScope: Set<String> = emptySet()

    private companion object {

        fun formatFriendlyName(friendlyName: String?): String {
            return if (friendlyName.isNullOrEmpty()) android.os.Build.MODEL else friendlyName
        }
    }

    override fun registerNewPasskey(
        authToken: AuthToken,
        friendlyName: String?,
        success: Success<Unit>,
        failure: Failure<ReachFiveError>,
        context: Context
    ) {
        if (credentialManager == null || sdkConfig.originWebAuthn == null)
            failure(ReachFiveError("Credential Manager or origin is null"))

        this.authToken = authToken

        reachFiveApi
            .createWebAuthnRegistrationOptions(
                authToken.authHeader,
                WebAuthnRegistrationRequest(sdkConfig.originWebAuthn!!, formatFriendlyName(friendlyName))
            )
            .enqueue(
                ReachFiveApiCallback.withContent<RegistrationOptions>(
                    success = { registrationOptions ->
                        handlePasskeyRegistration(
                            registrationOptions,
                            success,
                            failure,
                            context
                        )
                    },
                    failure = failure
                )
            )
    }

    private fun handlePasskeyRegistration(
        registrationOptions: RegistrationOptions,
        success: Success<Unit>,
        failure: Failure<ReachFiveError>,
        context: Context
    ) {
        val authToken = this.authToken ?: return failure(ReachFiveError.from("No auth token!"))

        val register =
            { registrationPublicKeyCredential: RegistrationPublicKeyCredential,
              success: Success<Unit>,
              failure: Failure<ReachFiveError> ->
                reachFiveApi
                    .registerWithWebAuthn(
                        authToken.authHeader,
                        registrationPublicKeyCredential
                    )
                    .enqueue(ReachFiveApiCallback.noContent(success, failure))
            }


        handleNewPasskey(
            registrationOptions.options.publicKey,
            context,
            success,
            failure,
            register
        )

    }


    private fun <T> handleNewPasskey(
        publicKeyCredentialCreationOptions: R5PublicKeyCredentialCreationOptions,
        context: Context,
        success: Success<T>,
        failure: Failure<ReachFiveError>,
        f: (RegistrationPublicKeyCredential, Success<T>, Failure<ReachFiveError>) -> Unit,
    ) {
        // FIXME The `authenticatorSelection` claim is not marked as required in WebAuthn spec,
        //  but passkey creation with Google Password Manager fails when it is missing or empty.
        val authenticatorSelectionFiller =
            if (publicKeyCredentialCreationOptions.authenticatorSelection == null)
                publicKeyCredentialCreationOptions.copy(
                    authenticatorSelection = R5AuthenticatorSelectionCriteria(
                        authenticatorAttachment = "platform",
                        residentKey = "required",
                        requireResidentKey = true,
                        userVerification = "preferred"
                    )
                )
            else publicKeyCredentialCreationOptions

        val jsonRegistrationOptions =
            GsonBuilder().create().toJson(authenticatorSelectionFiller)

        val createPublicKeyCredentialRequest =
            CreatePublicKeyCredentialRequest(requestJson = jsonRegistrationOptions)

        val cancellationSignal = CancellationSignal()

        credentialManager!!.createCredentialAsync(
            request = createPublicKeyCredentialRequest,
            context = context,
            callback = object :
                CredentialManagerCallback<CreateCredentialResponse, CreateCredentialException> {
                override fun onError(e: CreateCredentialException) {
                    failure(ReachFiveError.from(e))
                }

                override fun onResult(result: CreateCredentialResponse) {
                    when (result) {
                        is CreatePublicKeyCredentialResponse -> {
                            val registrationPublicKeyCredential = Gson().fromJson(
                                result.registrationResponseJson,
                                RegistrationPublicKeyCredential::class.java
                            )

                            f(registrationPublicKeyCredential, success, failure)
                        }

                        // FIXME error message
                        else -> failure(ReachFiveError("Unexpected credential success response"))
                    }
                }

            },
            cancellationSignal = cancellationSignal,
            executor = ContextCompat.getMainExecutor(context),
        )
    }


    override fun signupWithPasskey(
        profile: ProfileWebAuthnSignupRequest,
        scope: Collection<String>,
        friendlyName: String?,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>,
        // TODO clarify, activity context
        context: Context
    ) {
        if (credentialManager == null || sdkConfig.originWebAuthn == null)
            failure(ReachFiveError("Credential Manager or origin is null"))

        val newFriendlyName = formatFriendlyName(friendlyName)

        reachFiveApi
            .createWebAuthnSignupOptions(
                WebAuthnRegistrationRequest(
                    sdkConfig.originWebAuthn!!,
                    newFriendlyName,
                    profile,
                    sdkConfig.clientId,
                ),
                SdkInfos.getQueries()
            )
            .enqueue(
                ReachFiveApiCallback.withContent<RegistrationOptions>(
                    success = { registrationOptions ->
                        handlePasskeySignup(
                            registrationOptions,
                            scope,
                            success,
                            failure,
                            context
                        )
                    },
                    failure = failure
                )
            )
    }

    private fun handlePasskeySignup(
        registrationOptions: RegistrationOptions,
        scope: Collection<String>,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>,
        context: Context
    ) {
        val webauthnId = registrationOptions.options.publicKey.user.id

        val signup =
            { registrationPublicKeyCredential: RegistrationPublicKeyCredential,
              success: Success<AuthToken>,
              failure: Failure<ReachFiveError> ->
                reachFiveApi
                    .signupWithWebAuthn(
                        WebauthnSignupCredential(
                            webauthnId = webauthnId,
                            publicKeyCredential = registrationPublicKeyCredential
                        )
                    )
                    .enqueue(
                        ReachFiveApiCallback.withContent<AuthenticationToken>(
                            success = {
                                sessionUtils.loginCallback(
                                    it.tkn,
                                    scope,
                                    success,
                                    failure
                                )
                            },
                            failure = failure
                        )
                    )
            }

        handleNewPasskey(
            registrationOptions.options.publicKey,
            context,
            success,
            failure,
            signup
        )

    }


    override fun loginWithPasskey(
        // FIXME scope in login request
        loginRequest: WebAuthnLoginRequest,
        scope: Collection<String>,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>,
        context: Context
    ) {
        if (credentialManager == null || sdkConfig.originWebAuthn == null)
            failure(ReachFiveError("Credential Manager or origin is null"))

        reachFiveApi.createWebAuthnAuthenticationOptions(
            WebAuthnLoginRequest.enrichWithClientId(loginRequest, sdkConfig.clientId, sdkConfig.originWebAuthn!!)
        ).enqueue(
            ReachFiveApiCallback.withContent<AuthenticationOptions>(
                success = { authenticationOptions ->
                    val requestJson = Gson().toJson(authenticationOptions.publicKey)

                    val getCredentialRequest =
                        GetCredentialRequest(listOf(GetPublicKeyCredentialOption(requestJson)))

                    handleCredentialManagerLogin(
                        getCredentialRequest,
                        context,
                        scope,
                        success,
                        failure
                    )
                },
                failure = failure
            )
        )
    }

    private fun handleCredentialManagerLogin(
        getCredentialRequest: GetCredentialRequest,
        context: Context,
        scope: Collection<String>,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>
    ) {

        val cancellationSignal = CancellationSignal()

        credentialManager!!.getCredentialAsync(
            context = context,
            request = getCredentialRequest,
            cancellationSignal = cancellationSignal,
            executor = ContextCompat.getMainExecutor(context),
            callback = object :
                CredentialManagerCallback<GetCredentialResponse, GetCredentialException> {
                override fun onError(e: GetCredentialException) {
                    failure(ReachFiveError.from(e))
                }

                override fun onResult(result: GetCredentialResponse) {
                    when (val credential = result.credential) {
                        is PasswordCredential -> {
                            val (email, phone) =
                                if (credential.id.contains('@')) Pair(credential.id, null)
                                else Pair(null, credential.id)

                            passwordAuth.loginWithPassword(
                                // FIXME Handle other identifier types
                                email = email,
                                phoneNumber = phone,
                                password = credential.password,
                                success = success,
                                failure = failure
                            )
                        }

                        is PublicKeyCredential -> {
                            val authenticationPublicKeyCredential = Gson().fromJson(
                                credential.authenticationResponseJson,
                                AuthenticationPublicKeyCredential::class.java
                            )

                            reachFiveApi
                                .authenticateWithWebAuthn(authenticationPublicKeyCredential)
                                .enqueue(
                                    ReachFiveApiCallback.withContent<AuthenticationToken>(
                                        success = {
                                            sessionUtils.loginCallback(
                                                it.tkn,
                                                scope,
                                                success,
                                                failure
                                            )
                                        },
                                        failure = failure
                                    )
                                )
                        }
                    }
                }
            }
        )
    }

    override fun discoverableLogin(
        scope: Collection<String>,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>,
        context: Context
    ) {
        if (credentialManager == null || sdkConfig.originWebAuthn == null)
            failure(ReachFiveError("Credential Manager or origin is null"))

        reachFiveApi.createWebAuthnAuthenticationOptions(
            WebAuthnLoginRequest.DiscoverableWithClientIdLoginRequest(
                origin = sdkConfig.originWebAuthn!!,
                scope = formatScope(scope),
                clientId = sdkConfig.clientId
            )
        ).enqueue(
            ReachFiveApiCallback.withContent<AuthenticationOptions>(
                success = { authenticationOptions ->
                    val requestJson = Gson().toJson(authenticationOptions.publicKey)

                    val getCredentialRequest =
                        GetCredentialRequest(listOf(GetPublicKeyCredentialOption(requestJson)))

                    handleCredentialManagerLogin(
                        getCredentialRequest,
                        context,
                        scope,
                        success,
                        failure
                    )
                },
                failure = failure
            )
        )
    }

}

internal interface CredentialManagerAuth {
    companion object {
    }

    var defaultScope: Set<String>

    fun discoverableLogin(
        scope: Collection<String> = defaultScope,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>,
        context: Context
    )

    fun loginWithPasskey(
        loginRequest: WebAuthnLoginRequest,
        scope: Collection<String> = defaultScope,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>,
        context: Context
    )

    fun signupWithPasskey(
        profile: ProfileWebAuthnSignupRequest,
        scope: Collection<String> = defaultScope,
        friendlyName: String?,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>,
        context: Context
    )

    fun registerNewPasskey(
        authToken: AuthToken,
        friendlyName: String?,
        success: Success<Unit>,
        failure: Failure<ReachFiveError>,
        context: Context
    )
}
