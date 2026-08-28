import com.reach5.identity.sdk.core.models.AuthToken

val authToken: AuthToken = // The authentication token obtained following signup or login.

client.updatePhoneNumber(
  authToken = authToken,
  phoneNumber = "+33792244940",
  success = { updatedProfile -> ...}, // Get the updated profile
  failure = { error -> ... } // Handle a ReachFive error
)
