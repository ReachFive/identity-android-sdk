<p align="center">
 <img src="https://www.reachfive.com/hs-fs/hubfs/Reachfive_April2019/Images/site-logo.png?width=700&height=192&name=site-logo.png"/>
</p>

# Identity Android SDK

## Table of Contents
1. [Installation](#installation)
2. [Getting Started](#getting-started)
    * [SDK Core](#sdk-core-required)
    * [SDK WebView](#sdk-webview)
    * [SDK Facebook native](#facebook-native-provider)
    * [SDK Google native](#google-native-provider)

## Installation

### IDE: Android studio
(https://developer.android.com/studio)[https://developer.android.com/studio]

### Configure repository

Add repository in `build.gradle`

```groovy
repositories {
    jcenter()

    // Developpement repository, in the future it will be stored into jcenter
    maven {
        url  "https://dl.bintray.com/reachfive/identity-sdk"
    }
}
```

## Getting Started

This SDK is modular and you import only what you really using, only the SDK Core is required

### SDK Core (required)
It containt all common tools and interfaces, authentication with passwords

```groovy
dependencies {
    implementation "com.reach5.identity:sdk-core:4.0.0"
}
```

These permissions is required to communicate with ReachFive servers

Add them into `AndroidManifest.xml`

```xml
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.USE_CREDENTIALS" />
```

### SDK WebView
This module use WebView to authenticate users, it give all providers supported by reachfive 

```groovy
dependencies {
    implementation "com.reach5.identity:sdk-webview:4.0.0"
}
```

You need to add this activity into `AndroidManifest.xml`

```xml
<activity
    android:name="com.reach5.identity.sdk.webview.ReachFiveLoginActivity"
    android:screenOrientation="portrait" />
```

### Facebook native provider

This module use Facebook native SDK to provider better user experience

#### Dependencies

```groovy
dependencies {
    implementation "com.reach5.identity:sdk-facebook:4.0.0"
    implementation 'com.facebook.android:facebook-login:4.37.0'
}
```

#### Configuration

(https://support.reach5.co/article/4-create-facebook-application)[Facebook Connect]

Note: if you use the latest version of facebook api, remove user_gender scope from reachfive client config

Add this lines into your `string.xml` resources file with your Facebook application ID
```xml
<resources>
    <string name="facebook_app_id">XXXXXXXXXXXXXXX</string>
</resources>
```

Add these lines into `AndroidManifest.xml` 

```xml
<meta-data
    android:name="com.facebook.sdk.ApplicationId"
    android:value="@string/facebook_app_id" />

<activity
    android:name="com.facebook.FacebookActivity"
    android:configChanges="keyboard|keyboardHidden|screenLayout|screenSize|orientation"
    android:label="@string/app_name" />
```

### Google native provider
This module use Google Native SDK to provider better user experience
#### Configuration

To use Google's native SDK you need to create a Google app, the steps are described in this article (https://support.reach5.co/article/5-create-google-application)[Google Connect]

Once the application is created, you need an 'ID client Oauth` specific to Android apps, you can create it by selecting Android, fill in an application name, package name and a SHA1 signature digest that you can retrieve with the following command (more infos https://developers.google.com/android/guides/client-auth)

```sh
keytool -exportcert -keystore path-to-debug-or-production-keystore -list -v
```

You have to use the Firebase Google Services, for that you have to create a Firebase project on https://console.firebase.google.com then you have to create an application by clicking on the logo of android

Enter the name of the package the name of the application and the signature SHA-1, download the file `google-services.json` and put it in the root of your project Android, more information in https://firebase.google.com/docs/android/setup or https://developers.google.com/android/guides/google-services-plugin#adding_the_json_file

Add these lines in this file `build.gradle`
```gradle
buildscript {
    dependencies {
        classpath 'com.google.gms:google-services:4.2.0'
    }
}
```

Make sure the google repository is there
```
repositories {
    google()
}
```

Then in `app/build.gradle` add the following line to activate the plugin
```
apply plugin: 'com.google.gms.google-services'
```

##### Dependency
```groovy
dependencies {
    implementation "com.reach5.identity:sdk-google:4.0.0"
}
```

## Usage with kotlin

### Initialaze the SDK
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    // ...

    val sdkConfig = SdkConfig(
        domain = "sdk-mobile-sandbox.reach5.net",
        clientId = "TYAIHFRJ2a1FGJ1T8pKD"
    )

    val providersCreators = listOf(
        GoogleProvider(),
        FacebookProvider(),
        WebViewProvider()
    )

    this.reach5 = ReachFive(
        sdkConfig = sdkConfig,
        providersCreators = providersCreators,
        activity = this
    )

    this.reach5.initialize({ providers ->
        // You can use this list of providers to show buttons
    }, { error ->
        Log.d(TAG, "ReachFive SDK init error ${error.message}")
    })
}
```

### Login with Provider
```kotlin
this.reach5.loginWithProvider(GoogleProvider.NAME, "origin", this)
// or
this.reach5.loginWithProvider("paypal", "origin", this)
```

### Login with Password
```kotlin
this.reach5.loginWithPassword(
    Profile(
        email = username.text.toString(),
        password = password.text.toString()
    ), success = {
    handleLoginSuccess(it)
}, failure = {
    Log.d(TAG, "loginWithPassword error=$it")
})
```

### Signup with Password
```kotlin
this.reach5.loginWithPassword(
    username = username.text.toString(),
    password = password.text.toString(),
    success = {
        handleLoginSuccess(it)
    },
    failure = {
        Log.d(TAG, "signupWithPassword error=$it")
    }
)
```

### Handle activity result
```kotlin
public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    this.reach5.onActivityResult(requestCode, resultCode, data, success = { authToken ->
        // Content user information
        val user = authToken.user
        val accessToken = authToken.accessToken
    }, failure = {
        Log.d(TAG, "onActivityResult error=$it")
        it.exception?.printStackTrace()
    })
}
```

### Stop the SDK

```kotlin
override fun onStop() {
    super.onStop()
    reach5.onStop()
}
```
