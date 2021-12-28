package co.reachfive.identity.sdk.webview

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import co.reachfive.identity.sdk.core.utils.PkceAuthCodeFlow
import co.reachfive.identity.sdk.webview.databinding.ReachfiveLoginActivityBinding
import java.util.regex.Pattern

class ReachFiveLoginActivity : Activity() {
    private val TAG = "Reach5"

    private lateinit var binding: ReachfiveLoginActivityBinding
    private var code: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.reachfive_login_activity)
        binding = ReachfiveLoginActivityBinding.inflate(layoutInflater)

        val webview = binding.webview.apply {
            @SuppressLint("SetJavaScriptEnabled")
            settings.javaScriptEnabled = true
            webViewClient = ReachFiveWebViewClient()
            // Google does not allow default implementations of WebView to be used, so we need to differentiate the WebView by looking for the wv field
            settings.setUserAgentString(
                getSettings().getUserAgentString().replace("wv", getString(R.string.app_name))
            );
        }

        val config = intent.getParcelableExtra<WebViewProviderConfig>(ConfiguredWebViewProvider.BUNDLE_ID)
        val pkce = intent.getParcelableExtra<PkceAuthCodeFlow>(ConfiguredWebViewProvider.PKCE)
        val url: String? = config?.run {
            pkce?.let {
                buildUrl(it)
            }
        }

        Log.d(TAG, "ReachFiveLoginActivity onCreate : $url")

        url?.let { webview.loadUrl(it) }
    }

    override fun onBackPressed() {
        if (binding.webview.canGoBack()) {
            binding.webview.goBack()
        } else {
            loginFailure("User aborted login!")
        }
    }

    private fun loginFailure(message: String) {
        val intent = Intent()
        intent.putExtra(
            ConfiguredWebViewProvider.RESULT_INTENT_ERROR,
            message
        )
        setResult(ConfiguredWebViewProvider.PROVIDER_REDIRECTION_REQUEST_CODE, intent)
        finish()
    }

    fun loginSuccess(authCode: String?) {
        intent.putExtra(ConfiguredWebViewProvider.AuthCode, authCode)
        setResult(ConfiguredWebViewProvider.PROVIDER_REDIRECTION_REQUEST_CODE, intent)
        finish()
    }

    fun hideLoader() {
        binding.progress.visibility = View.INVISIBLE
        binding.webview.visibility = View.VISIBLE
    }

    inner class ReachFiveWebViewClient : WebViewClient() {

        // regex : (reachfive://word character: [a-zA-Z_0-9]/callback)(any character zero or more times)
        private val PATTERN = Pattern.compile("^(reachfive:\\/\\/\\w+\\/callback)(.*)$")

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            if (url != null) {
                handleUrlLoading(url)
            }
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            val url = request?.url.toString()
            val isHandled = handleUrlLoading(url)
            return if (isHandled) {
                false
            } else {
                super.shouldOverrideUrlLoading(view, request)
            }
        }

        // TODO compatibility reason
        @Suppress("DEPRECATION")
        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            return if (url != null) {
                val isHandled = handleUrlLoading(url)
                if (isHandled) {
                    false
                } else {
                    super.shouldOverrideUrlLoading(view, url)
                }
            } else true
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            hideLoader()
            super.onPageFinished(view, url)
        }

        private fun handleUrlLoading(url: String): Boolean {
            val matcher = PATTERN.matcher(url)
            return if (matcher.matches()) {
                // avoid multiple calls
                if (code == null) {
                    code = Uri.parse(url).getQueryParameter("code")
                    loginSuccess(code)
                }
                false
            } else {
                true
            }
        }
    }
}
