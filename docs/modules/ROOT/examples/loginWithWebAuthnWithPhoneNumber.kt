import com.reach5.identity.sdk.core.models.requests.webAuthn.WebAuthnLoginRequest

client.loginWithWebAuthn(
    webAuthnLoginRequest = WebAuthnLoginRequest.PhoneNumberWebAuthnLoginRequest(
        origin = "https://dev-sandbox-268508.web.app",
        phoneNumber = "+33682234940",
        scope = setOf("openid", "profile", "phone")
    ),
    failure = { error -> ... }, // Handle a ReachFive error
    activity = activity
)
