#! /bin/bash -v

export PATH=~/Library/Android/sdk/build-tools/30.0.3/:$PATH
export PATH=/Applications/Android\ Studio.app/Contents/jre/Contents/Home/:$PATH

export JAVA_HOME=/Applications/Android\ Studio.app/Contents/jre/Contents/Home/
export JRE_HOME=/Applications/Android\ Studio.app/Contents/jre/Contents/Home/


keytool -genkey -v -keystore ../release.keystore -alias MyReleaseKey -keyalg RSA -keysize 2048 -validity 10000 -storepass MyPassword -keypass MyPassword
