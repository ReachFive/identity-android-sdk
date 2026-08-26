import com.reach5.identity.sdk.core.models.Profile

client.startPasswordless(
    email = "john.doe@email.com",
    redirectUri = "reachfive-${clientId}://callback",
    success = { _ -> ... }, // Do something
    failure = { error -> ... }, // Handle a ReachFive error
    activity = activity
)
