import com.reach5.identity.sdk.core.models.Profile

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
    success = { authToken -> ... }, // Get the profile's authentication token
    failure = { error -> ... } // Handle a ReachFive error
)
