package com.reach5.identity.sdk.web

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.browser.customtabs.*
import com.reach5.identity.sdk.core.utils.Pkce
import com.reach5.identity.sdk.web.ConfiguredWebProvider.Companion.PKCE


class ReachFiveLoginActivity : Activity() {
    private val CUSTOM_TAB_PACKAGE_NAME = "com.reach5.identity.sdk.customtab"
    private val TAG = "Reach5"

    private var authCode: String? = null

    private var customTabsConnection: CustomTabsServiceConnection? = null
    private var customTabsClient: CustomTabsClient? = null
    private var customTabsSession: CustomTabsSession? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        customTabsConnection = object : CustomTabsServiceConnection() {
            override fun onCustomTabsServiceConnected(
                componentName: ComponentName,
                newCustomTabsClient: CustomTabsClient
            ) {
                //Pre-warming
                customTabsClient = newCustomTabsClient
                customTabsClient?.warmup(0L)
                customTabsSession = customTabsClient?.newSession(object : CustomTabsCallback() {})
            }

            override fun onServiceDisconnected(name: ComponentName) {
                customTabsClient = null
                customTabsConnection = null
            }
        }

        CustomTabsClient.bindCustomTabsService(this, CUSTOM_TAB_PACKAGE_NAME, customTabsConnection)

        val config = intent.getParcelableExtra<WebProviderConfig>(ConfiguredWebProvider.BUNDLE_ID)
        val pkce = getPkceFromIntent(intent)
        val url = config.buildUrl(pkce)

        Log.d(TAG, "ReachFiveLoginActivity onCreated launch url : $url")
        CustomTabsIntent.Builder(customTabsSession)
            .build()
            .launchUrl(this, Uri.parse(url))
    }

    override fun onNewIntent(newIntent: Intent?) {
        super.onNewIntent(newIntent)

        val appLinkAction = newIntent?.action
        val appLinkData: Uri? = newIntent?.data

        if (Intent.ACTION_VIEW == appLinkAction && appLinkData!= null) {
            // Get the authorization code
            authCode = appLinkData.getQueryParameter("code")
            newIntent.putExtra(ConfiguredWebProvider.AUTH_CODE, authCode)
        }
        else {
            newIntent?.putExtra(ConfiguredWebProvider.RESULT_INTENT_ERROR, "No authorization core retrieved.")
        }

        // Put the PKCE in the new intent
        newIntent?.putExtra(PKCE, getPkceFromIntent(intent))

        setResult(ConfiguredWebProvider.REQUEST_CODE, newIntent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()

        if (customTabsConnection == null) return

        Log.d(TAG, "ReachFiveLoginActivity onDestroy")
        this.unbindService(customTabsConnection)
        customTabsClient = null
        customTabsSession = null
    }

    private fun getPkceFromIntent(intent: Intent) = intent.getParcelableExtra<Pkce>(PKCE)

}
