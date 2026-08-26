package com.reach5.identity.sdk.demo

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.reach5.identity.sdk.core.ReachFive
import com.reach5.identity.sdk.core.models.requests.webAuthn.WebAuthnLoginRequest
import kotlinx.android.synthetic.main.webauthn_login.*

// The activity controlling the view
class MainActivity : AppCompatActivity() {
    // The ReachFive client
    private lateinit var reach5: ReachFive

    // The scope assigned to the user
    private val scope = setOf("openid", "email", "profile", "phone_number", "offline_access", "events", "full_write")

    companion object {
        // The domain of the origin call (it can be stored in an env file)
        const val ORIGIN = "https://dev-sandbox-268508.web.app"
        // The code of the FIDO2 authentication request
        const val WEBAUTHN_LOGIN_REQUEST_CODE = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Define the action on the click of the "Login" button
        loginWithWebAuthn.setOnClickListener {
            val email: String = webAuthnEmail.text.toString()

            // Build the request according to the identifier provided by the user
            val webAuthnLoginRequest: WebAuthnLoginRequest = if (email.isNotEmpty()) {
                WebAuthnLoginRequest.EmailWebAuthnLoginRequest(
                    origin = ORIGIN,
                    email = email,
                    scope = scope
                )
            } else {
                WebAuthnLoginRequest.PhoneNumberWebAuthnLoginRequest(
                    origin = ORIGIN,
                    phoneNumber = webAuthnPhoneNumber.text.toString(),
                    scope = scope
                )
            }

            // Launch a pending FIDO2 task for authentication
            this.reach5.loginWithWebAuthn(
                loginRequest = webAuthnLoginRequest,
                loginRequestCode = WEBAUTHN_LOGIN_REQUEST_CODE,
                failure = {
                    Log.d("ReachFive","Unable to login with FIDO2: $it")
                }
            )
        }
    }
}