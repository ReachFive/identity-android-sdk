package com.reach5.identity.sdk.demo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.reach5.identity.sdk.core.ReachFive
import com.reach5.identity.sdk.core.RedirectionActivity.Companion.REDIRECTION_REQUEST_CODE
import com.reach5.identity.sdk.core.models.ReachFiveError
import com.reach5.identity.sdk.core.models.SdkConfig
import com.reach5.identity.sdk.core.models.requests.ProfileSignupRequest
import com.reach5.identity.sdk.core.models.requests.ProfileWebAuthnSignupRequest
import com.reach5.identity.sdk.core.models.requests.webAuthn.WebAuthnLoginRequest
import com.reach5.identity.sdk.core.models.AuthToken
import com.reach5.identity.sdk.demo.AuthenticatedActivity.Companion.AUTH_TOKEN
import com.reach5.identity.sdk.demo.AuthenticatedActivity.Companion.SDK_CONFIG
import com.reach5.identity.sdk.facebook.FacebookProvider
import com.reach5.identity.sdk.google.GoogleProvider
import com.reach5.identity.sdk.webview.WebViewProvider
import io.github.cdimascio.dotenv.dotenv
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.email
import kotlinx.android.synthetic.main.activity_main.phoneNumber
import kotlinx.android.synthetic.main.callback_login.*
import kotlinx.android.synthetic.main.webauthn_login.*
import kotlinx.android.synthetic.main.webauthn_signup.*

class MainActivity : AppCompatActivity() {

    private val TAG = "Reach5_MainActivity"

    private val dotenv = dotenv {
        directory = "/assets"
        filename = "env"
    }
    private val domain =
        dotenv["DOMAIN"]
            ?: throw IllegalArgumentException("The ReachFive domain is undefined! Check your `env` file.")
    private val clientId =
        dotenv["CLIENT_ID"]
            ?: throw IllegalArgumentException("The ReachFive client ID is undefined! Check your `env` file.")
    private val scheme =
        dotenv["SCHEME"]
            ?: throw IllegalArgumentException("The ReachFive redirect URI is undefined! Check your `env` file.")

    // This variable is only mandatory for the FIDO2 login flow
    private val origin = dotenv["ORIGIN"] ?: ""

    private val sdkConfig = SdkConfig(domain, clientId, scheme)

    private val assignedScope = setOf(
        "openid",
        "email",
        "profile",
        "phone_number",
        "offline_access",
        "events",
        "full_write"
    )

    private lateinit var reach5: ReachFive

    private lateinit var providerAdapter: ProvidersAdapter

    private lateinit var webAuthnId: String

    companion object {
        const val WEBAUTHN_LOGIN_REQUEST_CODE = 2
        const val WEBAUTHN_SIGNUP_REQUEST_CODE = 3
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

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
            this.reach5.loginWithProvider(
                name = provider.name,
                origin = "home",
                scope = assignedScope,
                activity = this
            )
        }

        passwordSignup.setOnClickListener {
            val signupRequest = when {
                ((email.text.toString().isNotEmpty()) &&(phoneNumber.text.toString().isEmpty()) ) -> ProfileSignupRequest (
                        email = email.text.toString(),
                        password = password.text.toString()
                )
                ((email.text.toString().isEmpty()) &&(phoneNumber.text.toString().isNotEmpty()) ) -> ProfileSignupRequest(
                        phoneNumber = phoneNumber.text.toString(),
                        password = password.text.toString()
                )

                else ->
                    ProfileSignupRequest(
                            email = email.text.toString(),
                            phoneNumber = phoneNumber.text.toString(),
                            password = password.text.toString()
                    )
            }

            this.reach5.signup(
                profile = signupRequest,
                redirectUrl = redirectUrl.text.toString().ifEmpty { null },
                success = { handleLoginSuccess(it) },
                failure = {
                    Log.d(TAG, "signup error=$it")
                    showErrorToast(it)
                }
            )
        }

        passwordLogin.setOnClickListener {
            this.reach5.loginWithPassword(
                username = email.text.trim().toString()
                    .ifEmpty { phoneNumber.text.trim().toString() },
                password = password.text.trim().toString(),
                success = { handleLoginSuccess(it) },
                failure = {
                    Log.d(TAG, "loginWithPassword error=$it")
                    showErrorToast(it)
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
                            showErrorToast(it)
                        }
                    )
                } else {
                    this.reach5.startPasswordless(
                        email = email.text.toString(),
                        successWithNoContent = { showToast("Email sent - Check your email box") },
                        failure = {
                            Log.d(TAG, "signup error=$it")
                            showErrorToast(it)
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
                            showErrorToast(it)
                        }
                    )
                } else {
                    this.reach5.startPasswordless(
                        phoneNumber = phoneNumber.text.toString(),
                        successWithNoContent = { showToast("Sms sent - Please enter the validation code below") },
                        failure = {
                            Log.d(TAG, "signup error=$it")
                            showErrorToast(it)
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
                    showErrorToast(it)
                }
            )
        }

        signupWithWebAuthn.setOnClickListener {
            this.reach5
                .signupWithWebAuthn(
                    profile = ProfileWebAuthnSignupRequest(
                        email = signupWebAuthnEmail.text.toString(),
                        givenName = signupWebAuthnGivenName.text.toString(),
                        familyName = signupWebAuthnFamilyName.text.toString()
                    ),
                    origin = origin,
                    friendlyName = signupWebAuthnNewFriendlyName.text.toString(),
                    signupRequestCode = WEBAUTHN_SIGNUP_REQUEST_CODE,
                    successWithWebAuthnId = { this.webAuthnId = it },
                    failure = {
                        Log.d(TAG, "signupWithWebAuthn error=$it")
                        showErrorToast(it)
                    }
                )
        }

        loginWithWebAuthn.setOnClickListener {
            val email = webAuthnEmail.text.toString()
            val webAuthnLoginRequest: WebAuthnLoginRequest =
                if (email.isNotEmpty())
                    WebAuthnLoginRequest.EmailWebAuthnLoginRequest(origin, email, assignedScope)
                else
                    WebAuthnLoginRequest.PhoneNumberWebAuthnLoginRequest(
                        origin,
                        webAuthnPhoneNumber.text.toString(),
                        assignedScope
                    )

            this.reach5
                .loginWithWebAuthn(
                    loginRequest = webAuthnLoginRequest,
                    loginRequestCode = WEBAUTHN_LOGIN_REQUEST_CODE,
                    failure = {
                        Log.d(TAG, "loginWithWebAuthn error=$it")
                        showErrorToast(it)
                    }
                )
        }

        loginWithCallback.setOnClickListener {
            reach5.loginCallback(
                tkn = tkn.text.toString(),
                scope = assignedScope
            )
        }

        val authorizationCode: String? = intent?.data?.getQueryParameter("code")
        if (authorizationCode != null) {
            this.reach5.exchangeCodeForToken(
                authorizationCode,
                success = { handleLoginSuccess(it) },
                failure = {
                    Log.d(TAG, "loginWithPassword error=$it")
                    showErrorToast(it)
                }
            )
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "MainActivity.onActivityResult requestCode=$requestCode resultCode=$resultCode")

        when (requestCode) {
            WEBAUTHN_LOGIN_REQUEST_CODE -> {
                when (resultCode) {
                    RESULT_OK -> {
                        if (data == null) Log.d(TAG, "The data is null")
                        else handleWebAuthnLoginResponse(data)
                    }
                    RESULT_CANCELED -> Log.d(TAG, "Operation is cancelled")
                    else -> Log.e(TAG, "Operation failed, with resultCode: $resultCode")
                }
            }

            WEBAUTHN_SIGNUP_REQUEST_CODE -> {
                when (resultCode) {
                    RESULT_OK -> {
                        if (data == null) Log.d(TAG, "The data is null")
                        else handleWebAuthnSignupResponse(data)
                    }
                    RESULT_CANCELED -> Log.d(TAG, "Operation is cancelled")
                    else -> Log.e(TAG, "Operation failed, with resultCode: $resultCode")
                }
            }

            REDIRECTION_REQUEST_CODE -> {
                if (data == null) Log.d(TAG, "The data is null")
                else handleLoginCallbackResponse(data, resultCode)
            }

            // Handle webview and native provider login
            else -> {
                this.reach5.onActivityResult(
                    requestCode = requestCode,
                    resultCode = resultCode,
                    data = data,
                    success = { authToken -> handleLoginSuccess(authToken) },
                    failure = { error ->
                        Log.d(TAG, "onActivityResult error=$error")
                        error.exception?.printStackTrace()
                        showErrorToast(error)
                    }
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        Log.d(
            TAG,
            "MainActivity.onRequestPermissionsResult requestCode=$requestCode permissions=$permissions grantResults=$grantResults"
        )
        reach5.onRequestPermissionsResult(requestCode, permissions, grantResults, failure = {})
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

    private fun handleLoginSuccess(authToken: AuthToken) {
        try {
            val intent = Intent(this, AuthenticatedActivity::class.java)
            intent.putExtra(AUTH_TOKEN, authToken)
            intent.putExtra(SDK_CONFIG, sdkConfig)

            startActivity(intent)
        } catch (e: Exception) {
            Log.d(TAG, "Login error=$authToken")
            showToast("Login error=$authToken")
        }
    }

    private fun handleWebAuthnLoginResponse(intent: Intent) {
        reach5.onLoginWithWebAuthnResult(
            intent = intent,
            failure = {
                Log.d(TAG, "onLoginWithWebAuthnResult error=$it")
                showErrorToast(it)
            }
        )
    }

    private fun handleWebAuthnSignupResponse(intent: Intent) {
        reach5.onSignupWithWebAuthnResult(
            intent = intent,
            webAuthnId = this.webAuthnId,
            scope = assignedScope,
            failure = {
                Log.d(TAG, "onSignupWithWebAuthnResult error=$it")
                showErrorToast(it)
            }
        )
    }

    private fun handleLoginCallbackResponse(intent: Intent, resultCode: Int) {
        reach5.onLoginCallbackResult(
            intent,
            resultCode = resultCode,
            success = { handleLoginSuccess(it) },
            failure = {
                Log.d(TAG, "onLoginWithCallbackResult error=$it")
                showErrorToast(it)
            }
        )
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun showErrorToast(error: ReachFiveError) {
        showToast(
            error.data?.errorUserMsg ?: (error.data?.errorDetails?.get(0)?.message
                ?: (error.data?.errorDescription
                    ?: error.message))
        )
    }
}
