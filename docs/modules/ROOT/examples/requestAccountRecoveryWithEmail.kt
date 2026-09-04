import co.reachfive.identity.sdk.core.models.ReCaptchaToken

val recaptchaToken: String = // Token returned by Google reCAPTCHA in your app

client.requestAccountRecovery(
    email = "john.doe@gmail.com",
    redirectUrl = "https://example-account-recovery.com",
    captcha = ReCaptchaToken(recaptchaToken),
    success = { _ -> ... }, // Do something
    failure = { error -> ... } // Handle a ReachFive error
)
