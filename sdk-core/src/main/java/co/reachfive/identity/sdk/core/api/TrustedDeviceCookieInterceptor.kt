package co.reachfive.identity.sdk.core.api

import android.webkit.CookieManager
import okhttp3.Interceptor
import okhttp3.Response

class TrustedDeviceCookieInterceptor(private val domain: String) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request().newBuilder()

        val url =  chain.request().url.toString()
        val isLoginUrl = url.startsWith("https://$domain/identity/v1/password/login")

        if (isLoginUrl)
            CookieManager.getInstance()?.let { cm ->
                val cookies = cm.getCookie("https://$domain")?.split("; ") ?: emptyList()
                cookies.forEach {
                    req.addHeader("Cookie", it)
                }
            }

        return chain.proceed(req.build())
    }
}