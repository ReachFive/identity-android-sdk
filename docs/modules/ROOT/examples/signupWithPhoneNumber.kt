import com.reach5.identity.sdk.core.ReachFive
import com.reach5.identity.sdk.core.models.ProfileSignupRequest
import com.reach5.identity.sdk.core.models.SignupResponse

client.signup(
    profile = ProfileSignupRequest(
        givenName = "John",
        familyName = "Doe",
        gender = "male",
        phoneNumber = "+353875551234",
        customIdentifier = "coolCat55",
        password = "hjk90wxc"
    ),
    scope = listOf("openid", "profile", "phone"),
    success = { response ->
        when (response) {
            is SignupResponse.AchievedLogin -> {
                // Signup succeeded and the user is logged in
                // Handle authenticated session
            }
            is SignupResponse.AwaitingIdentifierVerification -> {
                // Signup succeeded but requires phone number verification before login
                // Prompt the user to verify their phone number
            }
        }
    },
    failure = { error ->
        // Handle a ReachFive error
    }
)
