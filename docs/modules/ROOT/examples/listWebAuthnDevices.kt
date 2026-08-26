import com.reach5.identity.sdk.core.models.AuthToken

val authToken: AuthToken = // The authentication token obtained following signup or login.

client.listWebAuthnDevices(
    authToken = authToken,
    success = { devices -> ... }, // Get the list of devices
    failure = { error -> ... } // Handle a ReachFive error
)
