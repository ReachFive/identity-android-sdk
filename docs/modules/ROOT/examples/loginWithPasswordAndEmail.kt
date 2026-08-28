client.loginWithPassword(
  email = "john.doe@gmail.com",
  password = "UCrcF4RH",
  scope = listOf("openid", "profile", "email"),
  success = { authToken -> ... }, // Get the profile's authentication token
  failure = { error -> ... } // Handle a ReachFive error
)
