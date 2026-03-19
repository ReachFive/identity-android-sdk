package co.reachfive.identity.sdk.core

import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.webkit.WebViewClientCompat
import androidx.webkit.WebViewFeature
import co.reachfive.identity.sdk.core.ReachFive.Companion.TAG
import co.reachfive.identity.sdk.core.RedirectionActivity.Companion.CODE_VERIFIER_KEY
import co.reachfive.identity.sdk.core.RedirectionActivity.Companion.RC_WEBLOGIN
import co.reachfive.identity.sdk.core.utils.PasskeyWebListener
import java.util.regex.Pattern

open class ReachFiveWebViewClient(
    val activity: RedirectionActivity,
    val codeVerifier: String?
) : WebViewClientCompat() {

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        request.url?.let { url ->
            // regex : (reachfive://word character: [a-zA-Z_0-9]/callback)(any character zero or more times)
            val pattern = Pattern.compile("^(reachfive:\\/\\/\\w+\\/callback)(.*)$")
            val isTargetReachFive = pattern.matcher(url.toString()).matches()
            Log.d(TAG, "WebViewClient isTargetReachFive: $isTargetReachFive")

            if (isTargetReachFive) {
                val intent = Intent()
                intent.data = url
                intent.putExtra(CODE_VERIFIER_KEY, codeVerifier)
                activity.setResult(RC_WEBLOGIN, intent)
                activity.finish()
                return true
            } else return false
        }

        Log.d(TAG, "WebViewClient: unexpected empty url.")
        activity.finish()
        return true
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER) && activity.passkeyListener != null) {
            Log.d(TAG, "RedirectionActivity onPageStarted: injecting passkey boilerplate")
            activity.passkeyListener?.onPageStarted()
            view?.evaluateJavascript(PasskeyWebListener.INJECTED_VAL, null)
        } else {
            Log.d(TAG, "RedirectionActivity onPageStarted: not injecting passkey boilerplate")
        }
    }
}
