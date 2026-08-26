client.loginWithPasskey(
    loginRequest = WebAuthnLoginRequest.PhoneNumberWebAuthnLoginRequest(
        phoneNumber = "+33682234940",
    ),
    origin = "passkey login from Android app",
    success = { authToken -> ... }, // Get the profile's authentication token
    failure = { error -> ... }, // Handle a ReachFive error
    activity = activity,
)