package com.reach5.identity.sdk.demo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.reach5.identity.sdk.core.ReachFive
import com.reach5.identity.sdk.core.models.AuthToken
import com.reach5.identity.sdk.core.models.SdkConfig
import com.reach5.identity.sdk.core.models.requests.ProfileSignupRequest
import com.reach5.identity.sdk.demo.AuthenticatedActivity.Companion.AUTH_TOKEN
import com.reach5.identity.sdk.facebook.FacebookProvider
import com.reach5.identity.sdk.google.GoogleProvider
import com.reach5.identity.sdk.webview.WebViewProvider
import io.github.cdimascio.dotenv.dotenv
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val TAG = "Reach5_MainActivity"

    private val dotenv = dotenv {
        directory = "/assets"
        filename = "env"
    }
    private val domain =
        dotenv["DOMAIN"] ?: throw IllegalArgumentException("The ReachFive domain is undefined! Check your `env` file.")
    private val clientId =
        dotenv["CLIENT_ID"] ?: throw IllegalArgumentException("The ReachFive client ID is undefined! Check your `env` file.")
    private val scheme =
        dotenv["SCHEME"] ?: throw IllegalArgumentException("The ReachFive redirect URI is undefined! Check your `env` file.")

    private lateinit var reach5: ReachFive

    private lateinit var providerAdapter: ProvidersAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        val sdkConfig = SdkConfig(domain, clientId, scheme)

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
            val scope = setOf("openid", "email", "profile", "phone_number", "offline_access", "events", "full_write")
            this.reach5.loginWithProvider(name = provider.name, origin = "home", scope = scope, activity = this)
        }

        passwordSignup.setOnClickListener {
            val signupRequest = if (email.text.toString().isNotEmpty()) {
                ProfileSignupRequest(
                    email = email.text.toString(),
                    password = password.text.toString()
                )
            } else {
                ProfileSignupRequest(
                    phoneNumber = phoneNumber.text.toString(),
                    password = password.text.toString()
                )
            }

            this.reach5.signup(
                signupRequest,
                success = { handleLoginSuccess(it) },
                failure = {
                    Log.d(TAG, "signup error=$it")
                    showToast("Signup With Password Error ${it.message}")
                }
            )
        }

        passwordLogin.setOnClickListener {
            this.reach5.loginWithPassword(
                username = email.text.toString().ifEmpty { phoneNumber.text.toString() },
                password = password.text.toString(),
                success = { handleLoginSuccess(it) },
                failure = {
                    Log.d(TAG, "loginWithPassword error=$it")
                    showToast("Login error=${it.message}")
                }
            )
        }

        startPasswordless.setOnClickListener {

            val redirectUri = redirectUriInput.text.toString()

            if (email.text.toString().isNotEmpty()) {
                if (redirectUri != "") {
                    this.reach5.startPasswordless(
                        email = email.text.toString(),
                        redirectUrl = redirectUri,
                        successWithNoContent = { showToast("Email sent - Check your email box") },
                        failure = {
                            Log.d(TAG, "signup error=$it")
                            showToast("Start passwordless with email Error ${it.message}")
                        }
                    )
                } else {
                    this.reach5.startPasswordless(
                        email = email.text.toString(),
                        successWithNoContent = { showToast("Email sent - Check your email box") },
                        failure = {
                            Log.d(TAG, "signup error=$it")
                            showToast("Start passwordless with email Error ${it.message}")
                        }
                    )
                }
            } else {
                if (redirectUri != "") {
                    this.reach5.startPasswordless(
                        phoneNumber = phoneNumber.text.toString(),
                        redirectUrl = redirectUri,
                        successWithNoContent = { showToast("Sms sent - Please enter the validation code below") },
                        failure = {
                            Log.d(TAG, "signup error=$it")
                            showToast("Start passwordless with sms Error ${it.message}")
                        }
                    )
                } else {
                    this.reach5.startPasswordless(
                        phoneNumber = phoneNumber.text.toString(),
                        successWithNoContent = { showToast("Sms sent - Please enter the validation code below") },
                        failure = {
                            Log.d(TAG, "signup error=$it")
                            showToast("Start passwordless with sms Error ${it.message}")
                        }
                    )
                }
            }
        }

        phoneNumberPasswordless.setOnClickListener {
            this.reach5.verifyPasswordless(
                phoneNumber = phoneNumber.text.toString(),
                verificationCode = verificationCode.text.toString(),
                success = { handleLoginSuccess(it) },
                failure = {
                    Log.d(TAG, "verifyPasswordless error=$it")
                    showToast("Login error=${it.message}")
                }
            )
        }

        val authorizationCode: String? = intent?.data?.getQueryParameter("code")
        if (authorizationCode != null) {
            this.reach5.exchangeCodeForToken(
                authorizationCode,
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
            val intent = Intent(this, AuthenticatedActivity::class.java)
            intent.putExtra(AUTH_TOKEN, authToken)

            startActivity(intent)
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        Log.d(
            "ReachFive",
            "MainActivity.onRequestPermissionsResult requestCode=$requestCode permissions=$permissions grantResults=$grantResults"
        )
        reach5.onRequestPermissionsResult(requestCode, permissions, grantResults, failure = {

        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
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
        menu?.findItem(R.id.menu_logout)?.isVisible = false
        return super.onCreateOptionsMenu(menu)
    }

    override fun onStop() {
        super.onStop()
        reach5.onStop()
    }
}
