import com.reach5.identity.sdk.core.models.AuthToken
import com.reach5.identity.sdk.core.models.Profile

val authToken: AuthToken = // The authentication token obtained from login or signup.

client.removeMfaPhoneNumber(
    authToken = authToken,
    phoneNumber = "+3531119393",
    success = { _ -> ... }, // Do something
    failure = { error -> ... } // Handle a ReachFive error
)
