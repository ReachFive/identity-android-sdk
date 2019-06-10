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

Add repository in your `build.gradle`

```groovy
repositories {
    jcenter()

    // Temporary repository, in the future it will be stored into jcenter
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
    implementation "com.reach5.identity:sdk-webview:4.0.0"
    implementation "com.reach5.identity:sdk-facebook:4.0.0"
    implementation "com.reach5.identity:sdk-google:4.0.0"
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

Add this lines into your `string.xml` resources file with your Facebook application ID
```xml
<resources>
    <string name="facebook_app_id">XXXXXXXXXXXXXXX</string>
</resources>
```

And into `AndroidManifest.xml` add these lines

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
(https://support.reach5.co/article/5-create-google-application)[Google Connect]

https://developers.google.com/android/guides/google-services-plugin#adding_the_json_file

https://developers.google.com/android/guides/client-auth

##### Dependency
```groovy
dependencies {
    implementation "com.reach5.identity:sdk-google:4.0.0"
    implementation "com.reach5.identity:sdk-google:4.0.0"
}
```

## Android simulator

### Using local sandbox

[https://stackoverflow.com/questions/41117715/how-to-edit-etc-hosts-file-in-android-studio-emulator-running-in-nougat](https://stackoverflow.com/questions/41117715/how-to-edit-etc-hosts-file-in-android-studio-emulator-running-in-nougat)

```sh
emulator -avd Nexus_6_API_28 -writable-system
adb root
adb remount
adb push hosts /etc/hosts
adb reboot
```
