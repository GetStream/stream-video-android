# Repackaging of the Video SDK

## Preparation
Make sure you have `maven` and `python3` installed:
```bash
brew install maven
brew install python3
```
Please, ensure that you have requested raw webrtc `repackaged` aar file from Stream. 

## Clone Repositories
We need to clone both repositories [stream-video-android](https://github.com/GetStream/stream-video-android) and [webrtc-android](https://github.com/GetStream/webrtc-android) accordingly.
```bash
git clone https://github.com/GetStream/stream-video-android
git clone https://github.com/GetStream/webrtc-android
```

## Run Repackage Script:
Script is available in the root of the [stream-video-android](https://github.com/GetStream/stream-video-android) repository.
```bash
./repackage.sh --repackaged_webrtc <path_to_webrtc_aar> --webrtc_android <path_to_webrtc-android_repo>
```

When script completes, the sample usage of the new dependencies can be found in dogfooding's [build.gradle.kts](https://github.com/GetStream/stream-video-android/blob/develop/dogfooding/build.gradle.kts):
```
// Stream Video SDK
implementation("io.getstream:streamx-video-android-compose:${Configuration.versionName}")
implementation("io.getstream:streamx-video-android-xml:${Configuration.versionName}")
implementation("io.getstream:streamx-video-android-tooling:${Configuration.versionName}")
implementation("io.getstream:streamx-video-android-datastore:${Configuration.versionName}")
compileOnly("io.getstream:streamx-video-android-mock:${Configuration.versionName}")
```

## Location of Repackaged Dependencies
The repackaged modules are stored in:
```
~/.m2/repository/io/getstream/streamx-<module-name>
```

> **Note**
> `streamx` prefix means that it is a repackaged module and it's stored on your machine locally only.

<img width="300" src=".repackage-assets/maven_local.png" alt="Stream Video for Android Header image" style="box-shadow: 0 3px 10px rgb(0 0 0 / 0.2); border-radius: 1rem" />


## How to Use It
In your android project, you need to add `mavenLocal()` to the repositories list in your `settings.gradle.kts`.

```gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal() // here we've added mavenLocal()
        google()
        mavenCentral()
    }
}
```

Now you can use the new dependencies in the following way:
```gradle
implementation("io.getstream:streamx-video-android-compose:X.Y.Z")
implementation("io.getstream:streamx-video-android-xml:X.Y.Z")
// the rest of the dependencies
```