client.loginWithPassword(
  customIdentifier = "coolCat55",
  password = "UCrcF4RH",
  scope = listOf("openid", "profile", "email"),
  success = { authToken -> ... }, // Get the profile's authentication token
  failure = { error -> ... } // Handle a ReachFive error
)
