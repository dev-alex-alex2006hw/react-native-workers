#  Patch by Bill Wang
1. Fixed code to be compatible with react native 0.35
2. Add patch to compile properly with Release on Android

# Disclaimer

I am not going to support this code base in long term. Will only update when necessary.


## Android release fix

In your code, especially for release scenario, please load the worker directly.
`      this.worker = new Worker('Worker.js')`, even if worker is not stored under your root folder.

So enable some fancy logic to toggle between release and development mode.

Add following tasks to your build.gradle in `android/app/build.gradle`

Configure the dirs properly

`
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
 `






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



