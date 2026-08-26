import com.reach5.identity.sdk.core.models.AuthToken

val authToken: AuthToken = // The authentication token obtained from login or signup.

client.verifyMfaEmailRegistration(
    authToken = authToken,
    verificationCode = "0123456",
    success = { _ -> ... }, // Do something
    failure = { error -> ... } // Handle a ReachFive error
)
