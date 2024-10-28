package co.reachfive.identity.sdk.core.api

import android.util.Log
import android.webkit.CookieManager
import co.reachfive.identity.sdk.core.ReachFive.Companion.TAG
import okhttp3.Interceptor
import okhttp3.Response

class LogoutCookieInterceptor(private val domain: String) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val request =
            if (originalRequest.url.toString().startsWith("https://$domain/identity/v1/logout"))
                CookieManager.getInstance()?.let { cm ->
                    val cookies = cm.getCookie("https://$domain")?.split("; ") ?: emptyList()

                    val requestBuilder = originalRequest.newBuilder()

                    cookies.forEach {
                        Log.d(TAG, "cookie attached to https://$domain/identity/v1/logout: $it")
                        requestBuilder.addHeader("Cookie", it)
                    }

                    requestBuilder.build()
                } ?: originalRequest
            else originalRequest


        return chain.proceed(request)
    }
}