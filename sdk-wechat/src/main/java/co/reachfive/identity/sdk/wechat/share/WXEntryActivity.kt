package co.reachfive.identity.sdk.wechat.share

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
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
                Toast.makeText(this, "Exception while parsing token", Toast.LENGTH_LONG).show()
            }

            BaseResp.ErrCode.ERR_USER_CANCEL -> Toast.makeText(
                this,
                "User canceled the request",
                Toast.LENGTH_LONG
            ).show()

            BaseResp.ErrCode.ERR_AUTH_DENIED -> Toast.makeText(
                this,
                "User denied the request",
                Toast.LENGTH_LONG
            ).show()
        }
        endedFlow = true
        finish()
    }

    companion object {
        var token: String? = null
        var endedFlow = false
    }
}