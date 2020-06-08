<p align="center">
 <img src="https://www.reachfive.com/hs-fs/hubfs/Reachfive_April2019/Images/site-logo.png?width=700&height=192&name=site-logo.png"/>
</p>

[![CircleCI](https://circleci.com/gh/ReachFive/identity-android-sdk/tree/master.svg?style=svg)](https://circleci.com/gh/ReachFive/identity-android-sdk/tree/master)
[![Download](https://api.bintray.com/packages/reachfive/identity-sdk/sdk-core/images/download.svg) ](https://bintray.com/reachfive/identity-sdk)

# ReachFive Identity Android SDK

## Installation

Refer to the [public documentation](https://developer.reach5.co/guides/installation/android/) to install the SDKs and to initialize your ReachFive client.

## Demo application

In addition to the libraries, we provide in the `/app` directory a simple Android application which integrates the ReachFive SDKs.

Clone the repository and import it in your favorite IDE (we advise you to use [Android Studio](https://developer.android.com/studio)).

Since the demo application uses Google services, you need to create a new [Firebase](https://firebase.google.com/) project.
Download the `google-services.json` file associated and put it at the root of the `/app` directory.

You also need to set the ReachFive client configuration in the `/app/src/main/assets/env` file as below:

```
# formatted as key=value
DOMAIN=my-reachfive-url
CLIENT_ID=my-reachfive-client-id
REDIRECT_URI=my-reachfive-url-scheme
```

The URL scheme must follow this pattern: `reachfive-${clientId}://callback`.

Finally install the dependencies with [Gradle](https://gradle.org/) (it will be done automatically with Android Studio), select a virtual device and run the application.

## Documentation

You'll find the documentation of the methods exposed on https://developer.reach5.co/api/identity-android.

## Testing

To launch integration tests, configure your domain and client ID as described above, start your Android emulator, and then run:

`./gradlew connectedAndroidTest`

## Changelog

Please refer to [changelog](CHANGELOG.md) to see the descriptions of each release.

## License

MIT © [ReachFive](https://reachfive.co/)
