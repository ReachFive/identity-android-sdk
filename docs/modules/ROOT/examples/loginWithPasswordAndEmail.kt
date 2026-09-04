import co.reachfive.identity.sdk.core.models.ReCaptchaToken

val recaptchaToken: String = // Token returned by Google reCAPTCHA in your app

client.loginWithPassword(
  email = "john.doe@gmail.com",
  password = "UCrcF4RH",
  scope = listOf("openid", "profile", "email"),
  captcha = ReCaptchaToken(recaptchaToken),
  success = { authToken -> ... }, // Get the profile's authentication token
  failure = { error -> ... } // Handle a ReachFive error
)
