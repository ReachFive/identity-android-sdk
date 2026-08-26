import com.reach5.identity.sdk.core.models.Profile
import com.reach5.identity.sdk.core.models.AuthToken

val authToken: AuthToken = // The authentication token obtained following signup or login.

client.updateProfile(
  authToken = authToken,
  profile = Profile(givenName = "Jonathan", phoneNumber = "+33750253354"),
  success = { updatedProfile -> ... }, // Get the updated profile
  failure = { error -> ... } // Handle a ReachFive error
)
