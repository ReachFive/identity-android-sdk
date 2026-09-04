client.loginWithWeb(
    activity = this,
    state = "state",
    nonce = "nonce",
    origin = "origin",
    loginUrlFragment = mapOf("channel" to "android"), // Example keys; the Login URL chooses what to read from the fragment
)
