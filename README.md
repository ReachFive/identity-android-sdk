<p align="center">
 <img src="https://reachfive.co/img/site-logo.png"/>
</p>

# Identity Android SDK


## Android studio
(https://developer.android.com/studio)[https://developer.android.com/studio]


## Installation

### Repository
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

### Dependencies
```groovy
dependencies {
    implementation "com.reach5.identity:sdk-core:4.0.0"
    implementation "com.reach5.identity:sdk-webview:4.0.0"
    implementation "com.reach5.identity:sdk-facebook:4.0.0"
    implementation "com.reach5.identity:sdk-google:4.0.0"
}
```

This sdk is modular and you can use only what you need

`sdk-core` required
`sdk-google` use native google auth sdk
`sdk-facebook` use native facebook auth sdk
`sdk-webview` it use webview to authenticate and support all ReachFive providers

### AndroidManifest.xml

These permissions is required becose it use network to communicate with ReachFive servers

```xml
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.USE_CREDENTIALS" />
```

### WebView

```groovy
dependencies {
    implementation "com.reach5.identity:sdk-webview:4.0.0"
}
```

To use webview module you need to add this activity
```xml
<activity
    android:name="com.reach5.identity.sdk.webview.ReachFiveLoginActivity"
    android:screenOrientation="portrait" />
```

### Facebook native provider

```groovy
dependencies {
    implementation "com.reach5.identity:sdk-facebook:4.0.0"
    implementation 'com.facebook.android:facebook-login:4.37.0'
}
```

#### Configuration
(https://support.reach5.co/article/4-create-facebook-application)[Facebook Connect]


### Google native provider

```groovy
dependencies {
    implementation "com.reach5.identity:sdk-google:4.0.0"
}
```

#### Configuration
(https://support.reach5.co/article/5-create-google-application)[Google Connect]


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
