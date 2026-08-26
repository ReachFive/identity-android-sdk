private lateinit var webAuthnId: String

client.signupWithWebAuthn(
    profile = ProfileWebAuthnSignupRequest(
        givenName = "John",
        familyName = "Doe",
        gender = "male",
        email = "john.doe@gmail.com"
    ),
    origin = "https://dev-sandbox-268508.web.app",
    friendlyName = "Nexus 5"
    success = { _ -> ... }, // Initial call success
    failure = { error -> ... }, // Handle a ReachFive error
    activity = activity
)
