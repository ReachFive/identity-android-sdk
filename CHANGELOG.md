# Changelog

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
