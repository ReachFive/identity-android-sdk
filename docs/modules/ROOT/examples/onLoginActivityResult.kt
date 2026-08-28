override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

    // Method A
    // Tip: nothing happens if the request code does not concern ReachFive
    client.onLoginActivityResult(
        requestCode = requestCode,
        resultCode = resultCode,
        intent = data,
        success = { handleLoginSuccess(it) },
        failure = { reachFiveError ->
            Log.e(TAG, "Login error!", reachFiveError)
        },
        activity = this
    )

    // Method B
    val handler: ActivityResultHandler? = reach5.resolveResultHandler(requestCode, resultCode, data)
    when (handler) {
        is LoginResultHandler -> handler.handle(
            success = { handleLoginSuccess(it) },
            failure = { reachfiveError ->
                Log.e(TAG, "Login error!", reachfiveError)
            },
            activity = this
        )

        else -> { /* Not a login callback, or not ReachFive */ }
    }
}
