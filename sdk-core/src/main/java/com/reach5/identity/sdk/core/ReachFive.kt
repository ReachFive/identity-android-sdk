package com.reach5.identity.sdk.core

import android.app.Activity
import android.content.Intent
import android.util.Log
import com.reach5.identity.sdk.core.api.ReachFiveApi
import com.reach5.identity.sdk.core.api.ReachFiveApiCallback
import com.reach5.identity.sdk.core.models.*
import com.reach5.identity.sdk.core.models.requests.UpdatePasswordRequest.Companion.enrichWithClientId
import com.reach5.identity.sdk.core.models.requests.*
import com.reach5.identity.sdk.core.utils.Failure
import com.reach5.identity.sdk.core.utils.Success
import com.reach5.identity.sdk.core.utils.SuccessWithNoContent

class ReachFive(val activity: Activity, val sdkConfig: SdkConfig, val providersCreators: List<ProviderCreator>) {

    companion object {
        private const val TAG = "Reach5"
    }

    private val reachFiveApi: ReachFiveApi = ReachFiveApi.create(sdkConfig)

    private var scope: Set<String> = emptySet()

    private var providers: List<Provider> = emptyList()

    fun initialize(success: Success<List<Provider>> = {}, failure: Failure<ReachFiveError> = {}): ReachFive {
        reachFiveApi
            .clientConfig(mapOf("client_id" to sdkConfig.clientId))
            .enqueue(ReachFiveApiCallback<ClientConfigResponse>(
                success = { clientConfig ->
                    scope = clientConfig.scope.split(" ").toSet()
                    providersConfigs(success, failure)
                },
                failure = failure)
            )

        return this
    }

    private fun providersConfigs(success: Success<List<Provider>>, failure: Failure<ReachFiveError>) {
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
                nativeProvider != null -> nativeProvider.create(config, sdkConfig, reachFiveApi, activity)
                webViewProvider != null -> webViewProvider.create(config, sdkConfig, reachFiveApi, activity)
                else -> {
                    Log.w(TAG, "Non supported provider found, please add webview or native provider")
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

    fun loginWithProvider(name: String, origin: String, activity: Activity) {
        getProvider(name)?.login(origin, activity)
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
            .enqueue(ReachFiveApiCallback(success = { it.toAuthToken().fold(success, failure) }, failure = failure))
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
            .enqueue(ReachFiveApiCallback(success = { it.toAuthToken().fold(success, failure) }, failure = failure))
    }

    fun logout(
        redirectTo: String? = null,
        successWithNoContent: SuccessWithNoContent<Unit>,
        failure: Failure<ReachFiveError>
    ) {
        val queries = SdkInfos.getQueries()
        val options = if (redirectTo != null) queries.plus(Pair("redirect_to", redirectTo)) else queries
        reachFiveApi
            .logout(options)
            .enqueue(ReachFiveApiCallback(successWithNoContent = successWithNoContent, failure = failure))
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
            .enqueue(ReachFiveApiCallback(successWithNoContent = successWithNoContent, failure = failure))
    }

    fun updateEmail(
        authToken: AuthToken,
        email: String,
        redirectUrl: String? = null,
        success: Success<Profile>,
        failure: Failure<ReachFiveError>
    ) {
        reachFiveApi
            .updateEmail(formatAuthorization(authToken),
                UpdateEmailRequest(email, redirectUrl), SdkInfos.getQueries())
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
        authToken: AuthToken,
        updatePhoneNumberRequest: UpdatePasswordRequest,
        successWithNoContent: SuccessWithNoContent<Unit>,
        failure: Failure<ReachFiveError>
    ) {
        reachFiveApi
            .updatePassword(
                formatAuthorization(authToken),
                enrichWithClientId(updatePhoneNumberRequest, sdkConfig.clientId),
                SdkInfos.getQueries()
            )
            .enqueue(ReachFiveApiCallback(successWithNoContent = successWithNoContent, failure = failure))
    }

    fun requestPasswordReset(
        authToken: AuthToken,
        email: String? = null,
        redirectUrl: String? = null,
        phoneNumber: String? = null,
        successWithNoContent: SuccessWithNoContent<Unit>,
        failure: Failure<ReachFiveError>
    ) {
        reachFiveApi
            .requestPasswordReset(
                formatAuthorization(authToken),
                RequestPasswordResetRequest(
                    sdkConfig.clientId,
                    email,
                    redirectUrl,
                    phoneNumber
                ),
                SdkInfos.getQueries()
            )
            .enqueue(ReachFiveApiCallback(successWithNoContent = successWithNoContent, failure = failure))
    }

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
        if (provider != null) {
            provider.onRequestPermissionsResult(requestCode, permissions, grantResults, failure)
        } else {
            failure(ReachFiveError.from("No provider found for this requestCode: $requestCode"))
        }
    }

    fun logoutWithProviders(callback: () -> Unit) {
        providers.forEach { it.logout() }.also { callback() }
    }

    fun onStop() {
        providers.forEach { it.onStop() }
    }

    private fun formatAuthorization(authToken: AuthToken): String {
        return "${authToken.tokenType} ${authToken.accessToken}"
    }

    private fun formatScope(scope: Collection<String>): String {
        return scope.toSet().joinToString(" ")
    }
}
