client.requestAccountRecovery(
    phoneNumber = "+33682234940",
    success = { _ -> ... }, // Do something
    failure = { error -> ... } // Handle a ReachFive error
)
