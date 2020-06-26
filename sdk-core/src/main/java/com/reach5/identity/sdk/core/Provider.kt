package com.reach5.identity.sdk.core

import android.app.Activity
import android.content.Intent
import com.reach5.identity.sdk.core.api.ReachFiveApi
import com.reach5.identity.sdk.core.models.AuthToken
import com.reach5.identity.sdk.core.models.ProviderConfig
import com.reach5.identity.sdk.core.models.ReachFiveError
import com.reach5.identity.sdk.core.models.SdkConfig
import com.reach5.identity.sdk.core.utils.Failure
import com.reach5.identity.sdk.core.utils.Success

interface ProviderCreator {
    val name: String
    fun create(
        providerConfig: ProviderConfig,
        sdkConfig: SdkConfig,
        reachFiveApi: ReachFiveApi,
        activity: Activity
    ): Provider
}

/**
 * Common interface of the provider
 */
interface Provider {

    val name: String

    /**
     * Identifier of the request, that identifies the return of an activity
     */
    val requestCode: Int

    /**
     * Initiate login action
     */
    fun login(origin: String, scope: Collection<String>?, activity: Activity)

    /**
     * Handle activity result of login action
     */
    fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>
    )

    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
        failure: Failure<ReachFiveError>
    )

    /**
     * On stop activity lifecycle
     */
    fun onStop() {}

    fun logout() {}
}
