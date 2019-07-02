package com.reach5.identity.sdk.demo

import kotlinx.android.synthetic.main.activity_main.*
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.reach5.identity.sdk.core.ReachFive
import com.reach5.identity.sdk.core.models.Profile
import com.reach5.identity.sdk.core.models.AuthToken
import com.reach5.identity.sdk.core.models.SdkConfig
import com.reach5.identity.sdk.facebook.FacebookProvider
import com.reach5.identity.sdk.google.GoogleProvider
import com.reach5.identity.sdk.webview.WebViewProvider
import java.lang.Exception

class MainActivity : AppCompatActivity() {

    private val TAG = "Reach5_MainActivity"

    private lateinit var reach5: ReachFive

    private lateinit var authToken: AuthToken

    private lateinit var providerAdapter: ProvidersAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        val sdkConfig = SdkConfig(
            domain = "sdk-mobile-sandbox.reach5.net",
            clientId = "TYAIHFRJ2a1FGJ1T8pKD"
        )

        val providersCreators = listOf(
            GoogleProvider(),
            FacebookProvider(),
            WebViewProvider()
        )

        this.reach5 = ReachFive(
            sdkConfig = sdkConfig,
            providersCreators = providersCreators,
            activity = this
        ).initialize({ providers ->
            providerAdapter.refresh(providers)
        }, {
            Log.d(TAG, "ReachFive init ${it.message}")
            showToast("ReachFive init ${it.message}")
        })

        providerAdapter = ProvidersAdapter(applicationContext, reach5.getProviders())

        providers.adapter = providerAdapter

        providers.setOnItemClickListener { _, _, position, _ ->
            val provider = reach5.getProviders()[position]
            this.reach5.loginWithProvider(provider.name, "home", this)
        }

        passwordSignup.setOnClickListener {
            this.reach5.signup(
                Profile(
                    email = username.text.toString(),
                    password = password.text.toString()
                ),
                scope = listOf("openid", "profile", "email"),
                success = { handleLoginSuccess(it) },
                failure = {
                    Log.d(TAG, "signup error=$it")
                    showToast("Signup With Password Error ${it.message}")
                }
            )
        }

        passwordLogin.setOnClickListener {
            this.reach5.loginWithPassword(
                username = username.text.toString(),
                password = password.text.toString(),
                scope = listOf("openid", "profile", "email"),
                success = { handleLoginSuccess(it) },
                failure = {
                    Log.d(TAG, "loginWithPassword error=$it")
                    showToast("Login error=${it.message}")
                }
            )
        }
    }

    private fun handleLoginSuccess(authToken: AuthToken) {
        try {
            this.authToken = authToken
            val user = authToken.user
            Log.d(TAG, "login user= success=$authToken")
            supportActionBar?.title = user?.email
            showToast("Login success= token=${authToken.accessToken}")
        } catch (e: Exception) {
            Log.d(TAG, "Login error=$authToken")
            showToast("Login error=$authToken")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d("ReachFive", "MainActivity.onActivityResult requestCode=$requestCode resultCode=$resultCode")
        this.reach5.onActivityResult(requestCode, resultCode, data, success = {
            handleLoginSuccess(it)
        }, failure = {
            Log.d(TAG, "onActivityResult error=$it")
            it.exception?.printStackTrace()
            showToast("LoginProvider error=${it.message}")
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        Log.d("ReachFive", "MainActivity.onRequestPermissionsResult requestCode=$requestCode permissions=$permissions grantResults=$grantResults")
        reach5.onRequestPermissionsResult(requestCode, permissions, grantResults, failure = {

        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_logout -> {
                reach5.logout {
                    // TODO
                }
                true
            }
            R.id.menu_java -> {
                this.startActivity(Intent(this, JavaMainActivity::class.java))
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onStop() {
        super.onStop()
        reach5.onStop()
    }
}
