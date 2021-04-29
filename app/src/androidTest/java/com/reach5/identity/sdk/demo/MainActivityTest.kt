package com.reach5.identity.sdk.demo

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.rule.ActivityTestRule
import com.reach5.identity.sdk.core.ReachFive
import com.reach5.identity.sdk.core.models.*
import com.reach5.identity.sdk.core.models.requests.ProfileSignupRequest
import com.reach5.identity.sdk.core.models.requests.UpdatePasswordRequest
import io.github.cdimascio.dotenv.dotenv
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.runner.RunWith
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.random.Random

/**
 * These tests use an account with:
 * - the SMS feature enabled
 * - the country set to "France"
 * - the following ENFORCED scope: ['email', 'full_write', 'openid', 'phone', 'profile', 'offline_access', 'address']
 */
@RunWith(AndroidJUnit4ClassRunner::class)
class MainActivityTest {
    private val dotenv = dotenv {
        directory = "/assets"
        filename = "env"
    }

    private val DOMAIN = dotenv["DOMAIN"] ?: ""
    private val CLIENT_ID = dotenv["CLIENT_ID"] ?: ""
    private val SCHEME = dotenv["SCHEME"] ?: ""

    private val defaultSdkConfig: SdkConfig = SdkConfig(DOMAIN, CLIENT_ID, SCHEME)

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
            sdkConfig = SdkConfig("", CLIENT_ID, SCHEME)
        ) { client, _ ->
            client.initialize()
        }
    }

    @Test
    fun testSuccessfulClientConfigFetch() = clientTest(initialize = false) { client, passTest ->
        val profile = aProfile()

        // given an uninitialized client
        client.signup(
            profile,
            /*scope = setOf("openid"),*/ // WHEN INITIALIZED: becomes setOf('email', 'full_write', 'openid', 'phone', 'profile', 'offline_access', 'address')
            success = { authToken ->
                // Signup success but no id_token returned
                assertNull(authToken.idToken)

                client
                    .initialize()
                    .signup(
                        profile,
                        success = { fail("This test should have failed because the email is now already used.") },
                        failure = { error ->
                            assertEquals("email_already_exists", error.data?.error)
                            assertEquals("Email already in use", error.data?.errorDescription)
                            passTest()
                        }
                    )
            },
            failure = { failWithReachFiveError(it) }
        )
    }

    @Test
    fun testSuccessfulSignupWithEmail() = clientTest { client, passTest ->
        val profile = aProfile()

        client.signup(
            profile,
            scope = openId,
            success = { passTest() },
            failure = { failWithReachFiveError(it) }
        )
    }

    @Test
    fun testFailedSignupWithAlreadyUsedEmail() = clientTest { client, passTest ->
        val profile = aProfile()

        client.signup(
            profile,
            scope = openId,
            success = { authToken ->
                // Check that the returned authentication token is not null
                assertNotNull(authToken)

                client.signup(
                    ProfileSignupRequest(email = profile.email, password = profile.password),
                    scope = openId,
                    success = { fail("This test should have failed because the email should be already used.") },
                    failure = { error ->
                        assertEquals("email_already_exists", error.data?.error)
                        assertEquals("Email already in use", error.data?.errorDescription)
                        passTest()
                    }
                )
            },
            failure = { failWithReachFiveError(it) }
        )
    }

    @Test
    fun testFailedSignupWithEmptyEmail() = clientTest { client, passTest ->
        val profileWithNoEmail = aProfile().copy(email = "")

        client.signup(
            profileWithNoEmail,
            scope = openId,
            success = { fail("This test should have failed because the email is empty.") },
            failure = { error ->
                assertEquals("invalid_request", error.data?.error)
                assertEquals("Validation failed", error.data?.errorDescription)
                assertEquals("data.email", error.data?.errorDetails?.get(0)?.field)
                assertEquals("Must be a valid email", error.data?.errorDetails?.get(0)?.message)
                passTest()
            }
        )
    }

    @Test
    fun testSuccessfulSignupWithPhoneNumber() = clientTest { client, passTest ->
        val profile = aProfile().copy(phoneNumber = aPhoneNumber())

        client.signup(
            profile,
            scope = openId,
            success = { passTest() },
            failure = { failWithReachFiveError(it) }
        )
    }

    @Test
    fun testSuccessfulSignupWithLocalPhoneNumber() = clientTest { client, passTest ->
        val profile = aProfile().copy(phoneNumber = aPhoneNumber(international = false))

        client.signup(
            profile,
            scope = openId,
            success = { passTest() },
            failure = { failWithReachFiveError(it) }
        )
    }

    @Test
    fun testSuccessfulSignupWithAddress() = clientTest { client, passTest ->
        val addresses = listOf(
            ProfileAddress(
                title = "Home",
                isDefault = true,
                addressType = ProfileAddressType.billing
            ),
            ProfileAddress(title = "Work", isDefault = false)
        )
        val theProfile = aProfile().copy(addresses = addresses)
        val scope = openId + email + profile + address

        client.signup(
            theProfile,
            scope = scope,
            success = { authToken ->
                assertNotNull(authToken)

                client.getProfile(
                    authToken,
                    success = {
                        val addressesIterator = addresses.listIterator()
                        for ((index, _) in addressesIterator.withIndex()) {
                            val expectedAddress = addresses[index]
                            val actualAddress = it.addresses?.get(index)

                            assertEquals(expectedAddress.title, actualAddress?.title)
                            assertEquals(expectedAddress.isDefault, actualAddress?.isDefault)
                            assertEquals(expectedAddress.addressType, actualAddress?.addressType)
                            assertEquals(
                                expectedAddress.streetAddress,
                                actualAddress?.streetAddress
                            )
                            assertEquals(expectedAddress.locality, actualAddress?.locality)
                            assertEquals(expectedAddress.region, actualAddress?.region)
                            assertEquals(expectedAddress.postalCode, actualAddress?.postalCode)
                            assertEquals(expectedAddress.country, actualAddress?.country)
                            assertEquals(expectedAddress.raw, actualAddress?.raw)
                            assertEquals(expectedAddress.deliveryNote, actualAddress?.deliveryNote)
                            assertEquals(expectedAddress.recipient, actualAddress?.recipient)
                            assertEquals(expectedAddress.company, actualAddress?.company)
                            assertEquals(expectedAddress.phoneNumber, actualAddress?.phoneNumber)
                        }
                        passTest()
                    },
                    failure = { failWithReachFiveError(it) }
                )
            },
            failure = { failWithReachFiveError(it) }
        )
    }

    @Test
    fun testFailedSignupWeakPassword() = clientTest { client, passTest ->
        val weakPassword = "toto"
        val profile = aProfile().copy(password = weakPassword)

        client.signup(
            profile,
            scope = openId,
            success = { fail("This test should have failed because the password is too weak.") },
            failure = { error ->
                assertEquals("Validation failed", error.data?.errorDescription)
                assertEquals("Password too weak", error.data?.errorDetails?.get(0)?.message)
                passTest()
            }
        )
    }

    @Test
    fun testSuccessfulLoginWithEmail() = clientTest { client, passTest ->
        val profile = aProfile()

        client.signup(
            profile,
            scope = openId,
            success = {
                client.loginWithPassword(
                    profile.email!!,
                    profile.password,
                    scope = openId,
                    success = { passTest() },
                    failure = { failWithReachFiveError(it) }
                )
            },
            failure = { failWithReachFiveError(it) }
        )
    }

    @Test
    fun testSuccessfulLoginWithPhoneNumber() = clientTest { client, passTest ->
        val profile = aProfile().copy(phoneNumber = aPhoneNumber())

        client.signup(
            profile,
            scope = openId,
            success = {
                client.loginWithPassword(
                    profile.phoneNumber!!,
                    profile.password,
                    scope = openId,
                    success = { passTest() },
                    failure = { failWithReachFiveError(it) }
                )
            },
            failure = { failWithReachFiveError(it) }
        )
    }

    @Test
    fun testFailedLoginWithNonExistingIdentifier() = clientTest { client, passTest ->
        client.loginWithPassword(
            "satoshi.nakamoto@testaccount.io",
            "buybitcoin",
            scope = openId,
            success = { fail("This test should have failed because the profile is not registered.") },
            failure = { error ->
                assertEquals("invalid_grant", error.data?.error)
                assertEquals("Invalid email or password", error.data?.errorDescription)
                passTest()
            }
        )
    }

    @Test
    fun testFailedLoginWithWrongPassword() = clientTest { client, passTest ->
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
                        assertEquals("invalid_grant", error.data?.error)
                        assertEquals(
                            "Invalid phone number or password",
                            error.data?.errorDescription
                        )
                        passTest()
                    }
                )
            },
            failure = { failWithReachFiveError(it) }
        )
    }

    @Test
    fun testSuccessfulGetProfile() = clientTest { client, passTest ->
        val theProfile = aProfile()
        val scope = openId + email + profile

        client.signup(
            theProfile,
            scope,
            success = { authToken ->
                client.getProfile(
                    authToken,
                    { fetchedProfile ->
                        assertEquals(theProfile.givenName, fetchedProfile.givenName)
                        assertEquals(theProfile.familyName, fetchedProfile.familyName)
                        assertEquals(theProfile.gender, fetchedProfile.gender)
                        assertEquals(theProfile.email, fetchedProfile.email)
                        passTest()
                    },
                    { error -> failWithReachFiveError(error) }
                )
            },
            failure = { failWithReachFiveError(it) }
        )
    }

    @Test
    fun testSuccessfulGetProfileWithCustomFields() = clientTest { client, passTest ->
        val customFields = mapOf<String, Any>(
            Pair("test_string", "toto"),
            Pair("mobile_number1", aPhoneNumber())
        )
        val theProfile = aProfile().copy(customFields = customFields)
        val scope = openId + email + profile

        client.signup(
            theProfile,
            scope = scope,
            success = { authToken ->
                assertNotNull(authToken)

                client.getProfile(
                    authToken,
                    success = {
                        assertNotNull(it.customFields)
                        assertEquals(
                            customFields["test_string"],
                            it.customFields?.get("test_string")
                        )
                        assertEquals(
                            customFields["mobile_number1"],
                            it.customFields?.get("mobile_number1")
                        )
                        passTest()
                    },
                    failure = { failWithReachFiveError(it) }
                )
            },
            failure = { failWithReachFiveError(it) }
        )
    }

    @Test
    fun testFailedGetProfileWithMissingScopes() = clientTest { client, passTest ->
        val theProfile = aProfile()
        val scope = openId + email

        client.signup(
            theProfile,
            scope,
            success = { authToken ->
                client.getProfile(
                    authToken,
                    { fetchedProfile ->
                        // Since the `profile` scope is missing, the personal data is not returned
                        assertNull(fetchedProfile.givenName)
                        assertNull(fetchedProfile.familyName)
                        assertNull(fetchedProfile.gender)
                        assertEquals(theProfile.email, fetchedProfile.email)
                        passTest()
                    },
                    { error -> failWithReachFiveError(error) }
                )
            },
            failure = { failWithReachFiveError(it) }
        )
    }

    @Test
    fun testFailedVerifyPhoneNumberWithWrongCode() = clientTest { client, passTest ->
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
                        assertEquals("invalid_grant", error.data?.error)
                        assertEquals("Invalid verification code", error.data?.errorDescription)
                        passTest()
                    }
                )
            },
            failure = { failWithReachFiveError(it) }
        )
    }

    @Test
    fun testSuccessfulEmailUpdate() = clientTest { client, passTest ->
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
                        passTest()
                    },
                    failure = { failWithReachFiveError(it) }
                )
            },
            { failWithReachFiveError(it) }
        )
    }

    @Test
    fun testFailedEmailUpdateWithSameEmail() = clientTest { client, passTest ->
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
                        assertEquals("email_already_exists", error.data?.error)
                        assertEquals("Email already in use", error.data?.errorDescription)
                        assertEquals("error.email.alreadyInUse", error.data?.errorMessageKey)
                        passTest()
                    }
                )
            },
            { failWithReachFiveError(it) }
        )
    }

    @Test
    fun testFailedEmailUpdateWithMissingScope() = clientTest { client, passTest ->
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
                        assertEquals("insufficient_scope", error.data?.error)
                        assertEquals(
                            "The token does not contain the required scope: full_write",
                            error.data?.errorDescription
                        )
                        passTest()
                    }
                )
            },
            failure = { failWithReachFiveError(it) }
        )
    }

    @Test
    fun testSuccessfulPhoneNumberUpdate() = clientTest { client, passTest ->
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
                        passTest()
                    },
                    failure = { failWithReachFiveError(it) }
                )
            },
            { failWithReachFiveError(it) }
        )
    }

    @Test
    fun testSuccessfulPhoneNumberUpdateWithSameNumber() = clientTest { client, passTest ->
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
                        passTest()
                    },
                    failure = { failWithReachFiveError(it) }
                )
            },
            { failWithReachFiveError(it) }
        )
    }

    @Test
    fun testFailedPhoneNumberUpdateWithMissingScope() = clientTest { client, passTest ->
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
                        assertEquals("insufficient_scope", error.data?.error)
                        assertEquals(
                            "The token does not contain the required scope: full_write",
                            error.data?.errorDescription
                        )
                        passTest()
                    }
                )
            },
            failure = { failWithReachFiveError(it) }
        )
    }

    @Test
    fun testSuccessfulProfileUpdate() = clientTest { client, passTest ->
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
                            passTest()
                        },
                        { failWithReachFiveError(it) }
                    )
            },
            { failWithReachFiveError(it) }
        )
    }

    @Test
    fun testFailedProfileUpdateWithMissingScope() = clientTest { client, passTest ->
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
                            assertEquals("insufficient_scope", error.data?.error)
                            assertEquals(
                                "The token does not contain the required scope: full_write",
                                error.data?.errorDescription
                            )
                            passTest()
                        }
                    )
            },
            failure = { failWithReachFiveError(it) }
        )
    }

    @Test
    fun testSuccessfulPasswordUpdateWithFreshAccessToken() = clientTest { client, passTest ->
        val profile = aProfile()
        val newPassword = "ZPf7LFtc"
        val scope = fullWrite + openId

        client.signup(
            profile,
            scope,
            { authToken ->
                client.updatePassword(
                    UpdatePasswordRequest.FreshAccessTokenParams(authToken, newPassword),
                    successWithNoContent = {
                        client.loginWithPassword(
                            profile.email!!,
                            newPassword,
                            success = { passTest() },
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
    fun testSuccessfulPasswordUpdateWithAccessToken() = clientTest { client, passTest ->
        val profile = aProfile()
        val newPassword = "XLpYXz7z"
        val scope = fullWrite + openId

        client.signup(
            profile,
            scope,
            { authToken ->
                client.updatePassword(
                    UpdatePasswordRequest.AccessTokenParams(
                        authToken,
                        profile.password,
                        newPassword
                    ),
                    successWithNoContent = {
                        client.loginWithPassword(
                            profile.email!!,
                            newPassword,
                            success = { passTest() },
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
    fun testFailedPasswordUpdateWithAccessTokenWithSamePassword() = clientTest { client, passTest ->
        val profile = aProfile()
        val scope = fullWrite + openId

        client.signup(
            profile,
            scope,
            { authToken ->
                client.updatePassword(
                    UpdatePasswordRequest.AccessTokenParams(
                        authToken,
                        profile.password,
                        profile.password
                    ),
                    successWithNoContent = { fail("This test should have failed because the password has not changed.") },
                    failure = { error ->
                        assertEquals("invalid_request", error.data?.error)
                        assertEquals(
                            "New password should be different from the old password",
                            error.data?.errorDescription
                        )
                        passTest()
                    }
                )
            },
            { failWithReachFiveError(it) }
        )
    }

    @Test
    fun testFailedPasswordUpdateWithEmailAndWrongCode() = clientTest { client, passTest ->
        val profile = aProfile()
        val incorrectVerificationCode = "234"
        val scope = fullWrite + openId

        client.signup(
            profile,
            scope,
            {
                client.updatePassword(
                    UpdatePasswordRequest.EmailParams(
                        profile.email!!,
                        incorrectVerificationCode,
                        "NEW-PASSWORD"
                    ),
                    successWithNoContent = { fail("This test should have failed because the verification code is incorrect.") },
                    failure = { error ->
                        assertEquals("invalid_grant", error.data?.error)
                        assertEquals("Invalid verification code", error.data?.errorDescription)
                        passTest()
                    }
                )
            },
            { failWithReachFiveError(it) }
        )
    }

    @Test
    fun testFailedPasswordUpdateWithPhoneNumberAndWrongCode() = clientTest { client, passTest ->
        val profile = aProfile().copy(phoneNumber = aPhoneNumber())
        val incorrectVerificationCode = "234"
        val scope = fullWrite + openId

        client.signup(
            profile,
            scope,
            {
                client.updatePassword(
                    UpdatePasswordRequest.SmsParams(
                        profile.phoneNumber!!,
                        incorrectVerificationCode,
                        "NEW-PASSWORD"
                    ),
                    successWithNoContent = { fail("This test should have failed because the verification code is incorrect.") },
                    failure = { error ->
                        assertEquals("invalid_grant", error.data?.error)
                        assertEquals("Invalid verification code", error.data?.errorDescription)
                        passTest()
                    }
                )
            },
            { failWithReachFiveError(it) }
        )
    }

    @Test
    fun testSuccessfulRequestPasswordResetWithEmail() = clientTest { client, passTest ->
        val profile = aProfile()
        val scope = fullWrite + openId

        client.signup(
            profile,
            scope,
            success = {
                client.requestPasswordReset(
                    email = profile.email!!,
                    successWithNoContent = { passTest() },
                    failure = { failWithReachFiveError(it) }
                )
            },
            failure = { failWithReachFiveError(it) }
        )
    }

    @Test
    fun testSuccessfulRequestPasswordResetWithPhoneNumber() = clientTest { client, passTest ->
        val profile = aProfile().copy(phoneNumber = aPhoneNumber())
        val scope = fullWrite + openId

        client.signup(
            profile,
            scope,
            success = {
                client.requestPasswordReset(
                    phoneNumber = profile.phoneNumber!!,
                    successWithNoContent = { passTest() },
                    failure = { failWithReachFiveError(it) }
                )
            },
            failure = { failWithReachFiveError(it) }
        )
    }

    @Test
    fun testFailedRequestPasswordResetWithNoIdentifier() = clientTest { client, passTest ->
        val profile = aProfile().copy(phoneNumber = aPhoneNumber())
        val scope = fullWrite + openId

        client.signup(
            profile,
            scope,
            success = {
                client.requestPasswordReset(
                    email = null,
                    phoneNumber = null,
                    successWithNoContent = { fail("This test should have failed because neither the email or the phone number were provided.") },
                    failure = { error ->
                        assertEquals("invalid_grant", error.data?.error)
                        assertEquals("Invalid credentials", error.data?.errorDescription)
                        passTest()
                    }
                )
            },
            failure = { failWithReachFiveError(it) }
        )
    }

    @Test
    fun testSuccessfulLogout() = clientTest { client, passTest ->
        val profile = aProfile()

        client.signup(
            profile,
            openId,
            success = {
                client.logout(
                    successWithNoContent = { passTest() },
                    failure = { failWithReachFiveError(it) }
                )
            },
            failure = { failWithReachFiveError(it) }
        )
    }

    @Test
    fun testSuccessfulRefresh() = clientTest { client, passTest ->
        val profile = aProfile()

        client.signup(
            profile,
            scope = email + offline,
            success = { authToken ->
                assertNotNull(authToken.refreshToken)

                // wait 1 second to avoid regenerating the same access token
                Thread.sleep(1000)

                client.refreshAccessToken(
                    authToken = authToken,
                    success = { newAuthToken ->
                        assertNotNull(newAuthToken.refreshToken)
                        assertNotEquals(
                            "Server should have generated a new access token",
                            authToken.accessToken,
                            newAuthToken.accessToken
                        )
                        passTest()
                    },
                    failure = { failWithReachFiveError(it) }
                )
            },
            failure = { failWithReachFiveError(it) }
        )
    }

    private fun clientTest(
        initialize: Boolean = true,
        sdkConfig: SdkConfig = defaultSdkConfig,
        block: (ReachFive, () -> Unit) -> Unit
    ) =
        runBlocking {
            withTimeout(10000) {
                suspendCancellableCoroutine<Unit> { continuation ->
                    val successLatch: () -> Unit = { continuation.resume(Unit) }

                    GlobalScope.launch(
                        CoroutineExceptionHandler { _, exception ->
                            continuation.resumeWithException(exception)
                        }
                    ) {
                        ReachFive(
                            activity = activityRule.activity,
                            sdkConfig = sdkConfig,
                            providersCreators = listOf()
                        ).also { client ->
                            if (initialize) client.initialize(
                                success = { block(client, successLatch) },
                                failure = { error -> continuation.resumeWithException(error) }
                            )
                            else block(client, successLatch)
                        }

                        Unit
                    }
                }
            }
        }

    private val fullWrite = setOf("full_write")
    private val openId = setOf("openid")
    private val email = setOf("email")
    private val profile = setOf("profile")
    private val address = setOf("address")
    private val phone = setOf("phone")
    private val offline = setOf("offline_access")

    private fun aProfile() =
        ProfileSignupRequest(
            givenName = "John",
            familyName = "Doe",
            gender = "male",
            email = anEmail(),
            password = "IAMNOTAWEAKPASSWORD!!!"
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
                Error message key: ${data.errorMessageKey}
                Details: ${
                data.errorDetails
                    ?.joinToString("\n", "> ") { (f, m) -> "'$f': $m" }
                    ?.let { "\n$it" } ?: "N/A"
            }
            """.trimIndent()
        }

        fail("\nReason: ${e.message} \n$maybeData￿")
    }

    private val TEST_SHOULD_FAIL_SCOPE_MISSING =
        "This test should have failed because the 'full_write' scope is missing."
}
