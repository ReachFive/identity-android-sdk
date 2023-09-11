package co.reachfive.identity.sdk.wechat.share;


import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;


public class WXEntryActivity extends AppCompatActivity implements IWXAPIEventHandler {
    public static String token;

    public static boolean endedFlow = false;
    private IWXAPI api;

    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        endedFlow = false;
        api = WXAPIFactory.createWXAPI(this, null , true);
        api.handleIntent(getIntent(), this);

        finish();
    }


    @Override
    public void onReq(BaseReq req) {
        // Never called
    }

    @Override
    public void onResp(BaseResp resp) {
        switch (resp.errCode) {
            case BaseResp.ErrCode.ERR_OK:
                try {
                    SendAuth.Resp sendResp = (SendAuth.Resp) resp;
                    WXEntryActivity.token = sendResp.code;
                } catch(Exception e){
                    Toast.makeText(this, "Exception while parsing token", Toast.LENGTH_LONG).show();
                }
                break;
            case BaseResp.ErrCode.ERR_USER_CANCEL:
                Toast.makeText(this, "User canceled the request", Toast.LENGTH_LONG).show();
                break;
            case BaseResp.ErrCode.ERR_AUTH_DENIED:
                Toast.makeText(this, "User denied the request", Toast.LENGTH_LONG).show();
                break;
        }
        endedFlow = true;
        finish();
    }
}