client.requestPasswordReset(
    email = "john.doe@gmail.com",
    redirectUrl = "https://example-password-reset.com"
    success = { _ -> ... }, // Do something
    failure = { error -> ... } // Handle a ReachFive error
)
