package co.reachfive.identity.sdk.core.api

import okhttp3.Interceptor
import okhttp3.Response

class CorrelationIdInterceptor(private val correlationId: String): Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val builder = chain.request().newBuilder()
        builder.addHeader("X-R5-Correlation-Id", correlationId)
        return chain.proceed(builder.build())
    }
}