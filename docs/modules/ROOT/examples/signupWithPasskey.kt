client.signupWithPasskey(
    ProfileWebAuthnSignupRequest(
        givenName = "John",
        familyName = "Doe",
        email = "john.doe@gmail.com"
    ),
    friendlyName = "Google Password Manager",
    origin = "passkey signup from Android app",
    success = { authToken -> ... }, // Get the profile's authentication token
    failure = { error -> ... }, // Handle a ReachFive error
    activity = activity,
)