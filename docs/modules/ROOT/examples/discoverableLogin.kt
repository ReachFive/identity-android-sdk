client.discoverableLogin(
    origin = "discoverable login from Android app",
    success = { authToken -> ... }, // Get the profile's authentication token
    failure = { error -> ... }, // Handle a ReachFive error
    activity = activity,
)