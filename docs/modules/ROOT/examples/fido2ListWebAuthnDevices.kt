package com.reach5.identity.sdk.demo

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.reach5.identity.sdk.core.ReachFive
import com.reach5.identity.sdk.core.models.responses.AuthToken
import com.reach5.identity.sdk.core.models.responses.webAuthn.DeviceCredential

// The activity controlling the view
class AuthenticatedActivity : AppCompatActivity() {
    // The ReachFive client already instantiated in another activity
    private lateinit var reach5: ReachFive
    // The authentication token retrieved after the user has logged-in
    private lateinit var authToken: AuthToken
    // The list of the FIDO2 devices of the user
    private lateinit var devicesDisplayed: List<DeviceCredential>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_authenticated)
        
        /*...*/
        
        // Fetch the authentication token from another activity
        this.authToken = intent.getParcelableExtra("AUTH_TOKEN")
        // Initialize the list of devices
        this.devicesDisplayed = listOf()
        
        // Fetch the registered devices of the user from Reachfive server
        this.reach5.listWebAuthnDevices(
            authToken = authToken,
            success = {
                // Refresh the list of displayed devices
                this.devicesDisplayed = it
            },
            failure = {
                Log.d("ReachFive","Unable to list the FIDO2 devices: $it")
            }
        )
    }
}