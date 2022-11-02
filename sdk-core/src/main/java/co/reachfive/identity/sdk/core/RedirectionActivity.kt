package co.reachfive.identity.sdk.core

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import co.reachfive.identity.sdk.core.ReachFive.Companion.TAG

class RedirectionActivity : Activity() {
    companion object {
        const val FQDN = "co.reachfive.identity.sdk.core.RedirectionActivity"
        const val CODE_VERIFIER_KEY = "CODE_VERIFIER"
        const val URL_KEY = "URL"
        const val SCHEME = "SCHEME"

        const val RC_WEBLOGIN = 52557

        fun isLoginRequestCode(code: Int): Boolean =
            setOf(RC_WEBLOGIN).contains(code)
    }

    private var started = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val urlString = intent.getStringExtra(URL_KEY)
        val codeVerifier = intent.getStringExtra(CODE_VERIFIER_KEY)

        val customTabsIntent = CustomTabsIntent.Builder().build().intent
        customTabsIntent.data = Uri.parse(urlString)
        customTabsIntent.putExtra(CODE_VERIFIER_KEY, codeVerifier)

        startActivity(customTabsIntent)
    }

    override fun onNewIntent(newIntent: Intent) {
        newIntent.flags = 0

        val scheme = intent.getStringExtra(SCHEME) ?: "???"
        val intentClass = newIntent.resolveActivity(packageManager).className
        val url = newIntent.data

        if (intentClass == FQDN && url.toString().startsWith(scheme)) {
            intent.data = newIntent.data
            setResult(Activity.RESULT_OK, intent)
        } else {
            Log.e(TAG, "Unrecognized intent!")
            setResult(Activity.RESULT_CANCELED)
        }

        finish()
    }

    override fun onResume() {
        super.onResume()

        if (!started) {
            started = true
        } else {
            finish()
        }
    }
}