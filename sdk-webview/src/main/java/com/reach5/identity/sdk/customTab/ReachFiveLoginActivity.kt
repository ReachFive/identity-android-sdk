package com.reach5.identity.sdk.customTab

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.browser.customtabs.*
import com.reach5.identity.sdk.core.utils.Pkce
import com.reach5.identity.sdk.customTab.ConfiguredCustomTabProvider.Companion.PKCE


class ReachFiveLoginActivity : Activity() {
    private val CUSTOM_TAB_PACKAGE_NAME = "com.reach5.identity.sdk.customtab"
    private val TAG = "Reach5"

    private var authCode: String? = null

    private var mCustomTabsServiceConnection: CustomTabsServiceConnection? = null
    private var mClient: CustomTabsClient? = null
    private var mCustomTabsSession: CustomTabsSession? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mCustomTabsServiceConnection = object : CustomTabsServiceConnection() {
            override fun onCustomTabsServiceConnected(
                componentName: ComponentName,
                customTabsClient: CustomTabsClient
            ) {
                //Pre-warming
                mClient = customTabsClient
                mClient?.warmup(0L)
                mCustomTabsSession = mClient?.newSession(object : CustomTabsCallback() {})
            }

            override fun onServiceDisconnected(name: ComponentName) {
                mClient = null
                mCustomTabsServiceConnection = null
            }
        }

        CustomTabsClient.bindCustomTabsService(this, CUSTOM_TAB_PACKAGE_NAME, mCustomTabsServiceConnection)

        val config = intent.getParcelableExtra<CustomTabProviderConfig>(ConfiguredCustomTabProvider.BUNDLE_ID)
        val pkce = getPkceFromIntent(intent)
        val url = config.buildUrl(pkce)

        Log.d(TAG, "ReachFiveLoginActivity onCreated launch url : $url")
        CustomTabsIntent.Builder(mCustomTabsSession)
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
            newIntent.putExtra(ConfiguredCustomTabProvider.AUTH_CODE, authCode)
        }
        else {
            newIntent?.putExtra(ConfiguredCustomTabProvider.RESULT_INTENT_ERROR, "No authorization core retrieved.")
        }

        // Put the PKCE in the new intent
        newIntent?.putExtra(PKCE, getPkceFromIntent(intent))

        setResult(ConfiguredCustomTabProvider.REQUEST_CODE, newIntent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()

        if (mCustomTabsServiceConnection == null) return

        Log.d(TAG, "ReachFiveLoginActivity onDestroy")
        this.unbindService(mCustomTabsServiceConnection)
        mClient = null
        mCustomTabsSession = null
    }

    private fun getPkceFromIntent(intent: Intent) = intent.getParcelableExtra<Pkce>(PKCE)

}
