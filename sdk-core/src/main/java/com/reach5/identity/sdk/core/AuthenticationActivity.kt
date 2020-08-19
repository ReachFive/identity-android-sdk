package com.reach5.identity.sdk.core

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent

class AuthenticationActivity : Activity() {
    companion object {
        const val CHROME_CUSTOM_TAB_REQUEST_CODE = 100

        const val SUCCESS_RESULT_CODE = 0
        const val UNEXPECTED_ERROR_RESULT_CODE = -1
        const val ABORT_RESULT_CODE = 1
        const val NO_AUTH_ERROR_RESULT_CODE = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val urlString: String = intent.getStringExtra("URL")

        Log.d("REACHFIVE", urlString)

        val customTabsIntent = CustomTabsIntent.Builder().build().intent
        customTabsIntent.data = Uri.parse(urlString)
        startActivityForResult(customTabsIntent, CHROME_CUSTOM_TAB_REQUEST_CODE)
    }

    override fun onNewIntent(intent: Intent?) {
        val data = intent?.data

        val newResultCode = if (intent?.action != Intent.ACTION_VIEW || data == null) {
            UNEXPECTED_ERROR_RESULT_CODE
        } else {
            val authCode = data.getQueryParameter("code")

            if (authCode == null) {
                NO_AUTH_ERROR_RESULT_CODE
            } else {
                intent.putExtra("CODE", authCode)
                SUCCESS_RESULT_CODE
            }
        }

        setResult(newResultCode, intent)
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CHROME_CUSTOM_TAB_REQUEST_CODE && data != null) {
            return when (resultCode) {
                SUCCESS_RESULT_CODE -> {
                    val authCode = data.getStringExtra("CODE")!!
                }
                ABORT_RESULT_CODE -> {
                    Unit
                }
                else -> {

                }
            }
        }
    }
}