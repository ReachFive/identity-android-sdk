import com.reach5.identity.sdk.core.models.AuthToken

val authToken: AuthToken = // The authentication token obtained following signup or login.

client.removeWebAuthnDevice(
    authToken = authToken,
    deviceId = "AcfbMjJcS7vE46R3WHOJL...",
    success = { _ -> ... }, // Do something
    failure = { error -> ... } // Handle a ReachFive error
)
