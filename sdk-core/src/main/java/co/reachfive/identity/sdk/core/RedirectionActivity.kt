package co.reachfive.identity.sdk.core

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.browser.customtabs.CustomTabsIntent
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.webkit.WebViewClientCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import co.reachfive.identity.sdk.core.ReachFive.Companion.TAG
import co.reachfive.identity.sdk.core.databinding.ReachfiveWebviewBinding
import co.reachfive.identity.sdk.core.utils.PasskeyWebListener
import java.util.regex.Pattern


class RedirectionActivity : ComponentActivity() {
    private lateinit var binding: ReachfiveWebviewBinding

    val passkeyListener = PasskeyWebListener(this, lifecycleScope)

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val urlString = intent.getStringExtra(URL_KEY)
        val codeVerifier = intent.getStringExtra(CODE_VERIFIER_KEY)

        val useWebView = intent.getBooleanExtra(USE_NATIVE_WEBVIEW, false)
        if (useWebView)
            launchWebView(codeVerifier, urlString)
        else
            launchCustomTab(urlString, codeVerifier)
    }

    private fun launchCustomTab(urlString: String?, codeVerifier: String?) {
        Log.d(TAG, "RedirectionActivity launchCustomTab url: $urlString")

        val customTabsIntent = CustomTabsIntent.Builder().build().intent
        customTabsIntent.data = Uri.parse(urlString)
        customTabsIntent.putExtra(CODE_VERIFIER_KEY, codeVerifier)

        startActivity(customTabsIntent)
    }

    private fun launchWebView(codeVerifier: String?, urlString: String?) {
        Log.d(TAG, "RedirectionActivity launchWebView url: $urlString")

        binding = ReachfiveWebviewBinding.inflate(layoutInflater)

        setContentView(binding.root)

        binding.webview.apply {
            @SuppressLint("SetJavaScriptEnabled")
            settings.javaScriptEnabled = true
            webViewClient = ReachFiveWebViewClient(codeVerifier)

            // Useful for WebView debugging
            //webChromeClient = object : WebChromeClient() {
            //
            //    override fun onConsoleMessage(message: ConsoleMessage): Boolean {
            //        Log.d(TAG, "${message.message()}")
            //        return true
            //    }
            //}

        }

        urlString?.let {
            if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
                Log.i(TAG, "Web Message Listener not supported: no passkey support in WebView.")
            } else {
                val rules = setOf("*")
                WebViewCompat.addWebMessageListener(
                    binding.webview,
                    PasskeyWebListener.INTERFACE_NAME,
                    rules,
                    passkeyListener
                )
            }

            binding.webview.loadUrl(urlString)
        }
    }

    override fun onNewIntent(newIntent: Intent) {
        super.onNewIntent(newIntent)

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

    override fun onBackPressed() {
        super.onBackPressed()

        if (binding.webview.canGoBack()) {
            binding.webview.goBack()
        } else {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    inner class ReachFiveWebViewClient(private val codeVerifier: String?) : WebViewClientCompat() {

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            request.url?.let { url ->
                // regex : (reachfive://word character: [a-zA-Z_0-9]/callback)(any character zero or more times)
                val pattern = Pattern.compile("^(reachfive:\\/\\/\\w+\\/callback)(.*)$")
                val isTargetReachFive = pattern.matcher(url.toString()).matches()

                if (isTargetReachFive) {
                    val intent = Intent()
                    intent.data = url
                    intent.putExtra(CODE_VERIFIER_KEY, codeVerifier)
                    setResult(RC_WEBLOGIN, intent)
                    finish()
                    return true
                } else return false
            }

            Log.d(TAG, "WebView: unexpected empty url.")
            finish()
            return true
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
                passkeyListener.onPageStarted()
                binding.webview.evaluateJavascript(PasskeyWebListener.INJECTED_VAL, null)
            }
        }
    }

}
