import com.reach5.identity.sdk.core.models.AuthToken
import com.reach5.identity.sdk.core.models.Profile

val authToken: AuthToken = // The authentication token obtained from login or signup.

client.removeMfaTrustedDevice(
    authToken = authToken,
    trustedDeviceId = trustedDeviceId,
    success = { _ -> ... }, // Do something
    failure = { error -> ... } // Handle a ReachFive error
)
