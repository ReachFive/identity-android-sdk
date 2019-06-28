package com.reach5.identity.sdk.demo

import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import com.reach5.identity.sdk.core.ReachFive
import com.reach5.identity.sdk.core.utils.Failure
import com.reach5.identity.sdk.core.utils.Success
import junit.framework.TestCase.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.rules.ExpectedException
import com.nhaarman.mockitokotlin2.*
import com.reach5.identity.sdk.core.models.*
import java.lang.Error
import java.lang.Exception
import java.lang.Thread.sleep

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    private val DOMAIN = "sdk-mobile-sandbox.reach5.net"
    private val CLIENT_ID = "TYAIHFRJ2a1FGJ1T8pKD"

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

        instantiateReachFiveClient("","TYAIHFRJ2a1FGJ1T8pKD")
    }

    @Test
    fun testSuccessfulSignupWithEmail() {
        val client = instantiateReachFiveClient()

        client.signup(
            Profile(
                givenName = "John",
                familyName = "Doe",
                gender = "male",
                email = "test_john.doe@gmail.com",
                password = "hjk90wxc"
            ),
            { authToken -> assertNotNull(authToken) },
            { fail("This test should have failed because the data are correct.") }
        )

        // TODO: replace the `sleep` method by a callback mock
        sleep(1000)
    }

    @Test
    fun testFailedSignupWithAlreadyUsedEmail() {
        val client = instantiateReachFiveClient()

        val EMAIL = "test_sylvie.lamour@gmail.com"
        val PASSWORD = "trcnjrn89"

        client.signup(
            Profile(
                givenName = "Sylvie",
                familyName = "Lamour",
                gender = "female",
                addresses = listOf(ProfileAddress(country = "France")),
                email = EMAIL,
                password = PASSWORD
            ),
            { authToken -> run {
                // Check that the returned authentication token is not null
                assertNotNull(authToken)

                client.signup(
                    Profile(email = EMAIL, password = PASSWORD),
                    { fail("This test should have failed because the email is already used.") },
                    { error ->
                        run {
                            assertEquals(error.message, "Bad Request")
                            assertEquals(error.data?.error, "email_already_exists")
                            assertEquals(error.data?.errorDescription, "Email already in use")
                        }
                    }
                )
            } },
            { fail("This test should have failed because the data are correct.") }
        )

        // TODO: replace the `sleep` method by a callback mock
        sleep(1000)
    }

    @Test
    fun testFailedSignupWithEmptyEmail() {
        val client = instantiateReachFiveClient()

//        val mocked = object {
//            var onSuccess: Success<AuthToken> = mock()
//        }

//        whenever(mocked.onSuccess(any())).thenThrow(Exception("This test should have failed because the email is empty."))

        client.signup(
            Profile(email = "", password = "jdhkzkzk"),
//            { a -> mocked.onSuccess(a) },
            //mock.onFailure
            { fail("This test should have failed because the email is empty.") },
            { error -> run {
                assertEquals(error.message, "Bad Request")
                assertEquals(error.data?.error, "invalid_request")
                assertEquals(error.data?.errorDescription, "Validation failed")
                assertEquals(error.data?.errorDetails?.get(0)?.field, "data.email")
                assertEquals(error.data?.errorDetails?.get(0)?.message, "Must be a valid email")
            } }
        )

        // TODO: replace the `sleep` method by a callback mock
        sleep(1000)

        //val verify = verify(mock, timeout(1000).times(1))

        //verify.onSuccess(check { fail("This test should have failed because the email is empty.") })
        //verify.onFailure(ReachFiveError(message = "Bad Request"))
    }

    @Test
    fun testFailedSignupWithWeakPassword() {
        val client = instantiateReachFiveClient()

        client.signup(
            Profile(email = "test_marshall.babin@gmail.fr", password = "toto"),
            { fail("This test should have failed because the password is too weak.") },
            { error -> run {
                assertEquals(error.message, "Bad Request")
                assertEquals(error.data?.errorDescription, "Validation failed")
                assertEquals(error.data?.errorDetails?.get(0)?.message, "Password too weak")
            } }
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

}

