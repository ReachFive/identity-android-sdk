package co.reachfive.identity.sdk.core

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.browser.customtabs.CustomTabsIntent
import co.reachfive.identity.sdk.core.ReachFive.Companion.TAG
import co.reachfive.identity.sdk.core.databinding.ReachfiveWebviewBinding
import java.util.regex.Pattern


class RedirectionActivity : Activity() {
    private lateinit var binding: ReachfiveWebviewBinding

    companion object {
        const val FQN = "co.reachfive.identity.sdk.core.RedirectionActivity"
        const val CODE_VERIFIER_KEY = "CODE_VERIFIER"
        const val URL_KEY = "URL"
        const val SCHEME = "SCHEME"
        const val USE_NATIVE_WEBVIEW = "USE_NATIVE_WEBVIEW"

        const val RC_WEBLOGIN = 52557

        fun isLoginRequestCode(code: Int): Boolean =
            setOf(RC_WEBLOGIN).contains(code)
    }

    private var started = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val urlString = intent.getStringExtra(URL_KEY)
        val codeVerifier = intent.getStringExtra(CODE_VERIFIER_KEY)

        val useWebview = intent.getBooleanExtra(USE_NATIVE_WEBVIEW, false)
        if (useWebview) {

            binding = ReachfiveWebviewBinding.inflate(layoutInflater)

            setContentView(binding.root)

            binding.webview.apply {
                @SuppressLint("SetJavaScriptEnabled")
                settings.javaScriptEnabled = true
                webViewClient = ReachFiveWebViewClient(codeVerifier)
            }

            urlString?.let { binding.webview.loadUrl(urlString) }

        } else {

            Log.d(TAG, "RedirectionActivity onCreate url: $urlString")

            val customTabsIntent = CustomTabsIntent.Builder().build().intent
            customTabsIntent.data = Uri.parse(urlString)
            customTabsIntent.putExtra(CODE_VERIFIER_KEY, codeVerifier)

            startActivity(customTabsIntent)

        }
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


    override fun onBackPressed() {
        if (binding.webview.canGoBack()) {
            binding.webview.goBack()
        } else {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    inner class ReachFiveWebViewClient(val codeVerifier: String?) : WebViewClient() {

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val urlString = request.url.toString()
            Log.d(TAG, "ReachfiveWebViewClient shouldOverrideUrlLoading url: $urlString")
            return dispatchUrl(urlString)
        }

        @Suppress("DEPRECATION")
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            Log.e(TAG, "ReachfiveWebViewClient shouldOverrideUrlLoading deprecated url: $url")
            return dispatchUrl(url)
        }

        private fun dispatchUrl(url: String): Boolean {
            // regex : (reachfive://word character: [a-zA-Z_0-9]/callback)(any character zero or more times)
            val pattern = Pattern.compile("^(reachfive:\\/\\/\\w+\\/callback)(.*)$")
            val isTargetReachFive = pattern.matcher(url).matches()
            Log.d(TAG, "ReachfiveWebViewClient dispatchUrl isReachFiveScheme: $isTargetReachFive")

            return if (isTargetReachFive) {
                val intent = Intent()
                intent.data = Uri.parse(url)
                intent.putExtra(CODE_VERIFIER_KEY, codeVerifier)
                setResult(RC_WEBLOGIN, intent)
                finish()
                true
            } else false
        }
    }

}
