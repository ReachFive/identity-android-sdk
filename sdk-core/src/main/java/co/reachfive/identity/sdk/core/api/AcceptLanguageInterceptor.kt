package co.reachfive.identity.sdk.core.api

import okhttp3.Interceptor
import okhttp3.Response
import java.util.Locale

class AcceptLanguageInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()
        requestBuilder.header("Accept-Language", Locale.getDefault().toLanguageTag())
        return chain.proceed(requestBuilder.build())
    }
}