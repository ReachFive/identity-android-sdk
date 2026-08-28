import com.reach5.identity.sdk.core.models.AuthToken

val authToken: AuthToken = // The authentication token obtained from login or signup.

client.startMfaPhoneNumberRegistration(
    authToken = authToken,
    phoneNumber = "+33612345678",
    success = { _ -> ... }, // Do something
    failure = { error -> ... } // Handle a ReachFive error
)
