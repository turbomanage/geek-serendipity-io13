set -x verbose #echo on

adb shell pm uninstall com.google.cloud.backend.android
adb install android-endstage6/bin/android-endstage6.apk
adb shell am start -n com.google.cloud.backend.android/.MapActivity 
echo "https://appengine.google.com/logs?&app_id=s~geekfinder2"



