package co.reachfive.identity.sdk.wechat.share

import android.app.Activity
import android.os.Bundle
import com.tencent.mm.opensdk.modelbase.BaseReq
import com.tencent.mm.opensdk.modelbase.BaseResp
import com.tencent.mm.opensdk.modelmsg.SendAuth
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler
import com.tencent.mm.opensdk.openapi.WXAPIFactory

class WXEntryActivity : Activity(), IWXAPIEventHandler {
    private lateinit var api: IWXAPI

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        endedFlow = false
        errorStr = null
        api = WXAPIFactory.createWXAPI(this, null, true)
        api.handleIntent(intent, this)
        finish()
    }

    override fun onReq(req: BaseReq) {
        // Never called
    }

    override fun onResp(resp: BaseResp) {
        when (resp.errCode) {
            BaseResp.ErrCode.ERR_OK -> try {
                val sendResp = resp as SendAuth.Resp
                token = sendResp.code
            } catch (e: Exception) {
                errorStr = "Exception while parsing token"
            }
            BaseResp.ErrCode.ERR_USER_CANCEL ->
                errorStr = "User canceled the request"

            BaseResp.ErrCode.ERR_AUTH_DENIED ->
                errorStr = "User denied the request"
        }
        endedFlow = true
        finish()
    }

    companion object {
        var token: String? = null
        var endedFlow = false
        var errorStr: String? = null
    }
}