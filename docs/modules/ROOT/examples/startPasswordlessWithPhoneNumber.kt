import com.reach5.identity.sdk.core.models.Profile

client.startPasswordless(
    phoneNumber = "+33612345678",
    redirectUri = "reachfive-${clientId}://callback",
    success = { _ -> ... }, // Do something
    failure = { error -> ... }, // Handle a ReachFive error
    activity = activity
)
