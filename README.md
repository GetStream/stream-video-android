# Stream Video Android

<p align="center">
  <a href="https://github.com/GetStream/stream-video-android/actions/workflows/android.yml"><img alt="Build Status" src="https://github.com/GetStream/stream-video-android/actions/workflows/android.yml/badge.svg"/></a>
  <a href="https://android-arsenal.com/api?level=21"><img alt="API" src="https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat"/></a>
  <a href="https://getstream.io"><img src="https://img.shields.io/endpoint?url=https://gist.githubusercontent.com/HayesGordon/e7f3c4587859c17f3e593fd3ff5b13f4/raw/11d9d9385c9f34374ede25f6471dc743b977a914/badge.json" alt="Stream Feeds"></a>
</p>

Stream Video repository for Android.

# Introduction

Stream Video Android contains the following parts below:

- [Square's wire](https://github.com/square/wire/), which is gRPC and protocol buffers for Android, Kotlin, and Java.
- In-house WebRTC implementation with all the SFU & SDP communication.
- Wrappers around various BE APIs.
- WebSocket connection and events parsing.
- Sample app using lower level Coordinator and WebRTC client.

## Square's wire

[Square's wire](https://github.com/square/wire/) contains dedicated compiler that interprets and generates interfaces for our Domain and API models defined in our protobuf schema.

By adding our protocol buffer schemes into the `stream-video-android` module's [/src/main/proto](https://github.com/GetStream/stream-video-android/tree/main/stream-video-kotlin/src/main/proto) package, it will generate code in the `stream.video` package as the below:

![generated-code](https://user-images.githubusercontent.com/17215808/178219855-18d27ad6-dacb-4ccb-b392-4b032338f53f.png)

You don't have to do anything yourself to generate the models, simply build the project and the Wire Gradle plugin takes care of everything.

## API Communication

Unfortunately, Android doesn't have a clear and easy way of generating HTTP services from RPC definitions in proto.

For this reason, we manually implemented the `CallCoordinatorService`, using [Retrofit](https://square.github.io/retrofit/) and its [Wire parsers](https://github.com/square/retrofit/blob/master/retrofit-converters/wire/src/main/java/retrofit2/converter/wire/WireConverterFactory.java).

That way, we don't have to write any parsing code, everything is done automatically for encoding/decoding.

## Sample app using our WebRTC implementation

We have a small sample app implemented. It currently hosts the following screens:
- Login screen
- Home Screen
- Call screen
- Various other bits & pieces of

### Login Screen

![Login Screen](https://user-images.githubusercontent.com/17215808/192727901-27782ff8-1bf0-4140-84e6-3451e546c3aa.png)

The Login screen allows you to choose between a few harcoded users and lets you put in the ID of the call you want to join.

Simply select any user and write the call ID to enter the main part of the app.

### Home Screen

Once logged in, you'll see the Home Screen.

![Home Screen](https://user-images.githubusercontent.com/17215808/192727870-22e2d30e-90b4-492a-a906-9a7ff43e8e40.png)

Here you can choose to create or join calls and invite other people to the call. You can also log out and choose another user.

After you create or join the call, you'll be able to communicate with people!

### Call Screen

![Call Screen](https://user-images.githubusercontent.com/17215808/180763075-676dcb28-fe01-4355-b2c7-839a7a101a3a.png)

The call screen is very simple - we create a grid of participants' tracks, where the last track is always the current user.

There are also a few options available to the user:
* Mute Audio
* Leave Call
* Mute Video
* Flip Camera
* Choose AudioDevice (Playback)

All of these UI elements are tied into reactive constructs and these should update based on the state.

## Running Samples

There are caveats when running the sample app. There are currently two ways to run the app and test the API and the video call SDK.

Bear in mind that both of these approaches require you to set up a few things to connect to the API:

* Set up the **Coordinator** [local server](https://github.com/GetStream/video).
* Set up the SFU [local server](https://github.com/GetStream/video-sfu)
* Clone the [proto repo](https://github.com/GetStream/video-proto) on the same level as both **video** and **video-sfu** repos.

It's best to keep all these repos cloned in the same directory/level, as they use each other to set things up.

### Using Emulators

If you're using a non-M1 CPU, you should be fine by simply running the app on an emulator. Make sure to set up hardware acceleration/virtualization based on your OS.

* [Windows](https://developer.android.com/studio/run/emulator-acceleration#vm-windows)
* [Linux](https://developer.android.com/studio/run/emulator-acceleration#vm-linux)
* [Mac](https://developer.android.com/studio/run/emulator-acceleration#vm-mac)

Then simply run the application in Android Studio.

You'll learn more about this, but make sure that all **redirect URLs** are null if you choose to host the servers locally.

### Using Real Devices

If you're using an M1 device, you have a weak PC or you simply want better video examples by using real cameras, we suggest using a real device, if you have them lying around.

However, connecting to **localhost** using real devices is not as easy on Android, as you have to connect to the IP of the device that's hosting it while being on the same network. This is more difficult because Android restricts `http://` access which can cause issues with connecting to the API.

And if you change the hosting device or the IP changes, you'd have to update the IP.

That's why we recommend hosting redirect URLs that let you connect to the server even if its IP changes, or if you change the device from which you're building to your devices.

To do this, you can use [ngrok](https://ngrok.com/docs/getting-started). It's a tool that helps you host your API calls on a temporary public URL that you can hit with your client. Once the API route is called, it will redirect from the ngrok URL to your localhost.

It's available for Windows, Linux and Mac. Once you install it, you can set up the redirect URLs.

#### Redirect URLs for Coordinator API

There are a few URLs you'll need to set up for the redirect using `ngrok`. These URLs refer to the **Coordinator** part of the API:

* Base URL for (most) API calls
* Ping server URL
* WebSocket server URL

Let's see how to set up each of these URLs.

**Base Server URL** can be set up in the following way:

First, run the following command in a new terminal tab (after launching the local server):

```
ngrok http 26991
```

This will generate a screen similar to this one:

![ngrok Generated redirect](https://user-images.githubusercontent.com/17215808/180743067-f49835b5-fcdd-4db9-923f-f7242a9d5f17.png)

Copy the highlighted URL and search the Android Studio project for `stream-video-android` for `REDIRECT_BASE_URL` and replace the value with the given URL. It's located in the `CallClientModule` at the bottom of the file.

![Redirect Base Url](https://user-images.githubusercontent.com/17215808/180743284-98c1a7ba-cd12-4001-b303-6af5c803cd8e.png)

**Ping Server URL** follows the same flow, but with a different port.

Generate a redirect for the ping server:

```
ngrok http 5764
```

And with the URL, replace the value of `REDIRECT_PING_URL` in the project and that the value ends in `/ping`, for example `https://<ngrok-url>/ping`.

**WebSocket URL** is a bit trickier. To generate a TCP/WS redirect, you need an authorization token for `ngrok`, which is free - you just need to [sign up](https://dashboard.ngrok.com/get-started/your-authtoken) to access it.

Once you do that, you can run the following:

```
ngrok tcp 8989 --authtoken <my-auth-token>
```

Then take the URL provided by the tool and replace the host part of `REDIRECT_WS_BASE_URL`. Make sure it starts with `ws://` rather than `http://` or `tcp://`.

It's located in the `CallsModule`, at the bottom of the file.

![Replacing the host part](https://user-images.githubusercontent.com/17215808/192728812-38652de7-ba5d-4a7e-83a0-d2efe2f84c2b.png)

Once that's done, you'll be ready to connect to the WebSocket!

#### Redirect URLs for the SFU

Finally, you have to set up the local SFU URL. To do this, run the SFU server and host its API using **ngrok**:

```
ngrok http 3031
```

Once you have this ready, you can look for the `REDIRECT_SIGNAL_URL` constant in the Android project and replace it with your generated URL.

It's located in the `WebRTCModule`, at the bottom of the file.

You should be all set, and you can now run the sample app from any PC, while hosting on another, or from the same PC directly.

Just run the sample app on real Android devices and you should be good to go!