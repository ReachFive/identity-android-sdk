package co.reachfive.identity.sdk.wechat

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import co.reachfive.identity.sdk.wechat.share.WXEntryActivity
import com.tencent.mm.opensdk.modelmsg.SendAuth
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory

class WeChatLoginActivity : Activity() {
    private lateinit var api: IWXAPI
    private var appId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appId = intent.getStringExtra("APP_ID")
        api = WXAPIFactory.createWXAPI(this, appId, true)

        api.registerApp(appId)
        val req = SendAuth.Req()
        req.scope = "snsapi_userinfo"
        req.state = "none"
        req.nonAutomatic = true
        api.sendReq(req)
    }

    public override fun onResume() {
        super.onResume()
        if (WXEntryActivity.endedFlow) {
            val intent = Intent().putExtra("token", WXEntryActivity.token)
            setResult(RESULT_OK, intent)
            api!!.unregisterApp()
            finish()
        }
        // When the activity is created, onResume is called a first time before the user interacts
        // with the authorization consent page. The condition on firstCallToResume prevents the
        // activity to finish before the user had a change to interact with the consents page,
        if (firstCallToResume) finish()
        firstCallToResume = true
    }

    companion object {
        private var firstCallToResume = false
    }
}