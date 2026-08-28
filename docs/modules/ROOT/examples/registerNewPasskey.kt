client.registerNewPasskey(
    authToken = authToken,
    friendlyName = "Google Password Manager",
    success = { ... }, // Handle success
    failure = { error -> ... }, // Handle a ReachFive error
    activity = activity,
)