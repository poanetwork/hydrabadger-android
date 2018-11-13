#/bin/bash

#create dir
mkdir -p ../android/app/src/main/jniLibs/arm64-v8a/
mkdir -p ../android/app/src/main/jniLibs/x86/
mkdir -p ../android/app/src/main/jniLibs/armeabi-v7a/

# move *.so
cp -f ./target/aarch64-linux-android/release/libhydra_android.so ../android/app/src/main/jniLibs/arm64-v8a/
cp -f ./target/i686-linux-android/release/libhydra_android.so ../android/app/src/main/jniLibs/x86/
cp -f ./target/armv7-linux-androideabi/release/libhydra_android.so ../android/app/src/main/jniLibs/armeabi-v7a/

# move generating files
cp -f ./app/src/main/java/net/korul/hbbft/* ../android/app/src/main/java/net/korul/hbbft/
