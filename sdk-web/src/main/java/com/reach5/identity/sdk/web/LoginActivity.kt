package com.reach5.identity.sdk.web

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.browser.customtabs.*
import com.reach5.identity.sdk.core.utils.Pkce

class LoginActivity : Activity() {

    companion object {
        const val REQUEST_CODE = 100
    }

    private var hasResult: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val config = intent.getParcelableExtra<WebProviderConfig>(ConfiguredWebProvider.CONFIG_KEY)
        val pkce = intent.getParcelableExtra<Pkce>(ConfiguredWebProvider.PKCE_KEY)
        val url = config.buildUrl(pkce)

        Log.d(LOG_TAG, "ReachFiveLoginActivity onCreated launch url : $url")

        val customTabsIntent = CustomTabsIntent.Builder().build().intent

        customTabsIntent.data = Uri.parse(url)
        startActivityForResult(customTabsIntent, REQUEST_CODE)
    }

    override fun onResume() {
        super.onResume()

        if (hasResult) {
            setResult(ConfiguredWebProvider.ABORT_CODE, intent)
            finish()
        } else {
            hasResult = true
        }
    }

    override fun onNewIntent(newIntent: Intent) {
        val data = newIntent.data

        val newResultCode = if (newIntent.action != Intent.ACTION_VIEW || data == null) {
            ConfiguredWebProvider.UNEXPECTED_ERROR_CODE
        } else {
            val authCode = data.getQueryParameter("code")

            if (authCode == null) {
                ConfiguredWebProvider.NO_AUTH_ERROR_CODE
            } else {
                intent.putExtra(ConfiguredWebProvider.AUTH_CODE_KEY, authCode)
                ConfiguredWebProvider.SUCCESS_CODE
            }
        }

        setResult(newResultCode, intent)
        finish()
    }

}
