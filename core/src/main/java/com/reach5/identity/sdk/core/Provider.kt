package com.reach5.identity.sdk.core

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.reach5.identity.sdk.core.api.ReachFiveApi
import com.reach5.identity.sdk.core.models.OpenIdTokenResponse
import com.reach5.identity.sdk.core.models.ProviderConfig
import com.reach5.identity.sdk.core.models.ReachFiveError
import com.reach5.identity.sdk.core.models.SdkConfig
import com.reach5.identity.sdk.core.utils.Failure
import com.reach5.identity.sdk.core.utils.Success

interface Provider {
    val name: String
    val requestCode: Int
    val providerConfig: ProviderConfig
    val reachFiveApi: ReachFiveApi
    fun login(origin: String, activity: Activity)
    fun onActivityResult(requestCode: Int, data: Intent?, success: Success<OpenIdTokenResponse>, failure: Failure<ReachFiveError>)
    fun onStop() {}
    fun logout() { /* TODO */ }
}

interface ProviderCreator {
    val name: String
    fun create(providerConfig: ProviderConfig, sdkConfig: SdkConfig, reachFiveApi: ReachFiveApi, context: Context): Provider
}
