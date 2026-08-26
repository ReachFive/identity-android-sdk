import com.reach5.identity.sdk.core.models.AuthToken

val authToken: AuthToken = // The authentication token obtained following signup or login.

client.verifyEmail(
    authToken = authToken,
    email = "bob@example.com",
    verificationCode = "0123456",
    success = { _ -> ... }, // Do something
    failure = { error -> ... } // Handle a ReachFive error
)
