package com.reach5.identity.sdk.demo

import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import android.util.Log
import com.reach5.identity.sdk.core.ReachFive
import com.reach5.identity.sdk.core.utils.Failure
import com.reach5.identity.sdk.core.utils.Success
import io.github.cdimascio.dotenv.dotenv
import junit.framework.TestCase.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.rules.ExpectedException
import com.nhaarman.mockitokotlin2.*
import com.reach5.identity.sdk.core.models.*
import org.junit.Ignore
import java.lang.Error
import java.lang.Exception
import java.lang.Thread.sleep
import java.util.UUID
import java.util.logging.Logger
import kotlin.random.Random

/**
 * These tests use an account with:
 * - the SMS feature enabled
 * - the country set to "France"
 */
@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    private val dotenv = dotenv {
        directory = "/assets"
        filename = "env"
    }

    private lateinit var TAG: String

    private val DOMAIN = dotenv["DOMAIN"] ?: ""
    private val CLIENT_ID = dotenv["CLIENT_ID"] ?: ""

    private val TEST_SHOULD_NOT_FAIL = "This test should not have failed because the data are correct."
    private val TEST_SHOULD_FAIL_SCOPE_MISSING = "This test should have failed because the 'full_write' scope is missing."
    private val NO_ID_TOKEN = "No id_token returned, verify that you have the `openid` scope configured in your API Client Settings."

    private fun getRandomSeed() = dotenv["RANDOM_SEED"]?.let { it.toInt() } ?: Random.nextInt(1000)
    private val random: Random =
        getRandomSeed().let { seed ->
            Log.i("", "Random seed: $seed")
            TAG = "MainActivityTest-S$seed"
            Random(seed)
        }

    @get:Rule
    val activityRule = ActivityTestRule(MainActivity::class.java)

    @get:Rule
    var exceptionRule: ExpectedException = ExpectedException.none()

    @Test
    fun testSuccessfulReachFiveClientInstantiation() {
        assertNotNull(instantiateReachFiveClient())
    }

    @Test
    fun testFailedReachFiveClientInstantiation() {
        exceptionRule.expect(IllegalArgumentException::class.java)
        exceptionRule.expectMessage("Invalid URL host: \"\"")

        instantiateReachFiveClient("", CLIENT_ID)
    }

    @Test
    fun testSuccessfulSignupWithEmail() {
        val client = instantiateReachFiveClient()

        val profile = aProfile()

        client.signup(
            profile,
            success = { authToken -> assertNotNull(authToken) },
            failure = { fail(TEST_SHOULD_NOT_FAIL) }
        )

        // TODO: replace the `sleep` method by a callback mock
        sleep(1000)

        // TODO: check that the profile has received a verification email
    }

    @Test
    fun testFailedSignupWithAlreadyUsedEmail() {
        val client = instantiateReachFiveClient()

        val profile = aProfile()

        client.signup(
            profile,
            success = { authToken -> run {
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
            } },
            failure = { fail(TEST_SHOULD_NOT_FAIL) }
        )

        // TODO: replace the `sleep` method by a callback mock
        sleep(1000)

        // TODO: check that the profile has not received a verification email
    }

    @Test
    fun testFailedSignupWithEmptyEmail() {
        val client = instantiateReachFiveClient()

        val profileWithNoEmail = aProfile().copy(email = "")

        client.signup(
            profileWithNoEmail,
            success = { fail("This test should have failed because the email is empty.") },
            failure = { error -> run {
                assertEquals(error.message, "Bad Request")
                assertEquals(error.data?.error, "invalid_request")
                assertEquals(error.data?.errorDescription, "Validation failed")
                assertEquals(error.data?.errorDetails?.get(0)?.field, "data.email")
                assertEquals(error.data?.errorDetails?.get(0)?.message, "Must be a valid email")
            } }
        )

        // TODO: replace the `sleep` method by a callback mock
        sleep(1000)

        // TODO: check that the profile has not received a verification email
    }

    @Test
    fun testSuccessfulSignupWithPhoneNumber() {
        val client = instantiateReachFiveClient()

        val profile = aProfile().copy(phoneNumber = aPhoneNumber(international = true))

        client.signup(
            profile,
            success = { authToken -> assertNotNull(authToken) },
            failure = { fail(TEST_SHOULD_NOT_FAIL) }
        )

        // TODO: replace the `sleep` method by a callback mock
        sleep(1000)

        // TODO: check that the profile has received a verification SMS
    }

    @Test
    fun testSuccessfulSignupWithLocalPhoneNumber() {
        val client = instantiateReachFiveClient()

        val profile = aProfile().copy(phoneNumber = aPhoneNumber(international = false))

        client.signup(
            profile,
            success = { authToken -> assertNotNull(authToken) },
            failure = { fail(TEST_SHOULD_NOT_FAIL) }
        )

        // TODO: replace the `sleep` method by a callback mock
        sleep(1000)

        // TODO: check that the profile has received a verification SMS
    }

    @Test
    fun testFailedSignupWeakPassword() {
        val client = instantiateReachFiveClient()

        val weakPassword = "toto"
        val profile = aProfile().copy(password = weakPassword)

        client.signup(
            profile,
            success = { fail("This test should have failed because the password is too weak.") },
            failure = { error -> run {
                assertEquals(error.message, "Bad Request")
                assertEquals(error.data?.errorDescription, "Validation failed")
                assertEquals(error.data?.errorDetails?.get(0)?.message, "Password too weak")
            } }
        )

        // TODO: replace the `sleep` method by a callback mock
        sleep(1000)

        // TODO: check that the profile has not received a verification email
    }

    @Test
    fun testFailedSignupAuthTokenRetrievalWithMissingScope() {
        val client = instantiateReachFiveClient()

        val profile = aProfile()

        client.signup(
            profile,
            scope = emptyList(),
            success = { fail("This test should have failed because no 'id_token' was found.") },
            failure = { error -> assertEquals(error.message, NO_ID_TOKEN) }
        )

        // TODO: replace the `sleep` method by a callback mock
        sleep(1000)

        // TODO: check that the profile has not received a verification email
    }

    @Test
    fun testSuccessfulLoginWithEmail() {
        val client = instantiateReachFiveClient()

        val profile = aProfile()

        client.signup(
            profile,
            success = {
                client.loginWithPassword(
                    profile.email!!,
                    profile.password!!,
                    success = { authToken -> assertNotNull(authToken) },
                    failure = { fail(TEST_SHOULD_NOT_FAIL) }
                )
            },
            failure = { fail(TEST_SHOULD_NOT_FAIL) }
        )

        // TODO: replace the `sleep` method by a callback mock
        sleep(1000)
    }

    @Test
    fun testSuccessfulLoginWithPhoneNumber() {
        val client = instantiateReachFiveClient()

        val profile = aProfile().copy(phoneNumber = aPhoneNumber(international = true))

        client.signup(
            profile,
            success = {
                client.loginWithPassword(
                    profile.phoneNumber!!,
                    profile.password!!,
                    success = { authToken -> assertNotNull(authToken) },
                    failure = { fail(TEST_SHOULD_NOT_FAIL) }
                )
            },
            failure = { fail(TEST_SHOULD_NOT_FAIL) }
        )

        // TODO: replace the `sleep` method by a callback mock
        sleep(1000)
    }

    @Test
    fun testFailedLoginWithNonExistingIdentifier() {
        val client = instantiateReachFiveClient()

        client.loginWithPassword(
            "satoshi.nakamoto@testaccount.io",
            "buybitcoin",
            success = { fail("This test should have failed because the profile is not registered.") },
            failure = { error -> run {
                assertEquals(error.message, "Bad Request")
                assertEquals(error.data?.error, "invalid_grant")
                assertEquals(error.data?.errorDescription, "Invalid email or password")
            } }
        )

        // TODO: replace the `sleep` method by a callback mock
        sleep(1000)
    }

    @Test
    fun testFailedLoginWithWrongPassword() {
        val client = instantiateReachFiveClient()

        val profile = aProfile().copy(phoneNumber = aPhoneNumber(international = true))

        client.signup(
            profile,
            success = {
                client.loginWithPassword(
                    profile.phoneNumber!!,
                    "WRONG_PASSWORD",
                    success = { fail("This test should have failed because the password is incorrect.") },
                    failure = { error -> run {
                        assertEquals(error.message, "Bad Request")
                        assertEquals(error.data?.error, "invalid_grant")
                        assertEquals(error.data?.errorDescription, "Invalid phone number or password")
                    } }
                )
            },
            failure = { fail(TEST_SHOULD_NOT_FAIL) }
        )

        // TODO: replace the `sleep` method by a callback mock
        sleep(1000)
    }

    @Test
    fun testFailedLoginAuthTokenRetrievalWithMissingScope() {
        val client = instantiateReachFiveClient()

        val profile = aProfile().copy(phoneNumber = aPhoneNumber(international = true))

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
            failure = { fail(TEST_SHOULD_NOT_FAIL) }
        )

        // TODO: replace the `sleep` method by a callback mock
        sleep(1000)
    }

    @Ignore
    @Test
    fun testSuccessVerifyPhoneNumber() {
        // TODO : write this test once we can get the SMS list from Twilio
    }

    @Test
    fun testFailedVerifyPhoneNumberWithWrongCode() {
        val client = instantiateReachFiveClient()

        val profile = aProfile().copy(phoneNumber = aPhoneNumber(international = true))
        val incorrectVerificationCode = "500"

        client.signup(
            profile,
            client.defaultScope.plus("full_write"),
            success = { authToken ->
                client.verifyPhoneNumber(
                    authToken,
                    profile.phoneNumber!!,
                    incorrectVerificationCode,
                    { fail("This test should have failed because the verification code is incorrect.") },
                    { error -> run {
                        assertEquals(error.message, "Technical Error")
                        assertEquals(error.data?.error, "invalid_grant")
                        assertEquals(error.data?.errorDescription, "Invalid verification code")
                    } }
                )
            },
            failure = { fail(TEST_SHOULD_NOT_FAIL) }
        )

        // TODO: replace the `sleep` method by a callback mock
        sleep(1000)
    }

    @Test
    fun testSuccessfulEmailUpdate() {
        val client = instantiateReachFiveClient()

        val profile = aProfile()
        val newEmail = anEmail()

        client.signup(
            profile,
            client.defaultScope.plus("full_write"),
            { authToken ->
                client.updateEmail(
                    authToken,
                    newEmail,
                    success = { updatedProfile -> run {
                        assertNotNull(updatedProfile)
                        assertEquals(updatedProfile.email, newEmail)
                    } },
                    failure = { fail(TEST_SHOULD_NOT_FAIL) }
                )
            },
            { fail(TEST_SHOULD_NOT_FAIL) }
        )

        // TODO: replace the `sleep` method by a callback mock
        sleep(1000)
    }

    @Test
    fun testFailedEmailUpdateWithSameEmail() {
        val client = instantiateReachFiveClient()

        val profile = aProfile()

        client.signup(
            profile,
            client.defaultScope.plus("full_write"),
            { authToken ->
                client.updateEmail(
                    authToken,
                    profile.email!!,
                    success = { fail("This test should have failed because the email has not changed.") },
                    failure = { error -> run {
                        assertEquals(error.message, "Bad Request")
                        assertEquals(error.data?.error, "email_already_exists")
                        assertEquals(error.data?.errorDescription, "Email already in use")
                    } }
                )
            },
            { fail(TEST_SHOULD_NOT_FAIL) }
        )

        // TODO: replace the `sleep` method by a callback mock
        sleep(1000)
    }

    @Test
    fun testFailedEmailUpdateWithMissingScope() {
        val client = instantiateReachFiveClient()

        val profile = aProfile()
        val newEmail = anEmail()

        client.signup(
            profile,
            success = { authToken ->
                client.updateEmail(
                    authToken,
                    newEmail,
                    success = { fail(TEST_SHOULD_FAIL_SCOPE_MISSING) },
                    failure =  { error -> run {
                        assertEquals(error.message, "Technical Error")
                        assertEquals(error.data?.error, "insufficient_scope")
                        assertEquals(error.data?.errorDescription, "The token does not contain the required scope: full_write")
                    } }
                )
            },
            failure = { fail(TEST_SHOULD_NOT_FAIL) }
        )

        // TODO: replace the `sleep` method by a callback mock
        sleep(1000)
    }

    @Test
    fun testSuccessfulPhoneNumberUpdate() {
        val client = instantiateReachFiveClient()

        val profile = aProfile()
        val newNumber = aPhoneNumber(international = true)

        client.signup(
            profile,
            client.defaultScope.plus("full_write"),
            { authToken ->
                client.updatePhoneNumber(
                    authToken,
                    newNumber,
                    success = { updatedProfile -> run {
                        assertNotNull(updatedProfile)
                        assertEquals(updatedProfile.phoneNumber, newNumber)
                    } },
                    failure = { fail(TEST_SHOULD_NOT_FAIL) }
                )
            },
            { fail(TEST_SHOULD_NOT_FAIL) }
        )

        // TODO: replace the `sleep` method by a callback mock
        sleep(1000)
    }

    @Test
    fun testSuccessfulPhoneNumberUpdateWithSameNumber() {
        val client = instantiateReachFiveClient()

        val profile = aProfile().copy(phoneNumber = aPhoneNumber(international = true))

        client.signup(
            profile,
            client.defaultScope.plus("full_write"),
            { authToken ->
                client.updatePhoneNumber(
                    authToken,
                    profile.phoneNumber!!,
                    success = { updatedProfile -> run {
                        assertNotNull(updatedProfile)
                        assertEquals(updatedProfile.phoneNumber, profile.phoneNumber!!)
                    } },
                    failure = { fail(TEST_SHOULD_NOT_FAIL) }
                )
            },
            { fail(TEST_SHOULD_NOT_FAIL) }
        )

        // TODO: replace the `sleep` method by a callback mock
        sleep(1000)
    }

    @Test
    fun testFailedPhoneNumberUpdateWithMissingScope() {
        val client = instantiateReachFiveClient()

        val profile = aProfile().copy(phoneNumber = aPhoneNumber(international = true))
        val newNumber = aPhoneNumber(international = true)

        client.signup(
            profile,
            success = { authToken ->
                client.updatePhoneNumber(
                    authToken,
                    newNumber,
                    success = { fail(TEST_SHOULD_FAIL_SCOPE_MISSING) },
                    failure =  { error -> run {
                        assertEquals(error.message, "Technical Error")
                        assertEquals(error.data?.error, "insufficient_scope")
                        assertEquals(error.data?.errorDescription, "The token does not contain the required scope: full_write")
                    } }
                )
            },
            failure = { fail(TEST_SHOULD_NOT_FAIL) }
        )

        // TODO: replace the `sleep` method by a callback mock
        sleep(1000)
    }

    @Test
    fun testSuccessfulProfileUpdate() {
        val client = instantiateReachFiveClient()

        val profile = aProfile()
        val updatedGivenName = "Christelle"
        val updatedFamilyName = "Couet"

        client.signup(
            profile,
            client.defaultScope.plus("full_write"),
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
                    { fail(TEST_SHOULD_NOT_FAIL) }
                )
            },
            { fail(TEST_SHOULD_NOT_FAIL) }
        )

        // TODO: replace the `sleep` method by a callback mock
        sleep(1000)
    }

    @Test
    fun testFailedProfileUpdateWithMissingScope() {
        val client = instantiateReachFiveClient()

        val profile = aProfile()

        client.signup(
            profile,
            success = { authToken ->
                client
                    .updateProfile(
                        authToken,
                        Profile(givenName = "Peter"),
                        { fail(TEST_SHOULD_FAIL_SCOPE_MISSING) },
                        { error -> run {
                            assertEquals(error.message, "Technical Error")
                            assertEquals(error.data?.error, "insufficient_scope")
                            assertEquals(error.data?.errorDescription, "The token does not contain the required scope: full_write")
                        } }
                    )
            },
            failure = { fail(TEST_SHOULD_NOT_FAIL) }
        )

        // TODO: replace the `sleep` method by a callback mock
        sleep(1000)
    }

    @Test
    fun testSuccessfulPasswordUpdateWithFreshAccessToken() {
        val client = instantiateReachFiveClient()

        val profile = aProfile()
        val newPassword = "ZPf7LFtc"

        client.signup(
            profile,
            client.defaultScope.plus("full_write"),
            { authToken ->
                client.updatePassword(
                    authToken,
                    UpdatePasswordRequest.FreshAccessTokenParams(newPassword),
                    successWithNoContent = {
                        client.loginWithPassword(
                            profile.email!!,
                            newPassword,
                            success = { authToken -> assertNotNull(authToken) },
                            failure = { fail(TEST_SHOULD_NOT_FAIL) }
                        )
                    },
                    failure = { fail(TEST_SHOULD_NOT_FAIL) }
                )
            },
            { fail(TEST_SHOULD_NOT_FAIL) }
        )

        // TODO: replace the `sleep` method by a callback mock
        sleep(2000)
    }

    @Test
    fun testSuccessfulPasswordUpdateWithAccessToken() {
        val client = instantiateReachFiveClient()

        val profile = aProfile()
        val newPassword = "XLpYXz7z"

        client.signup(
            profile,
            client.defaultScope.plus("full_write"),
            { authToken ->
                client.updatePassword(
                    authToken,
                    UpdatePasswordRequest.AccessTokenParams(profile.password!!, newPassword),
                    successWithNoContent = {
                        client.loginWithPassword(
                            profile.email!!,
                            newPassword,
                            success = { authToken -> assertNotNull(authToken) },
                            failure = { fail(TEST_SHOULD_NOT_FAIL) }
                        )
                    },
                    failure = { fail(TEST_SHOULD_NOT_FAIL) }
                )
            },
            { fail(TEST_SHOULD_NOT_FAIL) }
        )

        // TODO: replace the `sleep` method by a callback mock
        sleep(2000)
    }

    @Test
    fun testFailedPasswordUpdateWithAccessTokenWithSamePassword() {
        val client = instantiateReachFiveClient()

        val profile = aProfile()

        client.signup(
            profile,
            client.defaultScope.plus("full_write"),
            { authToken ->
                client.updatePassword(
                    authToken,
                    UpdatePasswordRequest.AccessTokenParams(profile.password!!, profile.password!!),
                    successWithNoContent = { fail("This test should have failed because the password has not changed.") },
                    failure = { error -> run {
                        assertEquals(error.message, "Bad Request")
                        assertEquals(error.data?.error, "invalid_request")
                        assertEquals(error.data?.errorDescription, "New password should be different from the old password")
                    } }
                )
            },
            { fail(TEST_SHOULD_NOT_FAIL) }
        )

        // TODO: replace the `sleep` method by a callback mock
        sleep(1000)
    }

    @Test
    fun testFailedPasswordUpdateWithEmailAndWrongCode() {
        val client = instantiateReachFiveClient()

        val profile = aProfile()
        val incorrectVerificationCode = "234"

        client.signup(
            profile,
            client.defaultScope.plus("full_write"),
            { authToken ->
                client.updatePassword(
                    authToken,
                    UpdatePasswordRequest.EmailParams(profile.email!!, incorrectVerificationCode, "NEW-PASSWORD"),
                    successWithNoContent = { fail("This test should have failed because the verification code is incorrect.") },
                    failure = { error -> run {
                        assertEquals(error.message, "Technical Error")
                        assertEquals(error.data?.error, "invalid_grant")
                        assertEquals(error.data?.errorDescription, "Invalid verification code")
                    } }
                )
            },
            { fail(TEST_SHOULD_NOT_FAIL) }
        )

        // TODO: replace the `sleep` method by a callback mock
        sleep(1000)
    }

    @Test
    fun testFailedPasswordUpdateWithPhoneNumberAndWrongCode() {
        val client = instantiateReachFiveClient()

        val profile = aProfile().copy(phoneNumber = aPhoneNumber(international = true))
        val incorrectVerificationCode = "234"

        client.signup(
            profile,
            client.defaultScope.plus("full_write"),
            { authToken ->
                client.updatePassword(
                    authToken,
                    UpdatePasswordRequest.SmsParams(profile.phoneNumber!!, incorrectVerificationCode, "NEW-PASSWORD"),
                    successWithNoContent = { fail("This test should have failed because the verification code is incorrect.") },
                    failure = { error -> run {
                        assertEquals(error.message, "Technical Error")
                        assertEquals(error.data?.error, "invalid_grant")
                        assertEquals(error.data?.errorDescription, "Invalid verification code")
                    } }
                )
            },
            { fail(TEST_SHOULD_NOT_FAIL) }
        )

        // TODO: replace the `sleep` method by a callback mock
        sleep(1000)
    }

    @Test
    fun testSuccessfulRequestPasswordResetWithEmail() {
        val client = instantiateReachFiveClient()

        val profile = aProfile()

        client.signup(
            profile,
            success = { authToken ->
                client.requestPasswordReset(
                    authToken,
                    email = profile.email!!,
                    successWithNoContent = {},
                    failure = { fail(TEST_SHOULD_NOT_FAIL) }
                )
            },
            failure = { fail(TEST_SHOULD_NOT_FAIL) }
        )

        // TODO: replace the `sleep` method by a callback mock
        sleep(1000)

        // TODO: check that the profile has received an email
    }

    @Test
    fun testSuccessfulRequestPasswordResetWithPhoneNumber() {
        val client = instantiateReachFiveClient()

        val profile = aProfile().copy(phoneNumber = aPhoneNumber(international = true))

        client.signup(
            profile,
            success = { authToken ->
                client.requestPasswordReset(
                    authToken,
                    phoneNumber = profile.phoneNumber!!,
                    successWithNoContent = {},
                    failure = { fail(TEST_SHOULD_NOT_FAIL) }
                )
            },
            failure = { fail(TEST_SHOULD_NOT_FAIL) }
        )

        // TODO: replace the `sleep` method by a callback mock
        sleep(1000)

        // TODO: check that the profile has received an SMS
    }

    @Test
    fun testFailedRequestPasswordResetWithNoIdentifier() {
        val client = instantiateReachFiveClient()

        val profile = aProfile().copy(phoneNumber = aPhoneNumber(international = true))

        client.signup(
            profile,
            success = { authToken ->
                client.requestPasswordReset(
                    authToken,
                    email = null,
                    phoneNumber = null,
                    successWithNoContent = { fail("This test should have failed because neither the email or the phone number were provided.") },
                    failure = { error -> run {
                        assertEquals(error.message, "Technical Error")
                        assertEquals(error.data?.error, "invalid_grant")
                        assertEquals(error.data?.errorDescription, "Invalid credentials")
                    } }
                )
            },
            failure = { fail(TEST_SHOULD_NOT_FAIL) }
        )

        // TODO: replace the `sleep` method by a callback mock
        sleep(1000)
    }

    @Test
    fun testSuccessfulLogout() {
        val client = instantiateReachFiveClient()

        val profile = aProfile()

        client.signup(
            profile,
            success = { client.logout(successWithNoContent = {}, failure = { fail(TEST_SHOULD_NOT_FAIL) }) },
            failure = { fail(TEST_SHOULD_NOT_FAIL) }
        )

        // TODO: replace the `sleep` method by a callback mock
        sleep(1000)
    }

    private fun instantiateReachFiveClient(domain: String = DOMAIN, clientId: String = CLIENT_ID): ReachFive {
        val sdkConfig = SdkConfig(domain = domain, clientId = clientId)

        return ReachFive(
            activity = activityRule.activity,
            sdkConfig = sdkConfig,
            providersCreators = listOf()
        ).initialize()
    }

    private fun aProfile() =
        Profile(
            givenName = "John",
            familyName = "Doe",
            gender = "male",
            email = anEmail(),
            password = "!Password123!"
        )

    private fun anEmail(): String = UUID.randomUUID().let { uuid -> "$uuid@testaccount.io" }

    private fun aPhoneNumber(international: Boolean = false): String =
        random
            .nextInt(10000000, 99999999)
            .let {
                if (international) "+336$it"
                else "07$it"
            }
            .also {
                Log.d(TAG, "Genered phone number: $it")
            }
}

