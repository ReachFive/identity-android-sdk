import com.reach5.identity.sdk.core.models.AuthToken
import com.reach5.identity.sdk.core.models.Profile

val authToken: AuthToken = // The authentication token obtained from login or signup.

client.listMfaTrustedDevices(
    authToken = authToken,
    success = { _ -> ... }, // Do something
    failure = { error -> ... } // Handle a ReachFive error
)
