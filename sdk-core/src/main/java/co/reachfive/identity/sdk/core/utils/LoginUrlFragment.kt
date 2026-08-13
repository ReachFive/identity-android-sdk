package co.reachfive.identity.sdk.core.utils

import okhttp3.HttpUrl

/**
 * Returns this URL with [loginUrlFragment]'s key/value pairs set as its fragment
 * (`#key=value&key2=value2`), or unchanged when [loginUrlFragment] is null or empty.
 */
internal fun HttpUrl.withLoginUrlFragment(loginUrlFragment: Map<String, String>?): HttpUrl {
    if (loginUrlFragment.isNullOrEmpty()) return this

    // Reuse OkHttp's query encoding. The URL used is a crutch, only its encoded query is kept.
    val encoder = HttpUrl.Builder().scheme("https").host("localhost")
    loginUrlFragment.forEach { (key, value) -> encoder.addQueryParameter(key, value) }

    return newBuilder().encodedFragment(encoder.build().encodedQuery).build()
}
