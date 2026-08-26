client.loadSocialProviders(
  this,
  success = { providers -> /* update the providers you display */},
  failure = { Log.d(TAG, "Loading providers failed ${it.message}") }
)
