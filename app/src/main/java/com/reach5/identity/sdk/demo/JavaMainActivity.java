package com.reach5.identity.sdk.demo;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import com.reach5.identity.sdk.core.JavaReachFive;
import com.reach5.identity.sdk.core.Provider;
import com.reach5.identity.sdk.core.api.Profile;
import com.reach5.identity.sdk.core.models.OpenIdTokenResponse;
import com.reach5.identity.sdk.core.models.SdkConfig;
import com.reach5.identity.sdk.core.models.User;
import com.reach5.identity.sdk.google.GoogleProvider;
import com.reach5.identity.sdk.webview.WebViewProvider;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;

public class JavaMainActivity extends AppCompatActivity {
    private static String TAG = "Reach5_MainActivity";
    private JavaReachFive reach5;
    private ProvidersAdapter providerAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.toolbar));

        SdkConfig sdkConfig = new SdkConfig(
            "egor-sandbox.reach5.net",
            "7qasrzZQBbZLomtKPmvS"
        );

        Objects.requireNonNull(getSupportActionBar()).setTitle("Java Sdk Example");

        reach5 = new JavaReachFive(
            this,
            sdkConfig,
            Arrays.asList(new GoogleProvider(), new WebViewProvider())
        );

        reach5.init(providers ->
            providerAdapter.refresh(providers)
        , error -> {
            Log.d(TAG, "ReachFive init ${it.message}");
            showToast("ReachFive init ${it.message}");
        });

        providerAdapter = new ProvidersAdapter(getApplicationContext(), reach5.getProviders());

        ListView providers = findViewById(R.id.providers);
        providers.setAdapter(providerAdapter);

        providers.setOnItemClickListener((parent, view, position, id) -> {
            Provider provider = reach5.getProviders().get(position);
            reach5.loginWithNativeProvider(provider.getName(), "home", this);
        });

        EditText usernameEditText = findViewById(R.id.username);
        EditText passwordEditText = findViewById(R.id.password);

        String username = usernameEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        Profile profile = new Profile(username, password);

        findViewById(R.id.passwordSignup).setOnClickListener(view -> {
            reach5.signupWithPassword(profile, this::handleLoginSuccess, failure -> {
                Log.d(TAG, "signupWithPassword error=" + failure.getMessage());
                showToast("Signup With Password Error " + failure.getMessage());
            });
        });

        findViewById(R.id.passwordLogin).setOnClickListener(view -> {
            reach5.loginWithPassword(username, password, this::handleLoginSuccess, failure -> {
                Log.d(TAG, "loginWithPassword error=" + failure.getMessage());
                showToast("Login With Password Error " + failure.getMessage());
            });
        });

    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void handleLoginSuccess(OpenIdTokenResponse openIdTokenResponse) {
        User user = openIdTokenResponse.getUser(); // TODO add try
        Log.d(TAG, "loginWithPassword user=$user success=$openIdTokenResponse");
        Objects.requireNonNull(getSupportActionBar()).setTitle(user.getEmail());
        showToast("Login success=${user?.email} token=${openIdTokenResponse.accessToken}");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("ReachFive", "MainActivity.onActivityResult requestCode=$requestCode resultCode=$resultCode");
        reach5.onActivityResult(requestCode, resultCode, data, this::handleLoginSuccess, it -> {
            Log.d(TAG, "onActivityResult error=$it");
            showToast("LoginProvider error=${it.message}");
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_logout:
                reach5.logout();
                return true;
            case R.id.menu_java:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onStop() {
        super.onStop();
        reach5.onStop();
    }
}
