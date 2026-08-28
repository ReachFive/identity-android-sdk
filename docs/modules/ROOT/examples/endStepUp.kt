import com.reach5.identity.sdk.core.models.AuthToken
import com.reach5.identity.sdk.core.models.Profile

val authToken: AuthToken = // The authentication token obtained from login or signup.

client.endStepUp(
    challengeId: "m3DaoT...7Rzp1m",
    verificationCode: "123456"
    success = { _ -> ... }, // Do something
    failure = { error -> ... }, // Handle a ReachFive error
    activity = activity,
    trustDevice = true
)
