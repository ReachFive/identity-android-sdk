package co.reachfive.identity.sdk.wechat;

import android.content.Intent;
import android.os.Bundle;


import androidx.appcompat.app.AppCompatActivity;

import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import co.reachfive.identity.sdk.wechat.share.WXEntryActivity;


public class WeChatLoginActivity extends AppCompatActivity {

    private IWXAPI api;
    private String appId;

    private static boolean firstCallToResume = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        appId = getIntent().getStringExtra("APP_ID");
        api = WXAPIFactory.createWXAPI(this, appId , true);
        api.registerApp(appId);
        SendAuth.Req req = new SendAuth.Req();
        req.scope = "snsapi_userinfo";
        req.state = "none";
        req.nonAutomatic = true;
        req.isOption1 = true;
        api.sendReq(req);
    }

    public void onResume(){
        super.onResume();

        if(WXEntryActivity.endedFlow){
            Intent intent = new Intent().putExtra("token", WXEntryActivity.token);
            setResult(RESULT_OK, intent);
            api.unregisterApp();
            finish();
        }
        // When the activity is created, onResume is called a first time before the user interacts
        // with the authorization consent page. The condition on firstCallToResume prevents the
        // activity to finish before the user had a change to interact with the consents page,
        if(firstCallToResume)
            finish();
        firstCallToResume = true;
    }
}