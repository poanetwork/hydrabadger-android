#!/bin/sh

#set -euo pipefail

# Build the rust project.
cd cargo
#cargo clean
#cargo test

# cargo lipo --release
$PWD/compile.sh
$PWD/move.sh
