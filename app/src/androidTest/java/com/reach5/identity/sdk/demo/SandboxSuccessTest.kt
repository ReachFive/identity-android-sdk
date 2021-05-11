package com.reach5.identity.sdk.demo

import android.view.View
import android.view.ViewGroup
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.allOf
import org.hamcrest.TypeSafeMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*
import java.util.regex.Pattern

@LargeTest
@RunWith(AndroidJUnit4::class)
class SandboxSuccessTest {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityTestRule(MainActivity::class.java)
    private fun anEmail(): String = UUID.randomUUID().let { uuid -> "$uuid@testaccount.io" }
    val newEmail = anEmail()
    val newPassword = "P@ssw0rd"
    val charPool : List<Char> = "0123456789".toList()
    val newPhone = "12345679".toList().random() + (1..8)
            .map { i -> kotlin.random.Random.nextInt(0, charPool.count()) }
            .map(charPool::get)
            .joinToString("");

    @Test
    fun sandboxSuccessTest() {
        // signup with email
        val appCompatEditText = onView(
                allOf(
                        withId(R.id.email),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(`is`("android.widget.LinearLayout")),
                                        0
                                ),
                                2
                        )
                )
        )

        appCompatEditText.perform(
                scrollTo(),
                replaceText(newEmail),
                closeSoftKeyboard()
        )

        val appCompatEditText2 = onView(
                allOf(
                        withId(R.id.password),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(`is`("android.widget.LinearLayout")),
                                        0
                                ),
                                4
                        )
                )
        )
        appCompatEditText2.perform(scrollTo(), replaceText(newPassword), closeSoftKeyboard())

        val appCompatButton = onView(
                allOf(
                        withId(R.id.passwordSignup), withText("Signup"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(`is`("android.widget.LinearLayout")),
                                        1
                                ),
                                1
                        )
                )
        )
        Thread.sleep(2_000)
        appCompatButton.perform(scrollTo(), click())

        val textView = onView(
                allOf(
                        withId(R.id.user_email),
                        withParent(withParent(withId(android.R.id.content))),
                        isDisplayed()
                )
        )
        Thread.sleep(4_000)
        textView.check(matches(isDisplayed()))
        textView.check(matches(withText(newEmail)))
        Espresso.pressBack()

        // login with email
        val appCompatLoginButton = onView(
                allOf(
                        withId(R.id.passwordLogin), withText("Login"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(`is`("android.widget.LinearLayout")),
                                        1
                                ),
                                0
                        )
                )
        )
        Thread.sleep(2_000)
        appCompatLoginButton.perform(scrollTo(), click())

        val textViewLogin = onView(
                allOf(
                        withId(R.id.user_email),
                        withParent(withParent(withId(android.R.id.content))),
                        isDisplayed()
                )
        )
        Thread.sleep(4_000)
        textViewLogin.check(matches(isDisplayed()))
        textViewLogin.check(matches(withText(newEmail)))

        // signup with phone
        Espresso.pressBack()
        appCompatEditText.perform(
                scrollTo(),
                replaceText(""),
                closeSoftKeyboard()
        )

        val appCompatEditTextPhone = onView(
                allOf(
                        withId(R.id.phoneNumber),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(`is`("android.widget.LinearLayout")),
                                        0
                                ),
                                3
                        )
                )
        )

        appCompatEditTextPhone.perform(
                scrollTo(),
                replaceText(newPhone),
                closeSoftKeyboard()
        )
        Thread.sleep(2_000)
        appCompatButton.perform(scrollTo(), click())

        val textViewPhone = onView(
                allOf(
                        withId(R.id.user_phone_number),
                        withParent(withParent(withId(android.R.id.content))),
                        isDisplayed()
                )
        )
        Thread.sleep(4_000)
        textViewPhone.check(matches(isDisplayed()))
        textViewPhone.check(matches(withText("+33"+newPhone)))

        // login with phone
        Thread.sleep(2_000)
        Espresso.pressBack()
        appCompatLoginButton.perform(scrollTo(), click())

        Thread.sleep(4_000)
        textViewPhone.check(matches(isDisplayed()))
        textViewPhone.check(matches(withText("+33"+newPhone)))

    }

    private fun childAtPosition(
            parentMatcher: Matcher<View>, position: Int
    ): Matcher<View> {

        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("Child at position $position in parent ")
                parentMatcher.describeTo(description)
            }

            public override fun matchesSafely(view: View): Boolean {
                val parent = view.parent
                return parent is ViewGroup && parentMatcher.matches(parent)
                        && view == parent.getChildAt(position)
            }
        }
    }
}
