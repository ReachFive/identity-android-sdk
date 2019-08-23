package com.reach5.identity.sdk.demo

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import com.reach5.identity.sdk.core.ReachFive
import com.reach5.identity.sdk.core.models.Profile
import com.reach5.identity.sdk.core.models.ProfileAddress
import com.reach5.identity.sdk.core.models.ReachFiveError
import com.reach5.identity.sdk.core.models.SdkConfig
import com.reach5.identity.sdk.core.models.requests.ProfileSignupRequest
import com.reach5.identity.sdk.core.models.requests.UpdatePasswordRequest
import io.github.cdimascio.dotenv.dotenv
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.runner.RunWith
import java.util.UUID
import kotlin.random.Random

/**
 * These tests use an account with:
 * - the SMS feature enabled
 * - the country set to "France"
 * - the following ENFORCED scope: ['email', 'full_write', 'openid', 'phone', 'profile', 'offline_access', 'address']
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
                        }
                    )
            },
            failure = { failWithReachFiveError(it) }
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
                    ProfileSignupRequest(email = profile.email, password = profile.password),
                    scope = openId,
                    success = { fail("This test should have failed because the email should be already used.") },
                    failure = { error ->
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
    fun testSuccessfulSignupWithAddress() = clientTest { client ->
        val addresses = listOf(
            ProfileAddress(title = "Home", isDefault = true, addressType = null, streetAddress = null, locality = null,
                region = null, postalCode = null, country = null, raw = null, deliveryNote = null, recipient = null, company = null,
                phoneNumber = null),
            ProfileAddress(title = "Work", isDefault = false, addressType = null, streetAddress = null, locality = null,
                region = null, postalCode = null, country = null, raw = null, deliveryNote = null, recipient = null, company = null,
                phoneNumber = null)
        )
        val theProfile = aProfile().copy(givenName = "Titti", addresses = addresses)
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
                            assertEquals(expectedAddress.streetAddress, actualAddress?.streetAddress)
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
                    },
                    failure = { failWithReachFiveError(it) }
                )
            },
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
                assertEquals("Validation failed", error.data?.errorDescription)
                assertEquals("Password too weak", error.data?.errorDetails?.get(0)?.message)
            }
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
                    profile.password,
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
                    profile.password,
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
                        assertEquals("invalid_grant", error.data?.error)
                        assertEquals("Invalid phone number or password", error.data?.errorDescription)
                    }
                )
            },
            failure = { failWithReachFiveError(it) }
        )
    }

    @Test
    fun testSuccessfulGetProfile() = clientTest { client ->
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
                    },
                    { error -> failWithReachFiveError(error) }
                )
            },
            failure = { failWithReachFiveError(it) }
        )
    }

    @Test
    fun testFailedGetProfileWithMissingScopes() = clientTest { client ->
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
                    },
                    { error -> failWithReachFiveError(error) }
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
                    UpdatePasswordRequest.FreshAccessTokenParams(authToken, newPassword),
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
                    UpdatePasswordRequest.AccessTokenParams(authToken, profile.password, newPassword),
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
                    UpdatePasswordRequest.AccessTokenParams(authToken, profile.password, profile.password),
                    successWithNoContent = { fail("This test should have failed because the password has not changed.") },
                    failure = { error ->
                        assertEquals("invalid_request", error.data?.error)
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
            {
                client.updatePassword(
                    UpdatePasswordRequest.EmailParams(profile.email!!, incorrectVerificationCode, "NEW-PASSWORD"),
                    successWithNoContent = { fail("This test should have failed because the verification code is incorrect.") },
                    failure = { error ->
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
            {
                client.updatePassword(
                    UpdatePasswordRequest.SmsParams(profile.phoneNumber!!, incorrectVerificationCode, "NEW-PASSWORD"),
                    successWithNoContent = { fail("This test should have failed because the verification code is incorrect.") },
                    failure = { error ->
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
            success = {
                client.requestPasswordReset(
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
            success = {
                client.requestPasswordReset(
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
            success = {
                client.requestPasswordReset(
                    email = null,
                    phoneNumber = null,
                    successWithNoContent = { fail("This test should have failed because neither the email or the phone number were provided.") },
                    failure = { error ->
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

    @Test
    fun testSuccessfulRefresh() = clientTest { client ->
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
                        assertNotEquals("Server should have generated a new access token", authToken.accessToken, newAuthToken.accessToken)
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
    private val address = setOf("address")
    private val phone = setOf("phone")
    private val offline = setOf("offline_access")

    private fun aProfile() =
        ProfileSignupRequest(
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
}
