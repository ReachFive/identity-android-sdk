import com.reach5.identity.sdk.core.models.AuthToken
import com.reach5.identity.sdk.core.models.requests.UpdatePasswordRequest.*

val freshauthToken: AuthToken = // Here paste the authorization token of the profile retrieved after login (less than 5 min)

client.updatePassword(
    updatePasswordRequest = UpdatePasswordRequest.FreshAccessTokenParams(freshauthToken, "ZPf7LFtc"),
    success = { _ -> ... }, // Do something
    failure = { error -> ... } // Handle a ReachFive error
)
