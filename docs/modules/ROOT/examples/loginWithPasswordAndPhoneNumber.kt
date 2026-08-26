client.loginWithPassword(
  phoneNumber = "+33682234940",
  password = "UCrcF4RH",
  scope = listOf("openid", "profile", "phone"),
  success = { authToken -> ... }, // Get the profile's authentication token
  failure = { error -> ... } // Handle a ReachFive error
)
