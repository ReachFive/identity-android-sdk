# Changelog

## v6.0.1 (13/11/2020)

### Fixes

- Fix the exchange of an authorization code with an access token at the end of a Webauthn workflow.
- Fix the Google login through a Webview.

## v6.0.0 (04/11/2020)

### Latest changes

The format expected for your redirection scheme has changed to `reachfive://${clientId}/callback`.

Some updates are required if you are using the [Webview SDK](https://developer.reachfive.com/sdk-android/index.html#sdk-webview):

1. Update the scheme required by the `SdkConfig` object for the [SDK initialization](https://developer.reachfive.com/sdk-android/index.html#initialization). 
In our example, the value is stored in the `env` file.

2. Update the scheme in the *Allowed Callback URLs* section of your Identity client on the ReachFive console. 

### Features

You can now implement a biometric authentication flow.

> Follow our [FIDO2](https://developer.reachfive.com/sdk-android/fido2.html) guide for more information on the configuration and methods.

## v5.7.0 (01/07/2020)

### Features

The `errorMessageKey` field is now returned in the error response object. See for example [`updateEmail`](https://developer.reachfive.com/sdk-android/updateEmail.html#reachfiveerror) to view the documentation of the new field.

## v5.6.0 (26/06/2020)

### Features

Add the `scope` parameter to the [`loginWithProvider`](https://developer.reachfive.com/sdk-android/loginWithProvider.html) method.

## v5.5.0 (15/06/2020)

### Changes

- App-specific scheme handling (pattern `reachfive-${clientId}://callback`). This custom scheme has to be specified in `AndroidManifest.xml` application and passed during SDK configuration in `SdkConfig` object:
```
DOMAIN=my-reachfive-url
CLIENT_ID=my-reachfive-client-id
SCHEME=my-reachfive-url-scheme
```

```kotlin
val sdkConfig = SdkConfig(
  domain = DOMAIN,
  clientId = CLIENT_ID,
  scheme = SCHEME
)
```

- This custom scheme will be used as a redirect URL by default in payload of Start Passwordless call.

## v5.4.6 (10/12/2019)

## v5.4.5 (09/12/2019)

## v5.4.4 (08/11/2019)

### Fixes

The following fields are now returned by the [`getProfile`](https://developer.reach5.co/api/identity-android/#get-profile) method: `bio`, `birthdate`, `company`, `external_id`, `locale`, `middle_name`, `nickname`, `picture` and `tos_accepted_at`.

## v5.4.3 (27/09/2019)

### Fixes

The `consents`, `emails`, `email_verified`, `phone_number` and `phone_number_verified` fields are returned again by the [`getProfile`](https://developer.reach5.co/api/identity-android/#get-profile) method.

## v5.4.2 (26/09/2019)

### Fixes

The custom fields are now returned by the [`getProfile`](https://developer.reach5.co/api/identity-android/#get-profile) method.

## v5.4.1 (23/09/2019)

### Changes

- The `redirectUrl` is configurable for the method [`startPasswordless`](https://developer.reach5.co/api/identity-android/#start-passwordless).

## v5.4.0 (18/09/2019)

### Features

- A new method [`startPasswordless`](https://developer.reach5.co/api/identity-android/#start-passwordless) was implemented to trigger an email/sms sending for a passwordless flow.
- A new method [`verifyPasswordless`](https://developer.reach5.co/api/identity-android/#verify-passwordless) was implemented to verify a passwordless sms verification code.
- A new method [`exchangeCodeForToken`](https://developer.reach5.co/api/identity-android/#exchange-code-for-token) was implemented to exchange an authorization code for an access token.

## v5.3.2 (28/08/2019)

### Fixes

- The `signedUid` attribute of the `Profile` model was removed.
- Login with the Facebook social account was fixed.

## v5.3.1 (23/08/2019)

### Fixes

- The `ProfileAddress` model's `isDefault` attribute is now correctly serialized.
- All the string attributes of the `ProfileAddress` model are now null by default.
- The `addressType` attribute of the `ProfileAddress` model is no longer a `String` but a `ProfileAddressType`.

## v5.3.0 (22/08/2019)

### Features

The ReachFive error models were improved:
- the `code` attribute was added to the [`ReachFiveError`](https://developer.reach5.co/api/identity-android/#reach5-error) model to specify the HTTP code response.
- the `errorId` and the `errorUserMsg` attributes were added to the [`ReachFiveApiError`](https://developer.reach5.co/api/identity-android/#reach5-api-error) model to specify the identifier and the user-friendly message of the error.

## v5.2.0 (22/08/2019)

### Features

A new method [`refreshAccessToken`](https://developer.reach5.co/api/identity-android/#refresh-access-token) was implemented to refresh a profile's access token.

## v5.1.0 (14/08/2019)

### Fixes

- Fix the sign-up on the demo application.
- The type of the [`consentType`](https://developer.reach5.co/api/identity-android/#consent) property was changed to `String`.

### Changes

- The `code` parameter was deleted from the [`AuthToken`](https://developer.reach5.co/api/identity-android/#auth-token) model since it was unused.
- The `redirectUrl` argument was deleted from the [`logout`](https://developer.reach5.co/api/identity-android/#logout) method since it wasn't pertinent for mobile.

## v5.0.1 (09/08/2019)

### Fixes

- The `authToken` argument of the [`updatePassword`](https://developer.reach5.co/api/identity-android/#update-password) method was deleted since it is not required when the user provides his email or phone number with a verification code.
Nevertheless, the `freshAuthToken` argument was added to [`FreshAccessTokenParams`](https://developer.reach5.co/api/identity-android/#update-password-request-fresh-access-token-params) and `authToken` to [`AccessTokenParams`](https://developer.reach5.co/api/identity-android/#update-password-request-access-token-params).
- The `updatePhoneNumberRequest` argument of the `updatePassword` method was renamed to `updatePasswordRequest`.

## v5.0.0 (01/08/2019)

### Breaking changes

- The new default is to use the scopes defined for your client via the ReachFive console.
Keep in mind that you must initialize the client through the `initialize` method for the scopes to be set, or an empty value will be used.
- All the data models used for requests were moved in a sub-folder named `requests` (`com.reach5.identity.sdk.core.models` -> `com.reach5.identity.sdk.core.models.requests`).
- The profile's data passed as an argument to the `signup` method is no longer a `Profile` but a [`ProfileSignupRequest`](https://developer.reach5.co/api/identity-android/#profile-signup-request).
Note that `ProfileSignupRequest` contains the same attributes as `Profile` plus the `password` field and minus the identifiers and authentication details.
- The `User` data model was renamed to [`OpenIdUser`](https://developer.reach5.co/api/identity-android/#openid-user).
- The `AuthToken.idToken` field became optional since it isn't returned when the `openid` scope is not provided.
- The `openid` scope is no longer provided by default to the `loginWithProvider` method through the `WebViewProvider`.
- The `logout` method now disconnects all sessions including those created with a provider.

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

The `requestPasswordReset` no longer needs the `authToken` argument.
    
## v4.2.0 (22/07/2019)

### Features

- All the SDK core models are now serialized for an Android implementation.
- The authorization flow with code and the PKCE are implemented for login with the `WebViewProvider`.

## v4.1.0 (12/07/2019)

### Features

- A new method [`updatePassword`](https://developer.reach5.co/api/identity-android/#update-password) was implemented to update the profile's password.
- A new method [`logout`](https://developer.reach5.co/api/identity-android/#update-password) was implemented to kill the SSO session of the profile.

### Fixes

The `success` parameter of the `requestPasswordReset` and `verifyPhoneNumber` methods was renamed into `successWithNoContent` and is now correctly called.

## v4.0.0 (07/07/2019)

### Changes

New modular version of the Identity SDK Android:

- [`sdk-core`](sdk-core)
- [`sdk-webview`](sdk-webview)
- [`sdk-facebook`](sdk-facebook)
- [`sdk-google`](sdk-google)
