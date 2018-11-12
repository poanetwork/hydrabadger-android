Hydrabadger
================

An experimental peer-to-peer client using the [Honey Badger Byzantine Fault
Tolerant consensus algorithm](https://github.com/poanetwork/hbbft) and Mobile messenger based on hbbft consensus 

## Usage

### Setup

1. `git clone git@github.com:poanetwork/hydrabadger-android.git`

2. Download Android studio, NDK, rust etc..

3. set needs environments 

`export ANDROID_HOME=/Users/$USER/Library/Android/sdk`
`export NDK_HOME=$ANDROID_HOME/ndk-bundle` 

and etc

4. Download [rustup](https://www.rustup.rs/). We will use this to setup Rust for
   cross-compiling.

    ```sh
    curl https://sh.rustup.rs -sSf | sh
    ```

5. Download targets for Android.

    ```sh
    # Android.
    rustup target add aarch64-linux-android armv7-linux-androideabi i686-linux-android
    ```

### Compile
-----

1. Create the standalone NDKs.

    ```sh
    ./create-ndk-standalone.sh
    ```

6. Copy the content of `cargo-config.toml` (consists of linker information of
   the Android targets) to `~/.cargo/config`

    ```sh
    cp cargo-config.toml ~/.cargo/config
    ```

### Creating the libraries
----------------------

You use the `sample/` project as an example.


1. Build the libraries.

    ```sh
	#!/bin/sh

	#set -euo pipefail

	# Build the rust project.
	cd cargo
	cargo clean
	#cargo test

	# cargo lipo --release
	$PWD/compile.sh
	$PWD/move.sh
    ```

	or

	run in `sample/` directory

	```
	build.sh
	```

### Android project

```
cd sample/android
```

Run this project in Android Studio, this project uses the compiled Hydrabadger.

### Current State

Network initialization node addition, transaction generation, consensus,
and batch outputs are all generally working. Batch outputs for each epoch are
printed to the log.

Overall the client is fragile and doesn't handle deviation from simple usage
very well yet.

### Unimplemented

* **Many edge cases and exceptions:** disconnects, reconnects, etc.
  * Connecting to a network which is in the process of key generation causes
    the entire network to fail. For now, wait until the network starts
    outputting batches before connecting additional peer nodes.
* **Error handling** is atrocious, most errors are simply printed to the log.
* **Usage as a library** is still a work in progress as the API settles.
* **Much, much more...**

### License

[![License: LGPL v3.0](https://img.shields.io/badge/License-LGPL%20v3-blue.svg)](https://www.gnu.org/licenses/lgpl-3.0)

This project is licensed under the GNU Lesser General Public License v3.0. See the [LICENSE](LICENSE) file for details.
