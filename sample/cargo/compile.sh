#/bin/bash

# Starts compile library Hydrabadger for android arm64 arm x86
# =========================


cargo build --target aarch64-linux-android --features no-simd --release
cargo build --target armv7-linux-androideabi --features no-simd --release
cargo build --target i686-linux-android --features no-simd --release

