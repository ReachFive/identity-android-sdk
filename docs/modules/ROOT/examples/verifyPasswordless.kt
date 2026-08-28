client.verifyPasswordless(
    phoneNumber = "+33712345678",
    verificationCode = "9876543210",
    success = { authToken -> ... }, // Get a new authentication token
    failure = { error -> ... }, // Handle a ReachFive error
    activity = activity
)
