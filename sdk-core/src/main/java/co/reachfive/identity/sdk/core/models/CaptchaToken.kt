package co.reachfive.identity.sdk.core.models

/**
 * A captcha token together with the provider that issued it. The pair is sent as-is to the API,
 * which verifies it against the provider the account has enabled (Console > Security > Captcha).
 */
abstract class CaptchaToken {
    abstract val provider: String
    abstract val token: String

    companion object {
        const val RECAPTCHA = "recaptcha"
        const val CAPTCHAFOX = "captchafox"
        const val RECAPTCHA_ENTERPRISE = "recaptcha_enterprise"
    }
}

data class ReCaptchaToken(override val token: String) : CaptchaToken() {
    override val provider: String = RECAPTCHA
}

data class CaptchaFoxToken(override val token: String) : CaptchaToken() {
    override val provider: String = CAPTCHAFOX
}

data class ReCaptchaEnterpriseToken(override val token: String) : CaptchaToken() {
    override val provider: String = RECAPTCHA_ENTERPRISE
}
