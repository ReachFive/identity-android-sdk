package com.reach5.identity.sdk.demo

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.reach5.identity.sdk.core.ReachFive
import com.reach5.identity.sdk.core.models.requests.ProfileWebAuthnSignupRequest
import kotlinx.android.synthetic.main.webauthn_signup.*

// The activity controlling the view
class MainActivity : AppCompatActivity() {
    // The ReachFive client
    private lateinit var reach5: ReachFive

    // The WebAuthn identifier of the user
    private lateinit var webAuthnId: String

    companion object {
        // The domain of the origin call (it can be stored in an env file)
        const val ORIGIN = "https://dev-sandbox-268508.web.app"
        // The code of the FIDO2 signup request
        const val WEBAUTHN_SIGNUP_REQUEST_CODE = 3
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Define the action on the click of the "Signup" button
        signupWithWebAuthn.setOnClickListener {
            // Launch a pending FIDO2 task for signup
            this.reach5.signupWithWebAuthn(
                profile = ProfileWebAuthnSignupRequest(
                    email = signupWebAuthnEmail.text.toString(),
                    givenName = signupWebAuthnGivenName.text.toString(),
                    familyName = signupWebAuthnFamilyName.text.toString()
                ),
                origin = ORIGIN,
                friendlyName = signupWebAuthnNewFriendlyName.text.toString(),
                signupRequestCode = WEBAUTHN_SIGNUP_REQUEST_CODE,
                successWithWebAuthnId = { this.webAuthnId = it },
                failure = {
                    Log.d("ReachFive","Unable to signup with FIDO2: $it")
                }
            )
        }
    }