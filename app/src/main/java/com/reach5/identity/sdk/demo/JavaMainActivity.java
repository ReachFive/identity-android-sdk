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
import com.reach5.identity.sdk.core.models.Profile;
import com.reach5.identity.sdk.core.models.AuthToken;
import com.reach5.identity.sdk.core.models.SdkConfig;
import com.reach5.identity.sdk.core.models.User;
import com.reach5.identity.sdk.google.GoogleProvider;
import com.reach5.identity.sdk.webview.WebViewProvider;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.Arrays;
import java.util.Objects;

public class JavaMainActivity extends AppCompatActivity {
    private Dotenv dotenv = Dotenv.configure().directory("/assets").filename("env").load();

    private static String TAG = "Reach5_MainActivity";
    private JavaReachFive reach5;
    private ProvidersAdapter providerAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.toolbar));

        SdkConfig sdkConfig = new SdkConfig(
            Objects.requireNonNull(dotenv.get("DOMAIN")),
            Objects.requireNonNull(dotenv.get("CLIENT_ID"))
        );

        Objects.requireNonNull(getSupportActionBar()).setTitle("Java Sdk Example");

        reach5 = new JavaReachFive(
            this,
            sdkConfig,
            Arrays.asList(new GoogleProvider(), new WebViewProvider())
        );

        reach5.initialize(providers ->
            providerAdapter.refresh(providers)
        , error -> {
            Log.d(TAG, "ReachFive init " + error.getMessage());
            showToast("ReachFive init " + error.getMessage());
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

        findViewById(R.id.passwordSignup).setOnClickListener(view -> {
            reach5.signup(
                new Profile(
                    usernameEditText.getText().toString(),
                    passwordEditText.getText().toString()
                ),
                this::handleLoginSuccess,
                failure -> {
                    Log.d(TAG, "signup error=" + failure.getMessage());
                    showToast("Signup With Password Error " + failure.getMessage());
                }
            );
        });

        findViewById(R.id.passwordLogin).setOnClickListener(view -> {
            reach5.loginWithPassword(
                usernameEditText.getText().toString(),
                passwordEditText.getText().toString(),
                this::handleLoginSuccess,
                failure -> {
                    Log.d(TAG, "loginWithPassword error=" + failure.getMessage());
                    showToast("Login With Password Error " + failure.getMessage());
                }
            );
        });

    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void handleLoginSuccess(AuthToken authToken) {
        User user = authToken.getUser();
        Objects.requireNonNull(getSupportActionBar()).setTitle(user.getEmail());
        showToast("Login success " + authToken.getAccessToken());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        reach5.onActivityResult(requestCode, resultCode, data, this::handleLoginSuccess, it -> {
            Log.d(TAG, "onActivityResult error=" + it.getMessage());
            showToast("LoginProvider error=" + it.getMessage());
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_logout:
                reach5.logoutWithProviders();
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
