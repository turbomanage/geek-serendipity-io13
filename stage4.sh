set -x verbose #echo on

adb shell pm uninstall com.google.cloud.backend.android
adb install android-endstage4/bin/android-endstage4.apk
adb shell am start -n com.google.cloud.backend.android/.MapActivity 


