# Stream Video Android

[![Android CI](https://github.com/GetStream/video-android/actions/workflows/android.yml/badge.svg)](https://github.com/GetStream/video-android/actions/workflows/android.yml)

Stream Video repository for Android.


# Introduction

Stream Video Android contains the following parts below:

- [Square's wire](https://github.com/square/wire/), which is gRPC and protocol buffers for Android, Kotlin, and Java.
- LiveKit wrapper.
- Sample app using LiveKit.
- Sample app using lower level WebRTC client.

## Square's wire

[Square's wire](https://github.com/square/wire/) contains dedicated compiler that interprets and generates interfaces for our Domain and API models defined in our protobuf schema.

By adding our protocol buffer schemes into the `stream-video-android` module's [/src/main/proto](https://github.com/GetStream/video-android/tree/main/stream-video-kotlin/src/main/proto) package, it will generate code in the `stream.video` package as the below:

![generated-codes](https://user-images.githubusercontent.com/17215808/178219855-18d27ad6-dacb-4ccb-b392-4b032338f53f.png)

You don't have to do anything yourself to generate the models, simply build the project and the Wire Gradle plugin takes care of everything.

## API Communication

Unfortunately, Android doesn't have a clear and easy way of generating HTTP services from RPC definitions in proto.

For this reason, we manually implemented the `CallCoordinatorService`, using [Retrofit](https://square.github.io/retrofit/) and its [Wire parsers](https://github.com/square/retrofit/blob/master/retrofit-converters/wire/src/main/java/retrofit2/converter/wire/WireConverterFactory.java).

That way, we don't have to write any parsing code, everything is done automatically for encoding/decoding.

## Sample app with LiveKit

We have a small sample app implemented with LiveKit. It currently hosts the following screens:
- Login screen
- Call screen

We'll add more steps to the sample to make it nicer in the following weeks.

### Login Screen

![login-screen](https://user-images.githubusercontent.com/17215808/180763094-f19c8644-45f4-41b6-902f-c9af53fdf6c9.png)

The Login screen allows you to choose between a few harcoded users and lets you put in the ID of the call you want to join.

Simply select any user and write the call ID and you can join a call!

> **Note**: We plan to expand this flow to add a way to choose between starting and joining calls, as an "intermediate" home page screen.

### Call Screen

![call-screen](https://user-images.githubusercontent.com/17215808/180763075-676dcb28-fe01-4355-b2c7-839a7a101a3a.png)

The call screen is very simple - at the top, we show the currently active speaker and their track. We also show a list of participants under the main stage.

There are also a few options available to the user:
* Mute Audio
* Leave Call
* Mute Video
* Flip Camera

All of these UI elements are tied into reactive constructs and these should update based on the state.

## Running Samples

There are caveats when running the sample app. There are currently two ways to run the app and test the API and the video call SDK.

Bear in mind that both of these approaches require you to start the [local server](https://github.com/GetStream/video) to connect to the API.

### Using Emulators

If you're using a non-M1 CPU, you should be fine by simply running the app on an emulator. Make sure to set up hardware acceleration/virtualization based on your OS.

* [Windows](https://developer.android.com/studio/run/emulator-acceleration#vm-windows)
* [Linux](https://developer.android.com/studio/run/emulator-acceleration#vm-linux)
* [Mac](https://developer.android.com/studio/run/emulator-acceleration#vm-mac)

Then simply run the application in Android Studio.

### Using Real Devices

If you're using an M1 device, you have a weak PC or you simply want better video examples by using real cameras, we suggest using a real device, if you have them lying around.

However, connecting to **localhost** using real devices is not as easy on Android, as you have to connect to the IP of the device that's hosting it while being on the same network. This is more difficult because Android restricts `http://` access which can cause issues with connecting to the API.

And if you change the hosting device or the IP changes, you'd have to update the IP.

That's why we recommend hosting redirect URLs that let you connect to the server even if its IP changes, or if you change the device from which you're building to your devices.

To do this, you can use [ngrok](https://ngrok.com/docs/getting-started). It's a tool that helps you host your API calls on a temporary public URL that you can hit with your client. Once the API route is called, it will redirect from the ngrok URL to your localhost.

It's available for Windows, Linux and Mac. Once you install it, you can set up the redirect URLs.

#### Redirect URLs

There are a few URLs you'll need to set up for the redirect using `ngrok`:

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

Copy the highlighted URL and search the Android Studio project for `stream-video-android` for `REDIRECT_BASE_URL` and replace the value with the given URL.

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

Then take the URL provided by the tool and replace the `REDIRECT_WS_BASE_URL`. Make sure it starts with `ws://` rather than `http://` or `tcp://`.

You should be all set, and you can now run the sample app from any PC, while hosting on another, or from the same PC directly.

Just run the sample app on real Android devices and you should be good to go!

## WebRTC sample app

