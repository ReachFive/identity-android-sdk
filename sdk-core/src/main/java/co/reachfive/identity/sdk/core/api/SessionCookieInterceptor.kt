package co.reachfive.identity.sdk.core.api

import android.util.Log
import android.webkit.CookieManager
import co.reachfive.identity.sdk.core.ReachFive.Companion.TAG
import okhttp3.Interceptor
import okhttp3.Response

class SessionCookieInterceptor(private val domain: String) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())

        val url =  chain.request().url.toString()
        val isTokenEndpoint = url.startsWith("https://$domain/oauth/token")
        val isLogoutEndpoint = url.startsWith("https://$domain/identity/v1/logout")
        val isVerifyPasswordless = url.startsWith("https://$domain/identity/v1/passwordless/verify")

        if (isTokenEndpoint || isLogoutEndpoint || isVerifyPasswordless)
            CookieManager.getInstance()?.let { cm ->
                response.headers("Set-Cookie").forEach { cookie ->
                    Log.d(TAG, "set cookie")
                    cm.setCookie("https://$domain", cookie)
                }
            }

        return response
    }
}