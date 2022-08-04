package co.reachfive.identity.sdk.core

import co.reachfive.identity.sdk.core.ProfileManagement.Companion.fields
import co.reachfive.identity.sdk.core.api.ReachFiveApi
import co.reachfive.identity.sdk.core.api.ReachFiveApiCallback
import co.reachfive.identity.sdk.core.models.AuthToken
import co.reachfive.identity.sdk.core.models.Profile
import co.reachfive.identity.sdk.core.models.ReachFiveError
import co.reachfive.identity.sdk.core.models.SdkInfos
import co.reachfive.identity.sdk.core.models.requests.UpdateEmailRequest
import co.reachfive.identity.sdk.core.models.requests.UpdatePhoneNumberRequest
import co.reachfive.identity.sdk.core.models.requests.VerifyPhoneNumberRequest
import co.reachfive.identity.sdk.core.utils.Failure
import co.reachfive.identity.sdk.core.utils.Success
import co.reachfive.identity.sdk.core.utils.SuccessWithNoContent

internal class ProfileManagementClient(
    private val reachFiveApi: ReachFiveApi,
) : ProfileManagement {

    override fun getProfile(
        authToken: AuthToken,
        success: Success<Profile>,
        failure: Failure<ReachFiveError>
    ) {
        reachFiveApi
            .getProfile(
                authToken.authHeader,
                SdkInfos.getQueries().plus(Pair("fields", fields.joinToString(",")))
            )
            .enqueue(ReachFiveApiCallback(success = success, failure = failure))
    }

    override fun verifyPhoneNumber(
        authToken: AuthToken,
        phoneNumber: String,
        verificationCode: String,
        successWithNoContent: SuccessWithNoContent,
        failure: Failure<ReachFiveError>
    ) {
        reachFiveApi
            .verifyPhoneNumber(
                authToken.authHeader,
                VerifyPhoneNumberRequest(phoneNumber, verificationCode),
                SdkInfos.getQueries()
            )
            .enqueue(
                ReachFiveApiCallback(
                    successWithNoContent = successWithNoContent,
                    failure = failure
                )
            )
    }

    override fun updateEmail(
        authToken: AuthToken,
        email: String,
        redirectUrl: String?,
        success: Success<Profile>,
        failure: Failure<ReachFiveError>
    ) {
        reachFiveApi
            .updateEmail(
                authToken.authHeader,
                UpdateEmailRequest(email, redirectUrl), SdkInfos.getQueries()
            )
            .enqueue(ReachFiveApiCallback(success = success, failure = failure))
    }

    override fun updatePhoneNumber(
        authToken: AuthToken,
        phoneNumber: String,
        success: Success<Profile>,
        failure: Failure<ReachFiveError>
    ) {
        reachFiveApi
            .updatePhoneNumber(
                authToken.authHeader,
                UpdatePhoneNumberRequest(phoneNumber),
                SdkInfos.getQueries()
            )
            .enqueue(ReachFiveApiCallback(success = success, failure = failure))
    }

    override fun updateProfile(
        authToken: AuthToken,
        profile: Profile,
        success: Success<Profile>,
        failure: Failure<ReachFiveError>
    ) {
        reachFiveApi
            .updateProfile(authToken.authHeader, profile, SdkInfos.getQueries())
            .enqueue(ReachFiveApiCallback(success = success, failure = failure))
    }
}

internal interface ProfileManagement {
    companion object {
        val fields = arrayOf(
            "addresses",
            "auth_types",
            "bio",
            "birthdate",
            "company",
            "consents",
            "created_at",
            "custom_fields",
            "devices",
            "email",
            "emails",
            "email_verified",
            "external_id",
            "family_name",
            "first_login",
            "first_name",
            "full_name",
            "gender",
            "given_name",
            "has_managed_profile",
            "has_password",
            "id",
            "identities",
            "last_login",
            "last_login_provider",
            "last_login_type",
            "last_name",
            "likes_friends_ratio",
            "lite_only",
            "locale",
            "local_friends_count",
            "login_summary",
            "logins_count",
            "middle_name",
            "name",
            "nickname",
            "origins",
            "phone_number",
            "phone_number_verified",
            "picture",
            "provider_details",
            "providers",
            "social_identities",
            "sub",
            "uid",
            "updated_at"
        )
    }

    fun getProfile(
        authToken: AuthToken,
        success: Success<Profile>,
        failure: Failure<ReachFiveError>
    )

    fun verifyPhoneNumber(
        authToken: AuthToken,
        phoneNumber: String,
        verificationCode: String,
        successWithNoContent: SuccessWithNoContent,
        failure: Failure<ReachFiveError>
    )

    fun updateEmail(
        authToken: AuthToken,
        email: String,
        redirectUrl: String? = null,
        success: Success<Profile>,
        failure: Failure<ReachFiveError>
    )

    fun updatePhoneNumber(
        authToken: AuthToken,
        phoneNumber: String,
        success: Success<Profile>,
        failure: Failure<ReachFiveError>
    )

    fun updateProfile(
        authToken: AuthToken,
        profile: Profile,
        success: Success<Profile>,
        failure: Failure<ReachFiveError>
    )
}
