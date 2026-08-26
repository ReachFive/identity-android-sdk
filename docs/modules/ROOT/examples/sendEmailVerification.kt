import com.reach5.identity.sdk.core.models.AuthToken

client.sendEmailVerification(
    authToken = authToken,
    success = { emailVerification ->
        // Handle success: Verification email sent
    },
    failure = { error ->
        // Handle a ReachFive error
    }
)