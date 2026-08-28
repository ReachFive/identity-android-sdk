import com.reach5.identity.sdk.core.models.AuthToken

val WEBAUTHN_REGISTER_REQUEST_CODE = 1

val authToken: AuthToken = // The authentication token obtained following signup or login.

client.addNewWebAuthnDevice(
    authToken = authToken,
    origin = "https://dev-sandbox-268508.web.app",
    friendlyName = "Nexus 5"
    registerRequestCode = WEBAUTHN_REGISTER_REQUEST_CODE,
    failure = { error -> ... } // Handle a ReachFive error
)
