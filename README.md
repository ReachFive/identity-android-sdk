<p align="center">
 <img src="https://www.reachfive.com/hs-fs/hubfs/Reachfive_April2019/Images/site-logo.png?width=700&height=192&name=site-logo.png"/>
</p>

[![CircleCI](https://circleci.com/gh/ReachFive/identity-android-sdk/tree/master.svg?style=svg)](https://circleci.com/gh/ReachFive/identity-android-sdk/tree/master)
[![Download](https://api.bintray.com/packages/reachfive/identity-sdk/sdk-core/images/download.svg) ](https://bintray.com/reachfive/identity-sdk)

# ReachFive Identity Android SDK

## Installation

Refer to the [public documentation](https://developer.reachfive.com/sdk-android/index.html) to install the SDKs and to initialize your ReachFive client.

## Demo application

In addition to the libraries, we provide in the `/app` directory a simple Android application which integrates the ReachFive SDKs.

Clone the repository and import it in your favorite IDE (we advise you to use [Android Studio](https://developer.android.com/studio)).

Since the demo application uses Google services, you need to create a new [Firebase](https://firebase.google.com/) project.
Download the `google-services.json` file associated and put it at the root of the `/app` directory.

You also need to set the ReachFive client configuration in the `/app/src/main/assets/env` file as below (create the `assets` folder if necessary):

```
# formatted as key=value
DOMAIN=my-reachfive-url
CLIENT_ID=my-reachfive-client-id
SCHEME=my-reachfive-url-scheme
```

The URL scheme must follow this pattern: `reachfive://${clientId}/callback`.

To login with a WebView, the scheme's path must also be set in a resource file as below:

```xml
<resources>
    <string name="reachfive_scheme">reachfive</string>
    <string name="reachfive_client_id">${clientId}</string>
    <string name="reachfive_path">/callback</string>
</resources>
```

Finally install the dependencies with [Gradle](https://gradle.org/) (it will be done automatically with Android Studio), select a virtual device and run the application.

### Login with FIDO2

If you want to login with FIDO2, you need to set the domain of the origin in the `/app/src/main/assets/env` file as below:

```
# formatted as key=value
DOMAIN=my-reachfive-url
CLIENT_ID=my-reachfive-client-id
SCHEME=my-reachfive-url-scheme

ORIGIN=my-webauthn-origin-domain
```

## Documentation

You'll find the documentation of the methods exposed on https://developer.reach5.co/api/identity-android.

## Testing

To launch integration tests, configure your domain and client ID as described above, start your Android emulator, and then run:

`./gradlew connectedAndroidTest`

## Changelog

Please refer to [changelog](CHANGELOG.md) to see the descriptions of each release.

## License

MIT © [ReachFive](https://reachfive.co/)
