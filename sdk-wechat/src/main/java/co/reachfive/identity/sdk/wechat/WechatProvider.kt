package co.reachfive.identity.sdk.wechat

import android.app.Activity
import android.content.Context
import android.content.Intent
import co.reachfive.identity.sdk.core.Provider
import co.reachfive.identity.sdk.core.ProviderCreator
import co.reachfive.identity.sdk.core.SessionUtilsClient
import co.reachfive.identity.sdk.core.models.AuthToken
import co.reachfive.identity.sdk.core.models.ProviderConfig
import co.reachfive.identity.sdk.core.models.ReachFiveError
import co.reachfive.identity.sdk.core.utils.Failure
import co.reachfive.identity.sdk.core.utils.Success
import co.reachfive.identity.sdk.wechat.WechatProvider.Companion.REQUEST_CODE
import com.tencent.mm.opensdk.modelmsg.SendAuth

class WechatProvider : ProviderCreator {
    companion object {
        const val NAME = "wechat"
        const val REQUEST_CODE = 160794467
    }

    override val name: String = NAME
    override fun create(
        providerConfig: ProviderConfig,
        sessionUtils: SessionUtilsClient,
        context: Context,
    ): Provider {
        return ConfiguredWechatProvider(providerConfig, sessionUtils, context)
    }
}

internal class ConfiguredWechatProvider(
    private val providerConfig: ProviderConfig,
    private val sessionUtils: SessionUtilsClient,
    private val context: Context
) : Provider {
    override val name: String = WechatProvider.NAME
    override val requestCode: Int = REQUEST_CODE
    private lateinit var origin: String
    private lateinit var scope: Collection<String>


    override fun login(origin: String, scope: Collection<String>, activity: Activity) {

        this.origin = origin

        this.scope = scope
        val intent = Intent(context, WeChatLoginActivity::class.java)
        intent.putExtra("APP_ID", providerConfig.clientId)
        activity.startActivityForResult(intent, REQUEST_CODE)

    }


    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        success: Success<AuthToken>,
        failure: Failure<ReachFiveError>
    ) {
        val errorCode = SendAuth.Resp().errCode
        if (errorCode == 0 && data?.getStringExtra("token") != null && data?.getStringExtra("errorStr") == null) {
            sessionUtils.loginWithProvider(
                "wechat:android",
                authCode = data?.getStringExtra("token"),
                origin,
                scope = scope,
                success = success,
                failure = failure
            )
        } else {
            failure(ReachFiveError.from(data?.getStringExtra("errorStr") ?: "No code delivered by WeChat"))
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
        failure: Failure<ReachFiveError>,
        activity: Activity
    ) {
        // Do nothing
    }

}
