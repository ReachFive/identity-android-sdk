package com.reach5.identity.sdk.demo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.reach5.identity.sdk.core.ReachFive
import com.reach5.identity.sdk.core.models.ReachFiveError
import com.reach5.identity.sdk.core.models.SdkConfig
import com.reach5.identity.sdk.core.models.AuthToken
import com.reach5.identity.sdk.core.models.responses.webAuthn.DeviceCredential
import io.github.cdimascio.dotenv.dotenv
import kotlinx.android.synthetic.main.webauthn_devices.*


class AuthenticatedActivity : AppCompatActivity() {
    private val TAG = "Reach5_AuthActivity"

    private val dotenv = dotenv {
        directory = "/assets"
        filename = "env"
    }

    // This variable is only mandatory for the FIDO2 login flow
    private val origin = dotenv["ORIGIN"] ?: ""

    private lateinit var reach5: ReachFive
    private lateinit var authToken: AuthToken

    private lateinit var deviceAdapter: DevicesAdapter
    private lateinit var devicesDisplayed: List<DeviceCredential>

    companion object {
        const val AUTH_TOKEN = "AUTH_TOKEN"
        const val SDK_CONFIG = "SDK_CONFIG"
        const val REGISTER_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_authenticated)

        this.authToken = intent.getParcelableExtra(AUTH_TOKEN)
        this.devicesDisplayed = listOf()

        val sdkConfig = intent.getParcelableExtra<SdkConfig>(SDK_CONFIG)
        this.reach5 = ReachFive(
            sdkConfig = sdkConfig,
            providersCreators = listOf(),
            activity = this
        )

        val givenNameTextView = findViewById<View>(R.id.user_given_name) as TextView
        givenNameTextView.text = this.authToken.user?.givenName

        val familyNameTextView = findViewById<View>(R.id.user_family_name) as TextView
        familyNameTextView.text = this.authToken.user?.familyName

        val emailTextView = findViewById<View>(R.id.user_email) as TextView
        emailTextView.text = this.authToken.user?.email

        val phoneNumberTextView = findViewById<View>(R.id.user_phone_number) as TextView
        phoneNumberTextView.text = this.authToken.user?.phoneNumber

        newFriendlyName.setText(android.os.Build.MODEL)
        addNewDevice.setOnClickListener {
            this.reach5.addNewWebAuthnDevice(
                authToken = this.authToken,
                origin = origin,
                friendlyName = newFriendlyName.text.trim().toString(),
                registerRequestCode = REGISTER_REQUEST_CODE
            ) {
                Log.d(TAG, "addNewWebAuthnDevice error=$it")
                showToast(it.data?.errorUserMsg ?: it.message)
            }
        }

        deviceAdapter =
            DevicesAdapter(applicationContext, this.devicesDisplayed, object : ButtonCallbacks {
                override fun removeDeviceCallback(position: Int) {
                    val device = deviceAdapter.getItem(position)

                    reach5.removeWebAuthnDevice(
                        authToken = authToken,
                        deviceId = device.id,
                        successWithNoContent = {
                            showToast("The FIDO2 device '${device.friendlyName}' is removed")
                            refreshDevicesDisplayed()
                        },
                        failure = {
                            Log.d(TAG, "removeWebAuthnDevice error=$it")
                            showErrorToast(it)
                        }
                    )
                }
            })
        devices.adapter = deviceAdapter

        refreshDevicesDisplayed()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult - requestCode: $requestCode, resultCode: $resultCode")

        when (resultCode) {
            RESULT_OK -> {
                data?.let {
                    when (requestCode) {
                        REGISTER_REQUEST_CODE -> {
                            handleWebAuthnRegisterResponse(data)
                        }
                    }
                }
            }
            RESULT_CANCELED -> {
                val result = "Operation is cancelled"
                Log.d(TAG, result)
            }
            else -> {
                val result = "Operation failed, with resultCode: $resultCode"
                Log.e(TAG, result)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_logout -> {
                finish()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        menu?.findItem(R.id.menu_java)?.isVisible = false
        return super.onCreateOptionsMenu(menu)
    }

    private fun showDevicesTitle() {
        devicesTitle.visibility =
            if (this.devicesDisplayed.isEmpty()) View.INVISIBLE else View.VISIBLE
    }

    private fun refreshDevicesDisplayed() {
        reach5.listWebAuthnDevices(
            authToken,
            success = {
                this.devicesDisplayed = it
                this.deviceAdapter.refresh(this.devicesDisplayed)
                showDevicesTitle()
            },
            failure = {
                Log.d(TAG, "listWebAuthnDevices error=$it")
                showErrorToast(it)
            }
        )
    }

    private fun handleWebAuthnRegisterResponse(intent: Intent) {
        reach5.onAddNewWebAuthnDeviceResult(
            authToken = this.authToken,
            intent = intent,
            successWithNoContent = {
                showToast("New FIDO2 device registered")
                refreshDevicesDisplayed()
            },
            failure = {
                Log.d(TAG, "onAddNewWebAuthnDeviceResult error=$it")
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
