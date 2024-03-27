package co.reachfive.identity.sdk.core

import android.app.Activity
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

    private fun checkInit(failure: Failure<ReachFiveError>) {
        if (credentialManager == null || sdkConfig.originWebAuthn == null)
            failure(ReachFiveError("Credential Manager or origin not properly initialized"))
    }

    override fun registerNewPasskey(
        authToken: AuthToken,
        friendlyName: String?,
        success: Success<Unit>,
        failure: Failure<ReachFiveError>,
        activity: Activity
    ) {
        checkInit(failure)

        this.authToken = authToken

        reachFiveApi
            .createWebAuthnRegistrationOptions(
                authToken.authHeader,
                WebAuthnRegistrationRequest(
                    sdkConfig.originWebAuthn!!,
                    formatFriendlyName(friendlyName)
                )
            )
            .enqueue(
                ReachFiveApiCallback.withContent<RegistrationOptions>(
                    success = { registrationOptions ->
                        handlePasskeyRegistration(
                            registrationOptions,
                            success,
                            failure,
                            activity
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
        activity: Activity
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
            activity,
            success,
            failure,
            register
        )

    }

    override fun signupWithPasskey(
        profile: ProfileWebAuthnSignupRequest,
        friendlyName: String?,
        scope: Collection<String>,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>,
        activity: Activity
    ) {
        checkInit(failure)

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
                            activity
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
        activity: Activity
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
            activity,
            success,
            failure,
            signup
        )

    }

    private fun <T> handleNewPasskey(
        publicKeyCredentialCreationOptions: R5PublicKeyCredentialCreationOptions,
        activity: Activity,
        success: Success<T>,
        failure: Failure<ReachFiveError>,
        f: (RegistrationPublicKeyCredential, Success<T>, Failure<ReachFiveError>) -> Unit,
    ) {

        val jsonRegistrationOptions =
            GsonBuilder().create().toJson(publicKeyCredentialCreationOptions)

        val createPublicKeyCredentialRequest =
            CreatePublicKeyCredentialRequest(requestJson = jsonRegistrationOptions)

        val cancellationSignal = CancellationSignal()

        credentialManager!!.createCredentialAsync(
            request = createPublicKeyCredentialRequest,
            context = activity,
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

                        else -> failure(ReachFiveError("Unexpected credential success response"))
                    }
                }

            },
            cancellationSignal = cancellationSignal,
            executor = ContextCompat.getMainExecutor(activity),
        )
    }

    override fun createPasswordCredential(
        id: String,
        password: String,
        activity: Activity,
        success: Success<Unit>,
        failure: Failure<ReachFiveError>,
    ) {
        checkInit(failure)

        val createPasswordRequest =
            CreatePasswordRequest(id = id, password = password)


        val cancellationSignal = CancellationSignal()

        credentialManager!!.createCredentialAsync(
            request = createPasswordRequest,
            context = activity,
            callback =
            object :
                CredentialManagerCallback<CreateCredentialResponse, CreateCredentialException> {
                override fun onError(e: CreateCredentialException) {
                    failure(ReachFiveError.from(e))
                }

                override fun onResult(result: CreateCredentialResponse) {
                    success(Unit)
                }

            },
            cancellationSignal = cancellationSignal,
            executor = ContextCompat.getMainExecutor(activity),
        )
    }

    override fun discoverableLogin(
        scope: Collection<String>,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>,
        activity: Activity
    ) {
        checkInit(failure)

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
                        GetCredentialRequest(
                            listOf(
                                GetPasswordOption(),
                                GetPublicKeyCredentialOption(requestJson)
                            )
                        )

                    handleCredentialManagerLogin(
                        getCredentialRequest,
                        activity,
                        scope,
                        success,
                        failure
                    )
                },
                failure = failure
            )
        )
    }

    override fun loginWithPasskey(
        loginRequest: WebAuthnLoginRequest,
        scope: Collection<String>,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>,
        activity: Activity
    ) {
        checkInit(failure)

        reachFiveApi.createWebAuthnAuthenticationOptions(
            WebAuthnLoginRequest.enrichWithClientId(
                loginRequest,
                sdkConfig.clientId,
                sdkConfig.originWebAuthn!!
            )
        ).enqueue(
            ReachFiveApiCallback.withContent<AuthenticationOptions>(
                success = { authenticationOptions ->
                    val requestJson = Gson().toJson(authenticationOptions.publicKey)

                    val getCredentialRequest =
                        GetCredentialRequest(listOf(GetPublicKeyCredentialOption(requestJson)))

                    handleCredentialManagerLogin(
                        getCredentialRequest,
                        activity,
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
        activity: Activity,
        scope: Collection<String>,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>
    ) {
        val cancellationSignal = CancellationSignal()

        credentialManager!!.getCredentialAsync(
            context = activity,
            request = getCredentialRequest,
            cancellationSignal = cancellationSignal,
            executor = ContextCompat.getMainExecutor(activity),
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
                                email = email,
                                phoneNumber = phone,
                                password = credential.password,
                                scope = scope,
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

}

internal interface CredentialManagerAuth {

    var defaultScope: Set<String>

    fun discoverableLogin(
        scope: Collection<String> = defaultScope,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>,
        activity: Activity
    )

    fun loginWithPasskey(
        loginRequest: WebAuthnLoginRequest,
        scope: Collection<String> = defaultScope,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>,
        activity: Activity
    )

    fun signupWithPasskey(
        profile: ProfileWebAuthnSignupRequest,
        friendlyName: String?,
        scope: Collection<String> = defaultScope,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>,
        activity: Activity
    )

    fun registerNewPasskey(
        authToken: AuthToken,
        friendlyName: String?,
        success: Success<Unit>,
        failure: Failure<ReachFiveError>,
        activity: Activity
    )

    fun createPasswordCredential(
        id: String,
        password: String,
        activity: Activity,
        success: Success<Unit>,
        failure: Failure<ReachFiveError>,
    )
}
