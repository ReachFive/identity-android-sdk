import com.reach5.identity.sdk.core.models.AuthToken
import com.reach5.identity.sdk.core.models.Profile

client.startStepUp(
    startStepUpFlow = startStepUpAuthTokenFlow,
    authType = "sms",
    redirectUri = "reachfive-${clientId}://callback",
    scope = setOf("openid", "profile", "phone"),
    success = { _ -> ... }, // Do something
    failure = { error -> ... }, // Handle a ReachFive error
    origin = "https://dev-sandbox-268508.web.app",
)
