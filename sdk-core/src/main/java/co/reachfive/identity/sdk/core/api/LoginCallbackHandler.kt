package co.reachfive.identity.sdk.core.api

import android.net.Uri
import co.reachfive.identity.sdk.core.models.ReachFiveError
import co.reachfive.identity.sdk.core.models.SdkConfig
import co.reachfive.identity.sdk.core.models.SdkInfos
import co.reachfive.identity.sdk.core.utils.Failure
import co.reachfive.identity.sdk.core.utils.PkceAuthCodeFlow
import co.reachfive.identity.sdk.core.utils.Success
import co.reachfive.identity.sdk.core.utils.TryOrNull.tryOrNull
import co.reachfive.identity.sdk.core.utils.formatScope
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.QueryMap

internal class LoginCallbackHandler(
    private val noFollowClient: NoFollowClient,
) {

    fun getAuthorizationCode(
        tkn: String,
        pkce: PkceAuthCodeFlow,
        clientId: String,
        redirectUri: String,
        scope: Collection<String>,
        nonce: String? = null,
        origin: String? = null,
        success: Success<String>,
        failure: Failure<ReachFiveError>,
    ) {

        val maybeNonce = if (nonce != null) mapOf("nonce" to nonce) else emptyMap()
        val maybeOrigin = if (origin != null) {
            mapOf("origin" to origin)
        } else emptyMap()

        val query = mapOf(
            "response_type" to "code",
            "client_id" to clientId,
            "redirect_uri" to redirectUri,
            "scope" to formatScope(scope),
            "response_mode" to "query",
            "code_challenge" to pkce.codeChallenge,
            "code_challenge_method" to pkce.codeChallengeMethod,
            "tkn" to tkn,
        ) + SdkInfos.getQueries() + maybeNonce + maybeOrigin

        noFollowClient
            .loginCallback(query)
            .enqueue(AuthCodeExtractorCallback(success, failure))
    }

    companion object {
        fun create(sdkConfig: SdkConfig): LoginCallbackHandler {
            val logging = HttpLoggingInterceptor()
            logging.apply { logging.level = HttpLoggingInterceptor.Level.BASIC }

            val client =
                OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .followRedirects(false)
                    .followSslRedirects(false)
                    .build()

            val api: NoFollowClient = Retrofit.Builder()
                .baseUrl("https://${sdkConfig.domain}")
                .client(client)
                .build()
                .create(NoFollowClient::class.java)

            return LoginCallbackHandler(api)
        }
    }
}

private class AuthCodeExtractorCallback(
    val success: Success<String>,
    val failure: Failure<ReachFiveError>
) : Callback<Unit> {
    override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
        if (response.raw().isRedirect) {
            val location = response.headers().get("Location")
            val uri = Uri.parse(location)
            val authCode = tryOrNull { uri.getQueryParameter("code") }

            if (authCode != null) {
                success(authCode)
            } else {
                val error = ReachFiveError.fromRedirectionResult(uri) ?: ReachFiveError.NoAuthCode
                failure(error)
            }
        } else {
            failure(ReachFiveError.fromHttpResponse(response))
        }
    }

    override fun onFailure(call: Call<Unit>, t: Throwable) {
        failure(ReachFiveError.from(t))
    }
}

internal interface NoFollowClient {

    @GET("/oauth/authorize")
    fun loginCallback(@QueryMap options: Map<String, String>): Call<Unit>
}


