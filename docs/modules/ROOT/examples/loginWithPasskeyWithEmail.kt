client.loginWithPasskey(
    loginRequest = WebAuthnLoginRequest.EmailWebAuthnLoginRequest(
        email = "john.doe@example.com"
    ),
    origin = "passkey login from Android app",
    success = { authToken -> ... }, // Get the profile's authentication token
    failure = { error -> ... }, // Handle a ReachFive error
    activity = activity,
)