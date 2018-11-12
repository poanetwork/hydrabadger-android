#/bin/bash

#create dir
mkdir -p ../android/app/src/main/libs/arm64/
mkdir -p ../android/app/src/main/libs/x86/
mkdir -p ../android/app/src/main/libs/armeabi/

# move *.so
cp -f ./target/aarch64-linux-android/release/libhydra_android.so ../android/app/src/main/libs/arm64/
cp -f ./target/i686-linux-android/release/libhydra_android.so ../android/app/src/main/libs/x86/
cp -f ./target/arm-linux-androideabi/release/libhydra_android.so ../android/app/src/main/libs/armeabi/

# move generating files
cp -f ./app/src/main/java/net/korul/hbbft/* ../android/app/src/main/java/net/korul/hbbft/
