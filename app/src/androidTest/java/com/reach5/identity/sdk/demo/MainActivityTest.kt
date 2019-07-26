package com.reach5.identity.sdk.demo

import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import com.reach5.identity.sdk.core.ReachFive
import com.reach5.identity.sdk.core.models.Profile
import com.reach5.identity.sdk.core.models.ReachFiveError
import com.reach5.identity.sdk.core.models.SdkConfig
import com.reach5.identity.sdk.core.models.requests.UpdatePasswordRequest
import io.github.cdimascio.dotenv.dotenv
import junit.framework.TestCase.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.runner.RunWith
import java.util.*
import kotlin.random.Random

/**
 * These tests use an account with:
 * - the SMS feature enabled
 * - the country set to "France"
 * - the following ENFORCED scope: ['email', 'full_write', 'openid', 'phone', 'profile']
 */
@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    private val dotenv = dotenv {
        directory = "/assets"
        filename = "env"
    }

    private val DOMAIN = dotenv["DOMAIN"] ?: ""
    private val CLIENT_ID = dotenv["CLIENT_ID"] ?: ""
    private val defaultSdkConfig: SdkConfig = SdkConfig(DOMAIN, CLIENT_ID)

    @get:Rule
    val activityRule = ActivityTestRule(MainActivity::class.java)

    @get:Rule
    var exceptionRule: ExpectedException = ExpectedException.none()

    @Test
    fun testFailedReachFiveClientInitialization() {
        exceptionRule.expect(IllegalArgumentException::class.java)
        exceptionRule.expectMessage("Invalid URL host: \"\"")

        clientTest(
            initialize = false,
            sdkConfig = SdkConfig("", CLIENT_ID)
        ) { client ->
            client.initialize(
                success = { fail("Should have failed initialization due to missing domain.") },
                failure = { failWithReachFiveError(it) }
            )
        }
    }

    @Test
    fun testSuccessfulClientConfigFetch() = clientTest(initialize = false) { client ->
        val profile = aProfile()

        // given an uninitialized client
        client.signup(
            profile,
            /*scope = setOf("openid"),*/ // WHEN INITIALIZED: becomes setOf('email', 'full_write', 'openid', 'phone', 'profile')
            success = { fail("This test should have failed because the `openid` scope should be missing prior to client initialization, causing auth token parsing to fail.") },
            failure = { expectedError ->
                // Signup success but no id_token returned
                assertEquals(
                    NO_ID_TOKEN,
                    expectedError.message
                )

                client
                    .initialize()
                    .signup(
                        profile,
                        success = { fail("This test should have failed because the email is now already used.") },
                        failure = { emailAlreadyExists ->
                            assertEquals(
                                "Bad Request",
                                emailAlreadyExists.message
                            )
                        }
                    )
            }
        )
    }

    @Test
    fun testSuccessfulSignupWithEmail() = clientTest { client ->
        val profile = aProfile()

        client.signup(
            profile,
            scope = openId,
            success = { authToken -> assertNotNull(authToken) },
            failure = { failWithReachFiveError(it) }
        )
    }

    @Test
    fun testFailedSignupWithAlreadyUsedEmail() = clientTest { client ->
        val profile = aProfile()

        client.signup(
            profile,
            scope = openId,
            success = { authToken ->
                // Check that the returned authentication token is not null
                assertNotNull(authToken)

                client.signup(
                    Profile(email = profile.email, password = profile.email),
                    scope = openId,
                    success = { fail("This test should have failed because the email should be already used.") },
                    failure = { error ->
                        assertEquals("Bad Request", error.message)
                        assertEquals("email_already_exists", error.data?.error)
                        assertEquals("Email already in use", error.data?.errorDescription)
                    }
                )
            },
            failure = { failWithReachFiveError(it) }
        )
    }

    @Test
    fun testFailedSignupWithEmptyEmail() = clientTest { client ->
        val profileWithNoEmail = aProfile().copy(email = "")

        client.signup(
            profileWithNoEmail,
            scope = openId,
            success = { fail("This test should have failed because the email is empty.") },
            failure = { error ->
                assertEquals("Bad Request", error.message)
                assertEquals("invalid_request", error.data?.error)
                assertEquals("Validation failed", error.data?.errorDescription)
                assertEquals("data.email", error.data?.errorDetails?.get(0)?.field)
                assertEquals("Must be a valid email", error.data?.errorDetails?.get(0)?.message)
            }
        )
    }

    @Test
    fun testSuccessfulSignupWithPhoneNumber() = clientTest { client ->
        val profile = aProfile().copy(phoneNumber = aPhoneNumber())

        client.signup(
            profile,
            scope = openId,
            success = { authToken -> assertNotNull(authToken) },
            failure = { failWithReachFiveError(it) }
        )
    }

    @Test
    fun testSuccessfulSignupWithLocalPhoneNumber() = clientTest { client ->
        val profile = aProfile().copy(phoneNumber = aPhoneNumber(international = false))

        client.signup(
            profile,
            scope = openId,
            success = { authToken -> assertNotNull(authToken) },
            failure = { failWithReachFiveError(it) }
        )
    }

    @Test
    fun testFailedSignupWeakPassword() = clientTest { client ->
        val weakPassword = "toto"
        val profile = aProfile().copy(password = weakPassword)

        client.signup(
            profile,
            scope = openId,
            success = { fail("This test should have failed because the password is too weak.") },
            failure = { error ->
                assertEquals("Bad Request", error.message)
                assertEquals("Validation failed", error.data?.errorDescription)
                assertEquals("Password too weak", error.data?.errorDetails?.get(0)?.message)
            }
        )
    }

    @Test
    fun testFailedSignupAuthTokenRetrievalWithMissingScope() = clientTest { client ->
        val profile = aProfile()

        client.signup(
            profile,
            scope = emptyList(),
            success = { fail("This test should have failed because no 'id_token' was found.") },
            failure = { error -> assertEquals(NO_ID_TOKEN, error.message) }
        )
    }

    @Test
    fun testSuccessfulLoginWithEmail() = clientTest { client ->
        val profile = aProfile()

        client.signup(
            profile,
            scope = openId,
            success = {
                client.loginWithPassword(
                    profile.email!!,
                    profile.password!!,
                    scope = openId,
                    success = { authToken -> assertNotNull(authToken) },
                    failure = { failWithReachFiveError(it) }
                )
            },
            failure = { failWithReachFiveError(it) }
        )
    }

    @Test
    fun testSuccessfulLoginWithPhoneNumber() = clientTest { client ->
        val profile = aProfile().copy(phoneNumber = aPhoneNumber())

        client.signup(
            profile,
            scope = openId,
            success = {
                client.loginWithPassword(
                    profile.phoneNumber!!,
                    profile.password!!,
                    scope = openId,
                    success = { authToken -> assertNotNull(authToken) },
                    failure = { failWithReachFiveError(it) }
                )
            },
            failure = { failWithReachFiveError(it) }
        )
    }

    @Test
    fun testFailedLoginWithNonExistingIdentifier() = clientTest { client ->
        client.loginWithPassword(
            "satoshi.nakamoto@testaccount.io",
            "buybitcoin",
            scope = openId,
            success = { fail("This test should have failed because the profile is not registered.") },
            failure = { error ->
                assertEquals("Bad Request", error.message)
                assertEquals("invalid_grant", error.data?.error)
                assertEquals("Invalid email or password", error.data?.errorDescription)
            }
        )
    }

    @Test
    fun testFailedLoginWithWrongPassword() = clientTest { client ->
        val profile = aProfile().copy(phoneNumber = aPhoneNumber())

        client.signup(
            profile,
            scope = openId,
            success = {
                client.loginWithPassword(
                    profile.phoneNumber!!,
                    "WRONG_PASSWORD",
                    scope = openId,
                    success = { fail("This test should have failed because the password is incorrect.") },
                    failure = { error ->
                        assertEquals("Bad Request", error.message)
                        assertEquals("invalid_grant", error.data?.error)
                        assertEquals("Invalid phone number or password", error.data?.errorDescription)
                    }
                )
            },
            failure = { failWithReachFiveError(it) }
        )
    }

    @Test
    fun testFailedLoginAuthTokenRetrievalWithMissingScope() = clientTest { client ->
        val profile = aProfile().copy(phoneNumber = aPhoneNumber())

        client.signup(
            profile,
            scope = openId,
            success = {
                client.loginWithPassword(
                    profile.phoneNumber!!,
                    profile.password!!,
                    scope = emptyList(),
                    success = { fail("This test should have failed because no 'id_token' was found.") },
                    failure = { error -> assertEquals(error.message, NO_ID_TOKEN) }
                )
            },
            failure = { failWithReachFiveError(it) }
        )
    }

    @Test
    fun testFailedVerifyPhoneNumberWithWrongCode() = clientTest { client ->
        val profile = aProfile().copy(phoneNumber = aPhoneNumber())
        val incorrectVerificationCode = "500"

        client.signup(
            profile,
            fullWrite + openId,
            success = { authToken ->
                client.verifyPhoneNumber(
                    authToken,
                    profile.phoneNumber!!,
                    incorrectVerificationCode,
                    { fail("This test should have failed because the verification code is incorrect.") },
                    { error ->
                        assertEquals("Technical Error", error.message)
                        assertEquals("invalid_grant", error.data?.error)
                        assertEquals("Invalid verification code", error.data?.errorDescription)
                    }
                )
            },
            failure = { failWithReachFiveError(it) }
        )
    }

    @Test
    fun testSuccessfulEmailUpdate() = clientTest { client ->
        val profile = aProfile()
        val newEmail = anEmail()
        val scope = fullWrite + openId + email

        client.signup(
            profile,
            scope,
            { authToken ->
                client.updateEmail(
                    authToken,
                    newEmail,
                    success = { updatedProfile ->
                        assertNotNull(updatedProfile)
                        assertEquals(newEmail, updatedProfile.email)
                    },
                    failure = { failWithReachFiveError(it) }
                )
            },
            { failWithReachFiveError(it) }
        )
    }

    @Test
    fun testFailedEmailUpdateWithSameEmail() = clientTest { client ->
        val profile = aProfile()
        val scope = fullWrite + openId

        client.signup(
            profile,
            scope,
            { authToken ->
                client.updateEmail(
                    authToken,
                    profile.email!!,
                    success = { fail("This test should have failed because the email has not changed.") },
                    failure = { error ->
                        assertEquals("Bad Request", error.message)
                        assertEquals("email_already_exists", error.data?.error)
                        assertEquals("Email already in use", error.data?.errorDescription)
                    }
                )
            },
            { failWithReachFiveError(it) }
        )
    }

    @Test
    fun testFailedEmailUpdateWithMissingScope() = clientTest { client ->
        val profile = aProfile()
        val newEmail = anEmail()

        client.signup(
            profile,
            openId,
            success = { authToken ->
                client.updateEmail(
                    authToken,
                    newEmail,
                    success = { fail(TEST_SHOULD_FAIL_SCOPE_MISSING) },
                    failure = { error ->
                        assertEquals(error.message, "Technical Error")
                        assertEquals(error.data?.error, "insufficient_scope")
                        assertEquals(
                            "The token does not contain the required scope: full_write",
                            error.data?.errorDescription
                        )
                    }
                )
            },
            failure = { failWithReachFiveError(it) }
        )
    }

    @Test
    fun testSuccessfulPhoneNumberUpdate() = clientTest { client ->
        val profile = aProfile()
        val newNumber = aPhoneNumber()
        val scope = fullWrite + openId + phone

        client.signup(
            profile,
            scope,
            { authToken ->
                client.updatePhoneNumber(
                    authToken,
                    newNumber,
                    success = { updatedProfile ->
                        assertNotNull(updatedProfile)
                        assertEquals(newNumber, updatedProfile.phoneNumber)
                    },
                    failure = { failWithReachFiveError(it) }
                )
            },
            { failWithReachFiveError(it) }
        )
    }

    @Test
    fun testSuccessfulPhoneNumberUpdateWithSameNumber() = clientTest { client ->
        val profile = aProfile().copy(phoneNumber = aPhoneNumber())
        val scope = fullWrite + openId + phone

        client.signup(
            profile,
            scope,
            { authToken ->
                client.updatePhoneNumber(
                    authToken,
                    profile.phoneNumber!!,
                    success = { updatedProfile ->
                        assertNotNull(updatedProfile)
                        assertEquals(profile.phoneNumber!!, updatedProfile.phoneNumber)
                    },
                    failure = { failWithReachFiveError(it) }
                )
            },
            { failWithReachFiveError(it) }
        )
    }

    @Test
    fun testFailedPhoneNumberUpdateWithMissingScope() = clientTest { client ->
        val profile = aProfile().copy(phoneNumber = aPhoneNumber())
        val newNumber = aPhoneNumber()

        client.signup(
            profile,
            openId,
            success = { authToken ->
                client.updatePhoneNumber(
                    authToken,
                    newNumber,
                    success = { fail(TEST_SHOULD_FAIL_SCOPE_MISSING) },
                    failure = { error ->
                        assertEquals("Technical Error", error.message)
                        assertEquals("insufficient_scope", error.data?.error)
                        assertEquals(
                            "The token does not contain the required scope: full_write",
                            error.data?.errorDescription
                        )
                    }
                )
            },
            failure = { failWithReachFiveError(it) }
        )
    }

    @Test
    fun testSuccessfulProfileUpdate() = clientTest { client ->
        val theProfile = aProfile()
        val updatedGivenName = "Christelle"
        val updatedFamilyName = "Couet"
        val scope = fullWrite + openId + email + profile

        client.signup(
            theProfile,
            scope,
            { authToken ->
                client
                    .updateProfile(
                        authToken,
                        Profile(givenName = updatedGivenName, familyName = updatedFamilyName),
                        { updatedProfile ->
                            assertNotNull(updatedProfile)
                            assertEquals(theProfile.email, updatedProfile.email)
                            assertEquals(updatedGivenName, updatedProfile.givenName)
                            assertEquals(updatedFamilyName, updatedProfile.familyName)
                            assertEquals(theProfile.gender!!, updatedProfile.gender)
                        },
                        { failWithReachFiveError(it) }
                    )
            },
            { failWithReachFiveError(it) }
        )
    }

    @Test
    fun testFailedProfileUpdateWithMissingScope() = clientTest { client ->
        val profile = aProfile()

        client.signup(
            profile,
            openId,
            success = { authToken ->
                client
                    .updateProfile(
                        authToken,
                        Profile(givenName = "Peter"),
                        { fail(TEST_SHOULD_FAIL_SCOPE_MISSING) },
                        { error ->
                            assertEquals("Technical Error", error.message)
                            assertEquals("insufficient_scope", error.data?.error)
                            assertEquals(
                                "The token does not contain the required scope: full_write",
                                error.data?.errorDescription
                            )
                        }
                    )
            },
            failure = { failWithReachFiveError(it) }
        )
    }

    @Test
    fun testSuccessfulPasswordUpdateWithFreshAccessToken() = clientTest { client ->
        val profile = aProfile()
        val newPassword = "ZPf7LFtc"
        val scope = fullWrite + openId

        client.signup(
            profile,
            scope,
            { authToken ->
                client.updatePassword(
                    authToken,
                    UpdatePasswordRequest.FreshAccessTokenParams(newPassword),
                    successWithNoContent = {
                        client.loginWithPassword(
                            profile.email!!,
                            newPassword,
                            success = { authToken -> assertNotNull(authToken) },
                            failure = { failWithReachFiveError(it) }
                        )
                    },
                    failure = { failWithReachFiveError(it) }
                )
            },
            { failWithReachFiveError(it) }
        )
    }

    @Test
    fun testSuccessfulPasswordUpdateWithAccessToken() = clientTest { client ->
        val profile = aProfile()
        val newPassword = "XLpYXz7z"
        val scope = fullWrite + openId

        client.signup(
            profile,
            scope,
            { authToken ->
                client.updatePassword(
                    authToken,
                    UpdatePasswordRequest.AccessTokenParams(profile.password!!, newPassword),
                    successWithNoContent = {
                        client.loginWithPassword(
                            profile.email!!,
                            newPassword,
                            success = { authToken -> assertNotNull(authToken) },
                            failure = { failWithReachFiveError(it) }
                        )
                    },
                    failure = { failWithReachFiveError(it) }
                )
            },
            { failWithReachFiveError(it) }
        )
    }

    @Test
    fun testFailedPasswordUpdateWithAccessTokenWithSamePassword() = clientTest { client ->
        val profile = aProfile()
        val scope = fullWrite + openId

        client.signup(
            profile,
            scope,
            { authToken ->
                client.updatePassword(
                    authToken,
                    UpdatePasswordRequest.AccessTokenParams(profile.password!!, profile.password!!),
                    successWithNoContent = { fail("This test should have failed because the password has not changed.") },
                    failure = { error ->
                        assertEquals(error.message, "Bad Request")
                        assertEquals(error.data?.error, "invalid_request")
                        assertEquals(
                            "New password should be different from the old password",
                            error.data?.errorDescription

                        )
                    }
                )
            },
            { failWithReachFiveError(it) }
        )
    }

    @Test
    fun testFailedPasswordUpdateWithEmailAndWrongCode() = clientTest { client ->
        val profile = aProfile()
        val incorrectVerificationCode = "234"
        val scope = fullWrite + openId

        client.signup(
            profile,
            scope,
            { authToken ->
                client.updatePassword(
                    authToken,
                    UpdatePasswordRequest.EmailParams(profile.email!!, incorrectVerificationCode, "NEW-PASSWORD"),
                    successWithNoContent = { fail("This test should have failed because the verification code is incorrect.") },
                    failure = { error ->
                        assertEquals("Technical Error", error.message)
                        assertEquals("invalid_grant", error.data?.error)
                        assertEquals("Invalid verification code", error.data?.errorDescription)
                    }
                )
            },
            { failWithReachFiveError(it) }
        )
    }

    @Test
    fun testFailedPasswordUpdateWithPhoneNumberAndWrongCode() = clientTest { client ->
        val profile = aProfile().copy(phoneNumber = aPhoneNumber())
        val incorrectVerificationCode = "234"
        val scope = fullWrite + openId

        client.signup(
            profile,
            scope,
            { authToken ->
                client.updatePassword(
                    authToken,
                    UpdatePasswordRequest.SmsParams(profile.phoneNumber!!, incorrectVerificationCode, "NEW-PASSWORD"),
                    successWithNoContent = { fail("This test should have failed because the verification code is incorrect.") },
                    failure = { error ->
                        assertEquals("Technical Error", error.message)
                        assertEquals("invalid_grant", error.data?.error)
                        assertEquals("Invalid verification code", error.data?.errorDescription)
                    }
                )
            },
            { failWithReachFiveError(it) }
        )
    }

    @Test
    fun testSuccessfulRequestPasswordResetWithEmail() = clientTest { client ->
        val profile = aProfile()
        val scope = fullWrite + openId

        client.signup(
            profile,
            scope,
            success = { authToken ->
                client.requestPasswordReset(
                    authToken,
                    email = profile.email!!,
                    successWithNoContent = {},
                    failure = { failWithReachFiveError(it) }
                )
            },
            failure = { failWithReachFiveError(it) }
        )
    }

    @Test
    fun testSuccessfulRequestPasswordResetWithPhoneNumber() = clientTest { client ->
        val profile = aProfile().copy(phoneNumber = aPhoneNumber())
        val scope = fullWrite + openId

        client.signup(
            profile,
            scope,
            success = { authToken ->
                client.requestPasswordReset(
                    authToken,
                    phoneNumber = profile.phoneNumber!!,
                    successWithNoContent = {},
                    failure = { failWithReachFiveError(it) }
                )
            },
            failure = { failWithReachFiveError(it) }
        )
    }

    @Test
    fun testFailedRequestPasswordResetWithNoIdentifier() = clientTest { client ->
        val profile = aProfile().copy(phoneNumber = aPhoneNumber())
        val scope = fullWrite + openId

        client.signup(
            profile,
            scope,
            success = { authToken ->
                client.requestPasswordReset(
                    authToken,
                    email = null,
                    phoneNumber = null,
                    successWithNoContent = { fail("This test should have failed because neither the email or the phone number were provided.") },
                    failure = { error ->
                        assertEquals("Technical Error", error.message)
                        assertEquals("invalid_grant", error.data?.error)
                        assertEquals("Invalid credentials", error.data?.errorDescription)
                    }
                )
            },
            failure = { failWithReachFiveError(it) }
        )
    }

    @Test
    fun testSuccessfulLogout() = clientTest { client ->
        val profile = aProfile()

        client.signup(
            profile,
            openId,
            success = { client.logout(successWithNoContent = {}, failure = { failWithReachFiveError(it) }) },
            failure = { failWithReachFiveError(it) }
        )
    }

    private fun clientTest(
        initialize: Boolean = true,
        sdkConfig: SdkConfig = defaultSdkConfig,
        block: (ReachFive) -> Unit
    ) {
        ReachFive(
            activity = activityRule.activity,
            sdkConfig = sdkConfig,
            providersCreators = listOf()
        ).also { client ->
            if (initialize) client.initialize(
                success = { block(client) },
                failure = { failWithReachFiveError(it) }
            )
            else block(client)
        }

        Unit
    }

    private val fullWrite = setOf("full_write")
    private val openId = setOf("openid")
    private val email = setOf("email")
    private val profile = setOf("profile")
    private val phone = setOf("phone")

    private fun aProfile() =
        Profile(
            givenName = "John",
            familyName = "Doe",
            gender = "male",
            email = anEmail(),
            password = "!Password123!"
        )

    private fun anEmail(): String = UUID.randomUUID().let { uuid -> "$uuid@testaccount.io" }

    private fun aPhoneNumber(international: Boolean = true): String =
        generateString(8, "0123456789")
            .let {
                if (international) "+336$it"
                else "06$it"
            }

    private fun generateString(length: Int, charset: String): String =
        (1..length)
            .map { charset[Random.nextInt(charset.length)] }
            .joinToString("")

    private fun failWithReachFiveError(e: ReachFiveError) {
        val maybeData = e.data?.let { data ->
            """
                Error: ${data.error}
                Description: ${data.errorDescription}
                Details: ${data.errorDetails
                ?.joinToString("\n", "> ") { (f, m) -> "'$f': $m" }
                ?.let { "\n$it" } ?: "N/A"
            }
            """.trimIndent()
        }

        fail("\nReason: ${e.message} $maybeData￿")
    }

    private val TEST_SHOULD_FAIL_SCOPE_MISSING =
        "This test should have failed because the 'full_write' scope is missing."
    private val NO_ID_TOKEN =
        "No id_token returned, verify that you have the `openid` scope configured in your API Client Settings."
}
