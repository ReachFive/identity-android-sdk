import com.reach5.identity.sdk.core.models.requests.webAuthn.WebAuthnLoginRequest

client.loginWithWebAuthn(
    webAuthnLoginRequest = WebAuthnLoginRequest.EmailWebAuthnLoginRequest(
        origin = "https://dev-sandbox-268508.web.app",
        email = "john.doe@example.com",
        scope = setOf("openid", "profile", "email")
    ),
    failure = { error -> ... }, // Handle a ReachFive error
    activity = activity
)
