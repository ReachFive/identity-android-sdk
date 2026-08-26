import com.reach5.identity.sdk.core.models.AuthToken

val authToken: AuthToken = // The authentication token obtained from login or signup.

client.refreshAccessToken(
  authToken = authToken,
  success = { authToken -> ... }, // Get a new authentication token
  failure = { error -> ... } // Handle a ReachFive error
)
