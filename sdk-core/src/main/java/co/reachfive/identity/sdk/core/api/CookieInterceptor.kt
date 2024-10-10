package co.reachfive.identity.sdk.core.api

import android.util.Log
import android.webkit.CookieManager
import co.reachfive.identity.sdk.core.ReachFive.Companion.TAG
import okhttp3.Interceptor
import okhttp3.Response

class CookieInterceptor(private val domain: String) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())

        if (chain.request().url.toString().startsWith("https://$domain/oauth/token"))
            CookieManager.getInstance()?.let { cm ->
                response.headers("Set-Cookie").forEach { cookie ->
                    Log.d(TAG, "set cookie $cookie")
                    cm.setCookie("https://$domain", cookie)
                }
            }

        return response
    }
}