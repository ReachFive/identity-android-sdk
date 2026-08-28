package com.reach5.identity.sdk.demo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.reach5.identity.sdk.core.ReachFive
import com.reach5.identity.sdk.core.models.responses.AuthToken
import kotlinx.android.synthetic.main.webauthn_devices.*

// The activity controlling the view
class AuthenticatedActivity : AppCompatActivity() {
    // The ReachFive client
    private lateinit var reach5: ReachFive

    // The authentication token retrieved after the user has logged-in
    private lateinit var authToken: AuthToken

    companion object {
        // The domain of the origin call (it can be stored in an env file)
        const val ORIGIN = "https://dev-sandbox-268508.web.app"
        // The code of the FIDO2 registration request
        const val WEBAUTHN_REGISTER_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_authenticated)

        // Fetch the authentication token from another activity
        this.authToken = intent.getParcelableExtra("AUTH_TOKEN")

        // Define the action on the click of the "Add new FIDO2 device" button
        addNewDevice.setOnClickListener {
            // Launch a pending FIDO2 task for registration
            this.reach5.addNewWebAuthnDevice(
                authToken = this.authToken,
                origin = ORIGIN,
                friendlyName = newFriendlyName.text.trim().toString(),
                registerRequestCode = WEBAUTHN_REGISTER_REQUEST_CODE,
                failure = { 
                    Log.d("ReachFive", "Unable to add a new FIDO2 device: $it") 
                }
            )
        }
    }
}