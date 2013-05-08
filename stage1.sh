set -x verbose #echo on

adb shell pm uninstall com.google.cloud.backend.android
adb install android-endstage1/bin/android-endstage1.apk
adb shell am start -n com.google.cloud.backend.android/.sample.guestbook.GuestbookActivity


