import com.reach5.identity.sdk.core.models.AuthToken
import com.reach5.identity.sdk.core.models.requests.UpdatePasswordRequest.*

val authToken: AuthToken = // The authentication token obtained following signup or login.

client.updatePassword(
    updatePasswordRequest = UpdatePasswordRequest.AccessTokenParams(authToken, "gVc7piBn", "ZPf7LFtc"),
    success = { _ -> ... }, // Do something
    failure = { error -> ... } // Handle a ReachFive error
)
