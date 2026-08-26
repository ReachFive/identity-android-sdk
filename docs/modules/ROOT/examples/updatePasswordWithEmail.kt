import com.reach5.identity.sdk.core.models.requests.UpdatePasswordRequest.*

client.updatePassword(
    updatePasswordRequest = UpdatePasswordRequest.EmailParams("john.doe@example.com", "234", "ZPf7LFtc"),
    success = { _ -> ... }, // Do something
    failure = { error -> ... } // Handle a ReachFive error
)
