import com.reach5.identity.sdk.core.models.AuthToken

val authToken: AuthToken = // The authentication token obtained following signup or login.

client.verifyPhoneNumber(
    authToken = authToken,
    phoneNumber = "+33750253354",
    verificationCode = "0123456",
    success = { _ -> ... }, // Do something
    failure = { error -> ... } // Handle a ReachFive error
)
