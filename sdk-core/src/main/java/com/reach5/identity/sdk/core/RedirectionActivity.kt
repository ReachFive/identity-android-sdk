package com.reach5.identity.sdk.core

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.browser.customtabs.CustomTabsIntent

class RedirectionActivity : Activity() {
    companion object {
        const val CODE_KEY = "CODE"
        const val CODE_VERIFIER_KEY = "CODE_VERIFIER"
        const val URL_KEY = "URL"

        const val REDIRECTION_REQUEST_CODE = 52558
        const val CHROME_CUSTOM_TAB_REQUEST_CODE = 100

        const val SUCCESS_RESULT_CODE = 0
        const val UNEXPECTED_ERROR_RESULT_CODE = -1
        const val ABORT_RESULT_CODE = 1
        const val NO_AUTH_ERROR_RESULT_CODE = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val urlString = intent.getStringExtra(URL_KEY)
        val codeVerifier = intent.getStringExtra(CODE_VERIFIER_KEY)

        val customTabsIntent = CustomTabsIntent.Builder().build().intent
        customTabsIntent.data = Uri.parse(urlString)
        customTabsIntent.putExtra(CODE_VERIFIER_KEY, codeVerifier)

        startActivityForResult(customTabsIntent, CHROME_CUSTOM_TAB_REQUEST_CODE)
    }

    override fun onNewIntent(newIntent: Intent) {
        val data = newIntent.data

        val newResultCode = if (newIntent.action != Intent.ACTION_VIEW || data == null) {
            UNEXPECTED_ERROR_RESULT_CODE
        } else {
            val authCode = data.getQueryParameter("code")

            if (authCode == null) NO_AUTH_ERROR_RESULT_CODE
            else {
                intent.putExtra(CODE_KEY, authCode)
                SUCCESS_RESULT_CODE
            }
        }

        setResult(newResultCode, intent)
        finish()
    }
}