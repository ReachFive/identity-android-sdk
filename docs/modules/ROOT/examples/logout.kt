client.logout(
    success = { _ -> 
        // Handle successful logout, e.g., navigate to login screen
    },
    failure = { error -> 
        // Handle ReachFive error, e.g., show error message
    },
    tokens = authToken, // Revokes access/refresh tokens if provided
    ssoCustomTab = activityInstance // Opens Custom Tab to clear browser's SSO session if provided
)
