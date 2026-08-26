import com.reach5.identity.sdk.core.models.AuthToken

val authToken: AuthToken = // The authentication token obtained following signup or login.

client.updateEmail(
    authToken = authToken,
    email = "johnatthan.doe@gmail.com",
    redirectUrl = "https://example-email-update.com"
    success = { updatedProfile -> ... }, // Get the updated profile
    failure = { error -> ... } // Handle a ReachFive error
)
