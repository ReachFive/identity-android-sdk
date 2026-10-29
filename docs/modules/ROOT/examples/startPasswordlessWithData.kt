import co.reachfive.identity.sdk.core.models.requests.PasswordlessSignupData

client.startPasswordless(
    email = "john.doe@email.com",
    redirectUri = "reachfive-${clientId}://callback",
    success = { _ -> ... }, // Do something
    failure = { error -> ... }, // Handle a ReachFive error
    activity = activity,
    data = PasswordlessSignupData(
        givenName = "John",
        familyName = "Doe",
        phoneNumber = "+33612345678",
        customFields = mapOf("favorite_color" to "blue")
    )
)
