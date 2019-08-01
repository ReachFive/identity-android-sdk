# Changelog

## v5.0.0 (01/08/2019)

### Breaking changes

- The default scopes for the `signup` and `loginWithPassword` methods used to be `openid`, `email`, `phone` and `profile`.
They are now the allowed scopes set up in your client's configuration through the console.
- All the data models used for requests were moved in a sub-folder named `requests` (`com.reach5.identity.sdk.core.models` -> `com.reach5.identity.sdk.core.models.requests`).
- The profile's data passed as an argument to the `signup` method is no longer a `Profile` but a [`ProfileSignupRequest`](https://developer.reach5.co/api/identity-android/#profile-signup-request).
Thus `ProfileSignupRequest` provides the same attributes than `Profile` without the identifiers and authentication details and with the `password` field.
- The `User` data model was renamed [`OpenIdUser`](https://developer.reach5.co/api/identity-android/#openid-user).
- The `AuthToken.idToken` field became optional since it's not returned if the `openid` scope is not provided.
- The `openid` scope is no longer provided by default to the `loginWithProvider` method through the `WebViewProvider`.
- The `logout` method can now also be used to disconnect sessions created with a provider.

### Features

- An [HTTP logging interceptor](https://github.com/square/okhttp/tree/master/okhttp-logging-interceptor) was added to the `sdk-core` module to log the API requests and responses in debug mode.
- New fields were added to the `Profile` data model. You'll find their descriptions on the [ReachFive developer documentation](https://developer.reach5.co/api/identity-android/#profile).
    - `uid`
    - `signedUid`
    - `profileURL`
    - `externalId`
    - `authTypes`
    - `loginSummary`
    - `emailVerified`
    - `emails`
    - `phoneNumberVerified`
    - `bio `
    - `customFields`
    - `consents`
    - `tosAcceptedAt`
    - `createdAt`
    - `updatedAt`
    - `liteOnly`
- New fields were also added to the `ProfileAddress` data model.
- A new method [`getProfile`](https://developer.reach5.co/api/identity-android/#get-profile) was implemented to fetch the profile's information.

### Fixes

- The `requestPasswordReset` no longer needs the `authToken` argument.
    
## v4.2.0 (22/07/2019)

### Features

- All the SDK core models are now serialized for an Android implementation.
- The authorization flow with code and the PKCE are implemented for a login with the `WebViewProvider`.

## v4.1.0 (12/07/2019)

### Features

- A new method [`updatePassword`](https://developer.reach5.co/api/identity-android/#update-password) was implemented to update the profile's password.
- A new method [`logout`](https://developer.reach5.co/api/identity-android/#update-password) was implemented to kill the SSO session of the profile.

### Fixes

- The `success` parameter of the `requestPasswordReset` and `verifyPhoneNumber` methods was renamed into `successWithNoContent` and is now correctly called.

## v4.0.0 (07/07/2019)

### Changes

New modular version of the Identity SDK Android:

- [`sdk-core`](sdk-core)
- [`sdk-webview`](sdk-webview)
- [`sdk-facebook`](sdk-facebook)
- [`sdk-google`](sdk-google)
