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
        const val FQN = "co.reachfive.identity.sdk.core.RedirectionActivity"
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
        // remove any flags from the new intent
        newIntent.flags = 0

        val intentClass = newIntent.resolveActivity(packageManager).className

        val scheme = intent.getStringExtra(SCHEME)
        val url = newIntent.data
        val isTargetR5 = intentClass == FQN && scheme != null && url.toString().startsWith(scheme)
        if (!isTargetR5) {
            Log.e(TAG, "Unrecognized intent class: $intentClass")
        }

        val callingPackageName = this.callingActivity?.packageName
        val isTrustedCaller =
            callingPackageName != null && callingPackageName == applicationContext.packageName
        if (!isTrustedCaller) {
            Log.e(TAG, "Unrecognized calling activity: ${callingPackageName ?: "N/A"}")
        }

        // ensure intent target && URL belong to us
        if (isTargetR5 && isTrustedCaller) {
            intent.data = url
            setResult(RESULT_OK, intent)
        } else {
            setResult(RESULT_CANCELED)
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