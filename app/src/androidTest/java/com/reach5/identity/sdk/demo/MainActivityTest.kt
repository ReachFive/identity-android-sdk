package com.reach5.identity.sdk.demo

import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import com.reach5.identity.sdk.core.ReachFive
import com.reach5.identity.sdk.core.models.Profile
import com.reach5.identity.sdk.core.models.ReachFiveError
import com.reach5.identity.sdk.core.models.SdkConfig
import com.reach5.identity.sdk.core.models.UpdatePasswordRequest
import io.github.cdimascio.dotenv.dotenv
import junit.framework.TestCase.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.runner.RunWith
import java.lang.Thread.sleep
import java.util.*
import kotlin.random.Random

/**
 * These tests use an account with:
 * - the SMS feature enabled
 * - the country set to "France"
 * - the following ENFORCED scope: ['email', 'full_write', 'openid', 'phone', 'profile']
 *
 * TODO:
 * - replace sleep(1000) with a better mechanism to wait for tests
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

    private fun getRandomSeed() = dotenv["RANDOM_SEED"]?.toInt() ?: Random.nextInt(1000)
    private val random: Random = Random(getRandomSeed())

    @get:Rule
    val activityRule = ActivityTestRule(MainActivity::class.java)

    @get:Rule
    var exceptionRule: ExpectedException = ExpectedException.none()

    @Test
    fun testFailedReachFiveClientInitialization() =
        clientTest(
            initialize = false,
            sdkConfig = SdkConfig("", CLIENT_ID)
        ) { client ->
            exceptionRule.expect(IllegalArgumentException::class.java)
            exceptionRule.expectMessage("Invalid URL host: \"\"")

            client.initialize(
                success = { fail("Should have failed initializatio due to missing domain.") },
                failure = { failWithReachFiveError(it) }
            )

            sleep(1000)
        }

    @Test
    fun testSuccessfulClientConfigFetch() = clientTest(initialize = false) { client ->
        val profile = aProfile()

        client.signup(
            profile,
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
            success = { authToken -> assertNotNull(authToken) },
            failure = { failWithReachFiveError(it) }
        )

    }

    @Test
    fun testFailedSignupWithAlreadyUsedEmail() = clientTest { client ->

        val profile = aProfile()

        client.signup(
            profile,
            success = { authToken ->
                run {
                    // Check that the returned authentication token is not null
                    assertNotNull(authToken)

                    client.signup(
                        Profile(email = profile.email, password = profile.email),
                        success = { fail("This test should have failed because the email should be already used.") },
                        failure = { error ->
                            run {
                                assertEquals(error.message, "Bad Request")
                                assertEquals(error.data?.error, "email_already_exists")
                                assertEquals(error.data?.errorDescription, "Email already in use")
                            }
                        }
                    )
                }
            },
            failure = { failWithReachFiveError(it) }
        )

    }

    @Test
    fun testFailedSignupWithEmptyEmail() = clientTest { client ->

        val profileWithNoEmail = aProfile().copy(email = "")

        client.signup(
            profileWithNoEmail,
            success = { fail("This test should have failed because the email is empty.") },
            failure = { error ->
                run {
                    assertEquals(error.message, "Bad Request")
                    assertEquals(error.data?.error, "invalid_request")
                    assertEquals(error.data?.errorDescription, "Validation failed")
                    assertEquals(error.data?.errorDetails?.get(0)?.field, "data.email")
                    assertEquals(error.data?.errorDetails?.get(0)?.message, "Must be a valid email")
                }
            }
        )

    }

    @Test
    fun testSuccessfulSignupWithPhoneNumber() = clientTest { client ->

        val profile = aProfile().copy(phoneNumber = aPhoneNumber())

        client.signup(
            profile,
            success = { authToken -> assertNotNull(authToken) },
            failure = { failWithReachFiveError(it) }
        )

    }

    @Test
    fun testSuccessfulSignupWithLocalPhoneNumber() = clientTest { client ->

        val profile = aProfile().copy(phoneNumber = aPhoneNumber(international = false))

        client.signup(
            profile,
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
            success = { fail("This test should have failed because the password is too weak.") },
            failure = { error ->
                run {
                    assertEquals(error.message, "Bad Request")
                    assertEquals(error.data?.errorDescription, "Validation failed")
                    assertEquals(error.data?.errorDetails?.get(0)?.message, "Password too weak")
                }
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
            failure = { error -> assertEquals(error.message, NO_ID_TOKEN) }
        )

    }

    @Test
    fun testSuccessfulLoginWithEmail() = clientTest { client ->

        val profile = aProfile()

        client.signup(
            profile,
            success = {
                client.loginWithPassword(
                    profile.email!!,
                    profile.password!!,
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
            success = {
                client.loginWithPassword(
                    profile.phoneNumber!!,
                    profile.password!!,
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
            success = { fail("This test should have failed because the profile is not registered.") },
            failure = { error ->
                run {
                    assertEquals(error.message, "Bad Request")
                    assertEquals(error.data?.error, "invalid_grant")
                    assertEquals(error.data?.errorDescription, "Invalid email or password")
                }
            }
        )

    }

    @Test
    fun testFailedLoginWithWrongPassword() = clientTest { client ->

        val profile = aProfile().copy(phoneNumber = aPhoneNumber())

        client.signup(
            profile,
            success = {
                client.loginWithPassword(
                    profile.phoneNumber!!,
                    "WRONG_PASSWORD",
                    success = { fail("This test should have failed because the password is incorrect.") },
                    failure = { error ->
                        run {
                            assertEquals(error.message, "Bad Request")
                            assertEquals(error.data?.error, "invalid_grant")
                            assertEquals(error.data?.errorDescription, "Invalid phone number or password")
                        }
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
            fullWriteScope,
            success = { authToken ->
                client.verifyPhoneNumber(
                    authToken,
                    profile.phoneNumber!!,
                    incorrectVerificationCode,
                    { fail("This test should have failed because the verification code is incorrect.") },
                    { error ->
                        run {
                            assertEquals(error.message, "Technical Error")
                            assertEquals(error.data?.error, "invalid_grant")
                            assertEquals(error.data?.errorDescription, "Invalid verification code")
                        }
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

        client.signup(
            profile,
            fullWriteScope,
            { authToken ->
                client.updateEmail(
                    authToken,
                    newEmail,
                    success = { updatedProfile ->
                        run {
                            assertNotNull(updatedProfile)
                            assertEquals(updatedProfile.email, newEmail)
                        }
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

        client.signup(
            profile,
            fullWriteScope,
            { authToken ->
                client.updateEmail(
                    authToken,
                    profile.email!!,
                    success = { fail("This test should have failed because the email has not changed.") },
                    failure = { error ->
                        run {
                            assertEquals(error.message, "Bad Request")
                            assertEquals(error.data?.error, "email_already_exists")
                            assertEquals(error.data?.errorDescription, "Email already in use")
                        }
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
            success = { authToken ->
                client.updateEmail(
                    authToken,
                    newEmail,
                    success = { fail(TEST_SHOULD_FAIL_SCOPE_MISSING) },
                    failure = { error ->
                        run {
                            assertEquals(error.message, "Technical Error")
                            assertEquals(error.data?.error, "insufficient_scope")
                            assertEquals(
                                error.data?.errorDescription,
                                "The token does not contain the required scope: full_write"
                            )
                        }
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

        client.signup(
            profile,
            fullWriteScope,
            { authToken ->
                client.updatePhoneNumber(
                    authToken,
                    newNumber,
                    success = { updatedProfile ->
                        run {
                            assertNotNull(updatedProfile)
                            assertEquals(updatedProfile.phoneNumber, newNumber)
                        }
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

        client.signup(
            profile,
            fullWriteScope,
            { authToken ->
                client.updatePhoneNumber(
                    authToken,
                    profile.phoneNumber!!,
                    success = { updatedProfile ->
                        run {
                            assertNotNull(updatedProfile)
                            assertEquals(updatedProfile.phoneNumber, profile.phoneNumber!!)
                        }
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
            success = { authToken ->
                client.updatePhoneNumber(
                    authToken,
                    newNumber,
                    success = { fail(TEST_SHOULD_FAIL_SCOPE_MISSING) },
                    failure = { error ->
                        run {
                            assertEquals(error.message, "Technical Error")
                            assertEquals(error.data?.error, "insufficient_scope")
                            assertEquals(
                                error.data?.errorDescription,
                                "The token does not contain the required scope: full_write"
                            )
                        }
                    }
                )
            },
            failure = { failWithReachFiveError(it) }
        )

    }

    @Test
    fun testSuccessfulProfileUpdate() = clientTest { client ->

        val profile = aProfile()
        val updatedGivenName = "Christelle"
        val updatedFamilyName = "Couet"

        client.signup(
            profile,
            fullWriteScope,
            { authToken ->
                client
                    .updateProfile(
                        authToken,
                        Profile(givenName = updatedGivenName, familyName = updatedFamilyName),
                        { updatedProfile ->
                            run {
                                assertNotNull(updatedProfile)
                                assertEquals(updatedProfile.email, profile.email)
                                assertEquals(updatedProfile.givenName, updatedGivenName)
                                assertEquals(updatedProfile.familyName, updatedFamilyName)
                                assertEquals(updatedProfile.gender, profile.gender!!)
                            }
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
            success = { authToken ->
                client
                    .updateProfile(
                        authToken,
                        Profile(givenName = "Peter"),
                        { fail(TEST_SHOULD_FAIL_SCOPE_MISSING) },
                        { error ->
                            run {
                                assertEquals(error.message, "Technical Error")
                                assertEquals(error.data?.error, "insufficient_scope")
                                assertEquals(
                                    error.data?.errorDescription,
                                    "The token does not contain the required scope: full_write"
                                )
                            }
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

        client.signup(
            profile,
            fullWriteScope,
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

        sleep(2000)
    }

    @Test
    fun testSuccessfulPasswordUpdateWithAccessToken() = clientTest { client ->

        val profile = aProfile()
        val newPassword = "XLpYXz7z"

        client.signup(
            profile,
            fullWriteScope,
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

        sleep(2000)
    }

    @Test
    fun testFailedPasswordUpdateWithAccessTokenWithSamePassword() = clientTest { client ->

        val profile = aProfile()

        client.signup(
            profile,
            fullWriteScope,
            { authToken ->
                client.updatePassword(
                    authToken,
                    UpdatePasswordRequest.AccessTokenParams(profile.password!!, profile.password!!),
                    successWithNoContent = { fail("This test should have failed because the password has not changed.") },
                    failure = { error ->
                        run {
                            assertEquals(error.message, "Bad Request")
                            assertEquals(error.data?.error, "invalid_request")
                            assertEquals(
                                error.data?.errorDescription,
                                "New password should be different from the old password"
                            )
                        }
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

        client.signup(
            profile,
            fullWriteScope,
            { authToken ->
                client.updatePassword(
                    authToken,
                    UpdatePasswordRequest.EmailParams(profile.email!!, incorrectVerificationCode, "NEW-PASSWORD"),
                    successWithNoContent = { fail("This test should have failed because the verification code is incorrect.") },
                    failure = { error ->
                        run {
                            assertEquals(error.message, "Technical Error")
                            assertEquals(error.data?.error, "invalid_grant")
                            assertEquals(error.data?.errorDescription, "Invalid verification code")
                        }
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

        client.signup(
            profile,
            fullWriteScope,
            { authToken ->
                client.updatePassword(
                    authToken,
                    UpdatePasswordRequest.SmsParams(profile.phoneNumber!!, incorrectVerificationCode, "NEW-PASSWORD"),
                    successWithNoContent = { fail("This test should have failed because the verification code is incorrect.") },
                    failure = { error ->
                        run {
                            assertEquals(error.message, "Technical Error")
                            assertEquals(error.data?.error, "invalid_grant")
                            assertEquals(error.data?.errorDescription, "Invalid verification code")
                        }
                    }
                )
            },
            { failWithReachFiveError(it) }
        )

    }

    @Test
    fun testSuccessfulRequestPasswordResetWithEmail() = clientTest { client ->

        val profile = aProfile()

        client.signup(
            profile,
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

        client.signup(
            profile,
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

        client.signup(
            profile,
            success = { authToken ->
                client.requestPasswordReset(
                    authToken,
                    email = null,
                    phoneNumber = null,
                    successWithNoContent = { fail("This test should have failed because neither the email or the phone number were provided.") },
                    failure = { error ->
                        run {
                            assertEquals(error.message, "Technical Error")
                            assertEquals(error.data?.error, "invalid_grant")
                            assertEquals(error.data?.errorDescription, "Invalid credentials")
                        }
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
            success = { client.logout(successWithNoContent = {}, failure = { failWithReachFiveError(it) }) },
            failure = { failWithReachFiveError(it) }
        )
    }

    private fun clientTest(
        initialize: Boolean = true,
        sdkConfig: SdkConfig = defaultSdkConfig,
        block: (ReachFive) -> Unit
    ) =
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
            .run {
            }

    private val fullWriteScope = setOf("full_write")

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
        random
            .nextInt(10000000, 99999999)
            .let {
                if (international) "+336$it"
                else "07$it"
            }

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

