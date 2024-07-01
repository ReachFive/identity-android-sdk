# Changelog

## Unreleased
- RBA support: 
  - Methods: `listMfaTrustedDevices`, `removeMfaTrustedDevice`

## v9.2.0 (20/06/2024)

### Fixes

- Fix completion of WebAuthn device registration

### Features

- MFA support:
    - Methods `startMfaPhoneNumberRegistration`, `verifyMfaPhoneNumberRegistration`, `startMfaEmailRegistration`,
    `verifyMfaEmailRegistration`, `listMfaCredentials`, `removeMfaEmail`, `removeMfaPhoneNumber`, `startStepUp`, `endStepUp`

- Passkey support:
  - methods `signupWithPasskey`, `loginWithPasskey`, `discoverableLogin`, `registerNewPasskey`
  - for passkeys, `originWebAuthn` is to be configured in the `SdkConfig` object
  - support in WebView flow
 
- Account Recovery support.

## v9.1.0 (25/01/2024)

### Breaking changes

- The ReachFive now SDK supports Android SDK from API Level 21 (Android version 5.0 Lollipop).

### Features

- Added custom fields for Address entity.
- Improve error messages for WeChat.
- Added 'Accept-Language' header in requests sent to the backend.

## v9.0.0 (05/01/2024)

### Breaking changes

- Upgrade Facebook Login 12.2 => 16.3: a client token is now required in the manifest (see https://developers.facebook.com/docs/android/getting-started/#add-app_id)

## v8.3.0 (12/12/2023)

### Features
- `loginWithWebView` performs the same flows as `loginWithWeb`, but within a WebView

## v8.2.0 (02/10/2023)
- Added WeChat provider's login

## v8.1.2 (18/04/2023)

### Changes

- Option 3 of google's mitigation doc (see: https://support.google.com/faqs/answer/9267555).

## v8.1.1 (14/11/2022)

### Changes

- Basic checks against intent redirections

## v8.1.0 (25/10/2022)

### Changes

- Added field Custom Identifier to loginWithPassword
- Added field Custom Identifier to signup

## v8.0.1 (12/08/2022)

### Changes

- Handle WebAuthn user cancellation as failures so that integrators can detect them more easily.

## v8.0.0 (05/08/2022)

This major releases includes many breaking changes but greatly simplifies the SDK integration overall.

### Features

- `loginWithWeb` enables SDK integrators to delegate login to another ReachFive first-party identity client such as web page they control.
- `onLoginActivityResult` handles all login flow results and automatically calls the appropriate internal completion flow. SDK integrators no longer have to match on request codes themselves. The method ignores any request code that does not concern the SDK.
- SDK integrators can use `ReachFive.resolveResultHandler` to let the SDK automatically wire the appropriate activity result handler (i.e., `onLoginActivityHandler` or `onAddNewWebAuthnDeviceResult`).

### Changes

- Client implementation was broken down into smaller modules.
- Client constructor no longer takes an `Activity`.
- Client initialization now only fetches client configuration.
- A separate initialization method (`loadSocialProviders`) has been added for social providers configuration.
- `WebViewProvider` social login flows now use a Custom Tab.
- Internal login callback no longer open a custom tab and silently obtain an authorization code.
- All errors are now communicated through the `failure` callback channel; Android `Activity` result codes no longer need to be manually inspected.
- An `ErrorCode` enumeration class documents all ReachFive API and SDK errors.
- The `SuccessWithNoContent<Unit>` type has been removed; all success callbacks now only use `Success<T>` or `Success<Unit>`.

### Removed methods

- `onLoginWithWebAuthnResult` : result is now automatically handled in `onLoginActivityResult`
- `onSignupWithWebAuthnResult` : result is now automatically handled in `onLoginActivityResult`

### Fixes

- Social login providers that did not support webviews are now fixed by virtue of using custom tabs.
- Social login errors have been improved.
- Activity callback methods no longer throw exceptions when the request code does not concern an SDK flow. Instead, no action is taken and a debug-level log is emitted.

## v7.0.3 (18/07/2022)

### Changes

- `Address` properties are all nullable
- Suppression of proguard rules

## v7.0.2 (09/03/2022)

### Changes

- Fix error propagation in FB login flows. (#152)

## v7.0.1 (01/03/2022)

### Changes

- Fix `SdkInfos.version` not indicating the correct SDK version. (#149)
- Use the scope configured in identity client as default value in `loginWithProvider`. (#148)
- Call `/userinfo` instead deprecated endpoint `/me`. (#147)

## v7.0.0 (29/12/2021)

### Changes

- Revised Gradle build
- Upgrade Android target SDK to 31
- Upgrade Kotlin to 1.6.10
- Upgrade build plugins
- Upgrade libraries
- Upgrade Facebook Login 11.3 => 12.2
- All packages have been migrated from `com.reach5` to `co.reachfive`

## v6.2.1 (20/09/2021)

### Changes

- Publish artifacts to Sonatype
- Package FQDN has changed from `com.reach5.identity` to `co.reachfive.identity`

## v6.2.0 (10/09/2021)

### Changes

- Passwordless API calls have been updated following the latest backend changes (#125).
- Upgrade Facebook SDK dependency from 5.0.3 to 8.2.0.

## v6.1.0 (30/11/2020)

### Feature

The `redirectUrl` was added to the [signup](https://developer.reachfive.com/sdk-android/signup.html)
method to redirect the user after the email confirmation.

## v6.0.1 (13/11/2020)

### Fixes

- Fix the exchange of an authorization code with an access token at the end of a Webauthn workflow.
- Fix the Google login through a Webview.

## v6.0.0 (04/11/2020)

### Latest changes

The format expected for your redirection scheme has changed to `reachfive://${clientId}/callback`.

Some updates are required if you are using
the [Webview SDK](https://developer.reachfive.com/sdk-android/index.html#sdk-webview):

1. Update the scheme required by the `SdkConfig` object for
   the [SDK initialization](https://developer.reachfive.com/sdk-android/index.html#initialization).
   In our example, the value is stored in the `env` file.

2. Update the scheme in the *Allowed Callback URLs* section of your Identity client on the ReachFive
   console.

### Features

You can now implement a biometric authentication flow.

> Follow our [FIDO2](https://developer.reachfive.com/sdk-android/fido2.html) guide for more information on the configuration and methods.

## v5.7.0 (01/07/2020)

### Features

The `errorMessageKey` field is now returned in the error response object. See for
example [`updateEmail`](https://developer.reachfive.com/sdk-android/updateEmail.html#reachfiveerror)
to view the documentation of the new field.

## v5.6.0 (26/06/2020)

### Features

Add the `scope` parameter to
the [`loginWithProvider`](https://developer.reachfive.com/sdk-android/loginWithProvider.html)
method.

## v5.5.0 (15/06/2020)

### Changes

- App-specific scheme handling (pattern `reachfive-${clientId}://callback`). This custom scheme has
  to be specified in `AndroidManifest.xml` application and passed during SDK configuration
  in `SdkConfig` object:

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

- This custom scheme will be used as a redirect URL by default in payload of Start Passwordless
  call.

## v5.4.6 (10/12/2019)

## v5.4.5 (09/12/2019)

## v5.4.4 (08/11/2019)

### Fixes

The following fields are now returned by
the [`getProfile`](https://developer.reach5.co/api/identity-android/#get-profile) method: `bio`
, `birthdate`, `company`, `external_id`, `locale`, `middle_name`, `nickname`, `picture`
and `tos_accepted_at`.

## v5.4.3 (27/09/2019)

### Fixes

The `consents`, `emails`, `email_verified`, `phone_number` and `phone_number_verified` fields are
returned again by the [`getProfile`](https://developer.reach5.co/api/identity-android/#get-profile)
method.

## v5.4.2 (26/09/2019)

### Fixes

The custom fields are now returned by
the [`getProfile`](https://developer.reach5.co/api/identity-android/#get-profile) method.

## v5.4.1 (23/09/2019)

### Changes

- The `redirectUrl` is configurable for the
  method [`startPasswordless`](https://developer.reach5.co/api/identity-android/#start-passwordless)
  .

## v5.4.0 (18/09/2019)

### Features

- A new
  method [`startPasswordless`](https://developer.reach5.co/api/identity-android/#start-passwordless)
  was implemented to trigger an email/sms sending for a passwordless flow.
- A new
  method [`verifyPasswordless`](https://developer.reach5.co/api/identity-android/#verify-passwordless)
  was implemented to verify a passwordless sms verification code.
- A new
  method [`exchangeCodeForToken`](https://developer.reach5.co/api/identity-android/#exchange-code-for-token)
  was implemented to exchange an authorization code for an access token.

## v5.3.2 (28/08/2019)

### Fixes

- The `signedUid` attribute of the `Profile` model was removed.
- Login with the Facebook social account was fixed.

## v5.3.1 (23/08/2019)

### Fixes

- The `ProfileAddress` model's `isDefault` attribute is now correctly serialized.
- All the string attributes of the `ProfileAddress` model are now null by default.
- The `addressType` attribute of the `ProfileAddress` model is no longer a `String` but
  a `ProfileAddressType`.

## v5.3.0 (22/08/2019)

### Features

The ReachFive error models were improved:

- the `code` attribute was added to
  the [`ReachFiveError`](https://developer.reach5.co/api/identity-android/#reach5-error) model to
  specify the HTTP code response.
- the `errorId` and the `errorUserMsg` attributes were added to
  the [`ReachFiveApiError`](https://developer.reach5.co/api/identity-android/#reach5-api-error)
  model to specify the identifier and the user-friendly message of the error.

## v5.2.0 (22/08/2019)

### Features

A new
method [`refreshAccessToken`](https://developer.reach5.co/api/identity-android/#refresh-access-token)
was implemented to refresh a profile's access token.

## v5.1.0 (14/08/2019)

### Fixes

- Fix the sign-up on the demo application.
- The type of the [`consentType`](https://developer.reach5.co/api/identity-android/#consent)
  property was changed to `String`.

### Changes

- The `code` parameter was deleted from
  the [`AuthToken`](https://developer.reach5.co/api/identity-android/#auth-token) model since it was
  unused.
- The `redirectUrl` argument was deleted from
  the [`logout`](https://developer.reach5.co/api/identity-android/#logout) method since it wasn't
  pertinent for mobile.

## v5.0.1 (09/08/2019)

### Fixes

- The `authToken` argument of
  the [`updatePassword`](https://developer.reach5.co/api/identity-android/#update-password) method
  was deleted since it is not required when the user provides his email or phone number with a
  verification code. Nevertheless, the `freshAuthToken` argument was added
  to [`FreshAccessTokenParams`](https://developer.reach5.co/api/identity-android/#update-password-request-fresh-access-token-params)
  and `authToken`
  to [`AccessTokenParams`](https://developer.reach5.co/api/identity-android/#update-password-request-access-token-params)
  .
- The `updatePhoneNumberRequest` argument of the `updatePassword` method was renamed
  to `updatePasswordRequest`.

## v5.0.0 (01/08/2019)

### Breaking changes

- The new default is to use the scopes defined for your client via the ReachFive console. Keep in
  mind that you must initialize the client through the `initialize` method for the scopes to be set,
  or an empty value will be used.
- All the data models used for requests were moved in a sub-folder
  named `requests` (`com.reach5.identity.sdk.core.models`
  -> `com.reach5.identity.sdk.core.models.requests`).
- The profile's data passed as an argument to the `signup` method is no longer a `Profile` but
  a [`ProfileSignupRequest`](https://developer.reach5.co/api/identity-android/#profile-signup-request)
  . Note that `ProfileSignupRequest` contains the same attributes as `Profile` plus the `password`
  field and minus the identifiers and authentication details.
- The `User` data model was renamed
  to [`OpenIdUser`](https://developer.reach5.co/api/identity-android/#openid-user).
- The `AuthToken.idToken` field became optional since it isn't returned when the `openid` scope is
  not provided.
- The `openid` scope is no longer provided by default to the `loginWithProvider` method through
  the `WebViewProvider`.
- The `logout` method now disconnects all sessions including those created with a provider.

### Features

-

An [HTTP logging interceptor](https://github.com/square/okhttp/tree/master/okhttp-logging-interceptor)
was added to the `sdk-core` module to log the API requests and responses in debug mode.

- New fields were added to the `Profile` data model. You'll find their descriptions on
  the [ReachFive developer documentation](https://developer.reach5.co/api/identity-android/#profile)
  .
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
- A new method [`getProfile`](https://developer.reach5.co/api/identity-android/#get-profile) was
  implemented to fetch the profile's information.

### Fixes

The `requestPasswordReset` no longer needs the `authToken` argument.

## v4.2.0 (22/07/2019)

### Features

- All the SDK core models are now serialized for an Android implementation.
- The authorization flow with code and the PKCE are implemented for login with the `WebViewProvider`
  .

## v4.1.0 (12/07/2019)

### Features

- A new method [`updatePassword`](https://developer.reach5.co/api/identity-android/#update-password)
  was implemented to update the profile's password.
- A new method [`logout`](https://developer.reach5.co/api/identity-android/#update-password) was
  implemented to kill the SSO session of the profile.

### Fixes

The `success` parameter of the `requestPasswordReset` and `verifyPhoneNumber` methods was renamed
into `successWithNoContent` and is now correctly called.

## v4.0.0 (07/07/2019)

### Changes

New modular version of the Identity SDK Android:

- [`sdk-core`](sdk-core)
- [`sdk-webview`](sdk-webview)
- [`sdk-facebook`](sdk-facebook)
- [`sdk-google`](sdk-google)
- ['sdk-wechat'](sdk-wechat)
