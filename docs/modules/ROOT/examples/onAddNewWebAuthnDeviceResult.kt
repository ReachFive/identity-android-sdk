import com.reach5.identity.sdk.core.models.AuthToken

override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    // Method A, using SDK helper:
    val handler = reach5.resolveResultHandler(requestCode, resultCode, data)
    if (handler is WebAuthnDeviceAddResult) {
        handler.handle(
            success = {
                showToast("New FIDO2 device registered")
                refreshDevicesDisplayed()
            },
            failure = {
                Log.d(TAG, "onAddNewWebAuthnDeviceResult error=$it")
                showErrorToast(it)
            }
        )
    }

    // Method B, directly calling method. Nothing happens if requestCode does not concern SDK:
    reach5.onAddNewWebAuthnDeviceResult(
        requestCode,
        data,
        success = {
            showToast("New FIDO2 device registered")
            refreshDevicesDisplayed()
        },
        failure = {
            Log.d(TAG, "onAddNewWebAuthnDeviceResult error=$it")
            showErrorToast(it)
        }
    )
}
