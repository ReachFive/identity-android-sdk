import com.reach5.identity.sdk.core.models.AuthToken

val authToken: AuthToken = // The authentication token obtained following signup or login.

client.getProfile(
    authToken = authToken,
    success = { profile -> ... }, // Get the profile
    failure = { error -> ... } // Handle a ReachFive error
)
