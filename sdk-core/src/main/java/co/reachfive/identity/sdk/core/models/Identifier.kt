package co.reachfive.identity.sdk.core.models

sealed interface Identifier {
    val value: String
}

data class Email(override val value: String): Identifier
data class PhoneNumber(override val value: String): Identifier