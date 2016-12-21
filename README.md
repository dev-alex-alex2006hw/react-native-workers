#  Patch by Bill Wang
1. Fixed code to be compatible with react native 0.35
2. Add patch to compile properly with Release on Android
3. Document iOS release patch

# Disclaimer

I am not going to support this code base in long term. Will only update when necessary.

## Android Release Patch

Add following tasks to your build.gradle in `android/app/build.gradle`

Configure the dirs properly

```
// Hot fix to enable worker compilation from
// https://github.com/facebook/react-native/blob/master/react.gradle
gradle.projectsEvaluated {
	// register task before react-native bundle to target asset
    processReleaseManifest.dependsOn(compileReleaseWorker)
}
task compileReleaseWorker(type: Exec) {
	// set up env and variables
	def ENTRY_FILE_BASENAME
	def resourcesDirRelease="$buildDir/intermediates/assets/release/workers"
	def collection =fileTree("../../App/Lib/") {
		include '*Worker.js'
	}
	// make sure target dir exists
	if( !new File(resourcesDirRelease).exists() ) {
  		new File(resourcesDirRelease).mkdirs()
	}

	workingDir '../../'
	def BUNDLE_FILE
	// iterate over workers, and bundle them
	collection.each{ File file ->
		ENTRY_FILE_BASENAME = file.name.split("\\.")[0]
		BUNDLE_FILE="$resourcesDirRelease/$ENTRY_FILE_BASENAME"+".bundle"
		commandLine("node", "node_modules/react-native/local-cli/cli.js", "bundle", "--platform", "android",
		"--dev", "false", "--reset-cache", "--entry-file", file, "--bundle-output", BUNDLE_FILE,
		"--assets-dest", resourcesDirRelease)
    }
 ```

## iOS Release Patch
Credit to @therealgilles,[see issue] (https://github.com/devfd/react-native-workers/issues/21)

Create a react-native-workers.sh script (see below) inside the ios folder and add it to Build Phases > Bundle React Native code and images:

``export NODE_BINARY=node
./react-native-workers.sh
../node_modules/react-native/packager/react-native-xcode.sh
``

--- ios/react-native-workers.sh ---

Change `App/Workers` and `App/Workers/*Worker.js` to match the location and filename pattern of your workers.

```
#!/bin/bash
# Copyright (c) 2015-present, Facebook, Inc.
# All rights reserved.
#
# This source code is licensed under the BSD-style license found in the
# LICENSE file in the root directory of this source tree. An additional grant
# of patent rights can be found in the PATENTS file in the same directory.

# Bundle React Native app's code and image assets.
# This script is supposed to be invoked as part of Xcode build process
# and relies on environment variables (including PWD) set by Xcode

case "$CONFIGURATION" in
  Debug)
    # Speed up build times by skipping the creation of the offline package for debug
    # builds on the simulator since the packager is supposed to be running anyways.
    if [[ "$PLATFORM_NAME" == *simulator ]]; then
      echo "Skipping bundling for Simulator platform"
      exit 0;
    fi

    DEV=true
    ;;
  "")
    echo "$0 must be invoked by Xcode"
    exit 1
    ;;
  *)
    DEV=false
    ;;
esac

# Path to react-native folder inside node_modules
REACT_NATIVE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../node_modules/react-native" && pwd)"

# Xcode project file for React Native apps is located in ios/ subfolder
cd ..

# Define NVM_DIR and source the nvm.sh setup script
[ -z "$NVM_DIR" ] && export NVM_DIR="$HOME/.nvm"

if [[ -s "$HOME/.nvm/nvm.sh" ]]; then
  . "$HOME/.nvm/nvm.sh"
elif [[ -x "$(command -v brew)" && -s "$(brew --prefix nvm)/nvm.sh" ]]; then
  . "$(brew --prefix nvm)/nvm.sh"
fi

# Set up the nodenv node version manager if present
if [[ -x "$HOME/.nodenv/bin/nodenv" ]]; then
  eval "$("$HOME/.nodenv/bin/nodenv" init -)"
fi

[ -z "$NODE_BINARY" ] && export NODE_BINARY="node"

nodejs_not_found()
{
  echo "error: Can't find '$NODE_BINARY' binary to build React Native bundle" >&2
  echo "If you have non-standard nodejs installation, select your project in Xcode," >&2
  echo "find 'Build Phases' - 'Bundle React Native code and images'" >&2
  echo "and change NODE_BINARY to absolute path to your node executable" >&2
  echo "(you can find it by invoking 'which node' in the terminal)" >&2
  exit 2
}

type $NODE_BINARY >/dev/null 2>&1 || nodejs_not_found

# Print commands before executing them (useful for troubleshooting)
set -x

# change App/Workers to match the location of your workers
DEST=$CONFIGURATION_BUILD_DIR/$UNLOCALIZED_RESOURCES_FOLDER_PATH/App/Workers
mkdir -p $DEST

# Get list of entry files (change App/Workers/*Worker.js to match the location and filename pattern of your workers)
for ENTRY_FILE in ${1:-App/Workers/*Worker.js}
do

  ENTRY_FILE_BASENAME=$(basename $ENTRY_FILE | cut -d. -f1)
  BUNDLE_FILE="$DEST/$ENTRY_FILE_BASENAME.jsbundle"

  $NODE_BINARY "$REACT_NATIVE_DIR/local-cli/cli.js" bundle \
    --entry-file "$ENTRY_FILE" \
    --platform ios \
    --dev $DEV \
    --reset-cache \
    --bundle-output "$BUNDLE_FILE" \
    --assets-dest "$DEST"

  if [[ ! $DEV && ! -f "$BUNDLE_FILE" ]]; then
    echo "error: File $BUNDLE_FILE does not exist. This must be a bug with" >&2
    echo "React Native, please report it here: https://github.com/facebook/react-native/issues"
    exit 2
  fi

done
```

# react-native-workers

Spin worker threads and run CPU intensive tasks in the background. Bonus point on Android you can keep a worker alive even when a user quit the application :fireworks:

## Features
- JS web workers for iOS and Android
- access to native modules (network, geolocation, storage ...)
- Android Services in JS :tada:

## Installation

```bash
npm install react-native-workers --save
```

### Automatic setup

simply `rnpm link react-native-workers` and you'r good to go.

### Manual setup

#### iOS

1. Open your project in XCode, right click on Libraries and click Add Files to "Your Project Name". Look under node_modules/react-native-workers/ios and add `Workers.xcodeproj`
2. Add `libWorkers.a` to `Build Phases -> Link Binary With Libraries`

#### Android

in `android/settings.gradle`

```
 include ':app', ':react-native-workers'
 project(':react-native-workers').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-workers/android')
 ```

in `android/app/build.gradle` add:

```
dependencies {
   ...
   compile project(':react-native-workers')
}
```

and finally, in your `MainApplication.java` add:

```java

import co.apptailor.Worker.WorkerPackage; // <--- This!

public class MainApplication extends Application implements ReactApplication {

private final ReactNativeHost mReactNativeHost = new ReactNativeHost(this) {
    @Override
    protected boolean getUseDeveloperSupport() {
      return BuildConfig.DEBUG;
    }

    @Override
    protected List<ReactPackage> getPackages() {
      return Arrays.<ReactPackage>asList(
          new MainReactPackage(),
          new WorkerPackage() // <--- and this
      );
    }
  };

  @Override
  public ReactNativeHost getReactNativeHost() {
      return mReactNativeHost;
  }
}

```

**Note**: only the official react native modules are available from your workers (vibration, fetch, etc...). To include additional modules in your workers add them to the WorkerPackage constructor. Like this:

```java
new WorkerPackage(new MyAwesomePackage(), new MyAmazingPackage())`
```

## JS API

From your application:
```js
import { Worker } from 'react-native-workers';

/* start worker */
const worker = new Worker("path/to/worker.js");

/* post message to worker. String only ! */
worker.postMessage("hello from application");

/* get message from worker. String only ! */
worker.onmessage = (message) => {

}

/* stop worker */
worker.terminate();

```

From your worker js file:
```js
import { self } from 'react-native-workers';

/* get message from application. String only ! */
self.onmessage = (message) => {
}

/* post message to application. String only ! */
self.postMessage("hello from worker");
```

## Lifecycle

- the workers are paused when the app enters in the background
- the workers are resumed once the app is running in the foreground
- During development, when you reload the main JS bundle (shake device -> `Reload`) the workers are killed

## Todo

- [x] Android - download worker files from same location as main bundle
- [x] iOS - download worker files from same location as main bundle
- [ ] script to package worker files for release build
- [x] load worker files from disk if not debug
