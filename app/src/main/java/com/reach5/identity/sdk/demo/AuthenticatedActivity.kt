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
import com.google.android.gms.fido.Fido
import com.google.android.gms.fido.fido2.api.common.AuthenticatorErrorResponse
import com.reach5.identity.sdk.core.ReachFive
import com.reach5.identity.sdk.core.ReachFive.Companion.FIDO2_REGISTER_REQUEST_CODE
import com.reach5.identity.sdk.core.models.SdkConfig
import com.reach5.identity.sdk.core.models.responses.AuthToken
import io.github.cdimascio.dotenv.dotenv
import kotlinx.android.synthetic.main.webauthn.*


class AuthenticatedActivity : AppCompatActivity() {
    private val TAG = "Reach5_AuthActivity"

    private val dotenv = dotenv {
        directory = "/assets"
        filename = "env"
    }
    private val origin =
        dotenv["RP_ID"] ?: throw IllegalArgumentException("The relying server ID is undefined! Check your `env` file.")

    private lateinit var reach5: ReachFive
    private lateinit var authToken: AuthToken

    private lateinit var deviceAdapter: DevicesAdapter

    companion object {
        const val AUTH_TOKEN = "AUTH_TOKEN"
        const val SDK_CONFIG = "SDK_CONFIG"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_authenticated)

        this.authToken = intent.getParcelableExtra(AUTH_TOKEN)

        val sdkConfig = intent.getParcelableExtra<SdkConfig>(SDK_CONFIG)
        this.reach5 = ReachFive(
            sdkConfig = sdkConfig,
            providersCreators = listOf(),
            activity = this
        )

        val givenNameTextView = findViewById<View>(R.id.user_given_name) as TextView
        givenNameTextView.text =   this.authToken.user?.givenName

        val familyNameTextView = findViewById<View>(R.id.user_family_name) as TextView
        familyNameTextView.text =   this.authToken.user?.familyName

        val emailTextView = findViewById<View>(R.id.user_email) as TextView
        emailTextView.text = this.authToken.user?.email

        val phoneNumberTextView = findViewById<View>(R.id.user_phone_number) as TextView
        phoneNumberTextView.text = this.authToken.user?.phoneNumber

        newFriendlyName.setText(android.os.Build.MODEL)
        addNewDevice.setOnClickListener {
            this.reach5.addNewWebAuthnDevice(this.authToken, origin, newFriendlyName.text.trim().toString()) {
                Log.d(TAG, "addNewWebAuthnDevice error=$it")
                showToast("Login error=${it.message}")
            }
        }

        reach5.listWebAuthnDevices(
            authToken,
            success = {
                deviceAdapter = DevicesAdapter(applicationContext, it)
                devices.adapter = deviceAdapter

                devicesTitle.visibility = View.VISIBLE
            },
            failure = {
                Log.d(TAG,"listWebAuthnDevices error=$it")
                showToast("Login error=${it.message}")
            }
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult - requestCode: $requestCode, resultCode: $resultCode")

        when (resultCode) {
            RESULT_OK -> {
                data?.let {
                    if (it.hasExtra(Fido.FIDO2_KEY_ERROR_EXTRA)) {
                        handleErrorResponse(data.getByteArrayExtra(Fido.FIDO2_KEY_ERROR_EXTRA))
                    } else if (it.hasExtra(Fido.FIDO2_KEY_RESPONSE_EXTRA)) {
                        val fido2Response = data.getByteArrayExtra(Fido.FIDO2_KEY_RESPONSE_EXTRA)
                        when (requestCode) {
                            FIDO2_REGISTER_REQUEST_CODE -> handleRegisterResponse(fido2Response)
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

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun handleErrorResponse(errorBytes: ByteArray) {
        val authenticatorErrorResponse = AuthenticatorErrorResponse.deserializeFromBytes(errorBytes)
        val errorName = authenticatorErrorResponse.errorCode.name
        val errorMessage = authenticatorErrorResponse.errorMessage

        Log.e(TAG, "errorCode.name: $errorName")
        Log.e(TAG, "errorMessage: $errorMessage")
    }

    private fun handleRegisterResponse(fido2Response: ByteArray) {
        reach5.onAddNewWebAuthnDeviceResult(
            authToken = this.authToken,
            fido2Response = fido2Response,
            successWithNoContent = { showToast("New FIDO2 device registered") },
            failure = {
                Log.d(TAG, "onAddNewWebAuthnDeviceResult error=$it")
                showToast("Login error=${it.message}")
            }
        )
    }
}