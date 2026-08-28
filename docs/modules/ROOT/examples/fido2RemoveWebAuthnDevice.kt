package com.reach5.identity.sdk.demo

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

    /*...*/

    private fun removeDevice(position: Int) {
        // The selected device
        val device = this.devicesDisplayed[position]

        // Delete the selected registered device from the ReachFive server
        reach5.removeWebAuthnDevice(
            authToken = authToken,
            deviceId = device.id,
            success = {
                Log.d("ReachFive", "The FIDO2 device '${device.friendlyName}' is removed")
            },
            failure = {
                Log.d("ReachFive", "Unable to remove the FIDO2 device '${device.friendlyName}': $it")
            }
        )
    }
}