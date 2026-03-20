package co.reachfive.identity.sdk.core

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import co.reachfive.identity.sdk.core.ReachFive.Companion.TAG
import co.reachfive.identity.sdk.core.databinding.ReachfiveWebviewBinding
import co.reachfive.identity.sdk.core.models.ErrorCode
import co.reachfive.identity.sdk.core.utils.PasskeyWebListener


class RedirectionActivity : ComponentActivity() {
    lateinit var binding: ReachfiveWebviewBinding

    private var isCustomTabFlow = false
    private var hasCustomTabStarted = false

    var passkeyListener: PasskeyWebListener? = null

    companion object {
        var webViewClientFactory: ((RedirectionActivity, String?) -> ReachFiveWebViewClient)? = null

        const val ORIGIN_WEBAUTHN = "ORIGIN_WEBAUTHN"
        const val FQN = "co.reachfive.identity.sdk.core.RedirectionActivity"
        const val CODE_VERIFIER_KEY = "CODE_VERIFIER"
        const val URL_KEY = "URL"
        const val SCHEME = "SCHEME"
        const val USE_NATIVE_WEBVIEW = "USE_NATIVE_WEBVIEW"
        const val FULL_SCREEN_WEBVIEW = "FULL_SCREEN_WEBVIEW"

        const val USE_EPHEMERAL_BROWSING = "USE_EPHEMERAL_BROWSING"

        const val RC_WEBLOGIN = 52557

        const val RC_WEBLOGOUT = 52558

        const val PROVIDER_KEY = "PROVIDER"

        fun isLoginRequestCode(code: Int): Boolean =
            setOf(RC_WEBLOGIN).contains(code)

        fun isLogoutRequestCode(code: Int): Boolean =
            setOf(RC_WEBLOGOUT).contains(code)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val urlString = intent.getStringExtra(URL_KEY)
        val codeVerifier = intent.getStringExtra(CODE_VERIFIER_KEY)

        val useWebView = intent.getBooleanExtra(USE_NATIVE_WEBVIEW, false)
        val fullScreenWebView = intent.getBooleanExtra(FULL_SCREEN_WEBVIEW, false)
        val originWebAuthn = intent.getStringExtra(ORIGIN_WEBAUTHN)

        val provider = intent.getStringExtra(PROVIDER_KEY)

        val useEphemeralBrowsing = intent.getBooleanExtra(USE_EPHEMERAL_BROWSING, false)

        if (provider == "google") {
            setResult(RESULT_OK)
            finish()
        } else if (urlString == null) {
            Log.d(TAG, "RedirectionActivity: no URL")
            finish()
        } else if (useWebView)
            launchWebView(codeVerifier, urlString, originWebAuthn, fullScreenWebView)
        else
            launchCustomTab(urlString, codeVerifier, useEphemeralBrowsing)
    }

    private fun launchCustomTab(urlString: String?, codeVerifier: String?, useEphemeralBrowsing: Boolean) {
        Log.d(TAG, "RedirectionActivity launchCustomTab url: $urlString")

        isCustomTabFlow = true

        val customTabsIntent = CustomTabsIntent.Builder().setEphemeralBrowsingEnabled(useEphemeralBrowsing).build().intent
        customTabsIntent.data = Uri.parse(urlString)
        customTabsIntent.putExtra(CODE_VERIFIER_KEY, codeVerifier)

        try {
            startActivity(customTabsIntent)
        } catch(e: Exception) {
            Log.e(TAG, "RedirectionActivity launchCustomTab error: ${e.message}")
            setResult(ErrorCode.CustomTabUnavailable.code)
            finish()
        }
    }

    fun createReachFiveWebViewClient(codeVerifier: String?): ReachFiveWebViewClient {
        return webViewClientFactory?.invoke(this, codeVerifier) ?: ReachFiveWebViewClient(
            this,
            codeVerifier
        )
    }

    private fun launchWebView(codeVerifier: String?, urlString: String?, originWebAuthn: String?, fullScreenWebView: Boolean) {
        Log.d(TAG, "RedirectionActivity launchWebView url: $urlString")

        binding = ReachfiveWebviewBinding.inflate(layoutInflater)

        setContentView(binding.root)

        if (!fullScreenWebView) {
            ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
                val systemBars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            or WindowInsetsCompat.Type.ime())
                view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                WindowInsetsCompat.CONSUMED
            }
        }

        binding.webview.apply {
            @SuppressLint("SetJavaScriptEnabled")
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = createReachFiveWebViewClient(codeVerifier)
        }

        urlString?.let {
            if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
                Log.i(TAG, "Web Message Listener not supported: no passkey support in WebView.")
            } else if (originWebAuthn == null) {
                Log.i(TAG, "WebView: webauthn origin not configured")
            } else {
                passkeyListener = PasskeyWebListener(originWebAuthn, this, lifecycleScope)

                val rules = setOf("*")
                WebViewCompat.addWebMessageListener(
                    binding.webview,
                    PasskeyWebListener.INTERFACE_NAME,
                    rules,
                    passkeyListener!!
                )
            }

            binding.webview.loadUrl(urlString)
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "RedirectionActivity onResume hasCustomTabStarted: $hasCustomTabStarted ; isCustomTabFlow: $isCustomTabFlow")

        // When Custom Tab returns, the Redirection Activity resumes and we need to end it.
        if (isCustomTabFlow && !hasCustomTabStarted) {
            hasCustomTabStarted = true
        } else if (isCustomTabFlow && hasCustomTabStarted) {
            isCustomTabFlow = false
            hasCustomTabStarted = false
            finish()
        }
    }

    override fun onNewIntent(newIntent: Intent) {
        super.onNewIntent(newIntent)

        // remove any flags from the new intent
        newIntent.flags = 0

        val intentClass = newIntent.resolveActivity(packageManager).className

        val scheme = intent.getStringExtra(SCHEME)
        val url = newIntent.data
        Log.d(TAG, "RedirectionActivity onNewIntent\nurl: $url\nscheme: $scheme")

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

        if (::binding.isInitialized && binding.webview.canGoBack()) {
            Log.d(TAG, "RedirectionActivity onBackPressed go back")
            binding.webview.goBack()
        } else {
            Log.d(TAG, "RedirectionActivity onBackPressed cancel")

            setResult(RESULT_CANCELED)
            finish()
        }
    }

}
