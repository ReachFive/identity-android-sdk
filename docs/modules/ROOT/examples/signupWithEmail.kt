import com.reach5.identity.sdk.core.ReachFive
import com.reach5.identity.sdk.core.models.ProfileSignupRequest
import com.reach5.identity.sdk.core.models.SignupResponse

client.signup(
    profile = ProfileSignupRequest(
        givenName = "John",
        familyName = "Doe",
        gender = "male",
        email = "john.doe@gmail.com",
        customIdentifier = "coolCat55",
        password = "hjk90wxc"
    ),
    scope = listOf("openid", "profile", "email"),
    success = { response ->
        when (response) {
            is SignupResponse.AchievedLogin(authToken) -> {
                // Signup succeeded and the user is logged in
                // Handle authenticated session
            }
            is SignupResponse.AwaitingIdentifierVerification -> {
                // Signup succeeded but requires email verification before login
                // Prompt the user to verify their email
            }
        }
    },
    failure = { error ->
        // Handle a ReachFive error
    }
)
