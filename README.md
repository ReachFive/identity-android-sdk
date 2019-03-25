<p align="center">
 <img src="https://reachfive.co/img/site-logo.png"/>
</p>

# Identity Android SDK


## Android studio
(https://developer.android.com/studio)[https://developer.android.com/studio]


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
