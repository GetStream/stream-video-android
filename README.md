# Official Android SDK for [Stream Video](https://getstream.io/video/docs/)

// TODO - image for the SDK

<p align="center">
    <a href="https://github.com/GetStream/stream-video-android/actions"><img src="https://github.com/GetStream/stream-video-android/workflows/App%20Distribute%20CI/badge.svg" /></a>
  <a href="https://android-arsenal.com/api?level=21"><img alt="API" src="https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat"/></a>
  <a href="https://github.com/GetStream/stream-video-android/releases"><img src="https://img.shields.io/github/v/release/GetStream/stream-video-android" /></a>
</p>

This is the official Android SDK for [Stream Video](https://getstream.io/video/docs/android/basics/tutorial/), a service for building powerful Audio and Video communication applications. The library includes several layers of control and customizability. Most users will want to start with our higher level components that let you put together UI, services like push notifications, Picture-In-Picture mode, background support and more.

Once you're familiarized with the SDK, you can rely on the low-level-client and build fully custom UI and behavior based on the state we provide.

The Android SDK supports both Kotlin and Java, as well as Jetpack Compose and XML for UI. We _strongly recommend_ using Kotlin and Jetpack Compose as they provide the most flexibility, customizability and the best developer experience.

## 💡Supported Features💡

Our Video SDK ships with a plethora of cool and modern features. Here are just some of the main functions we support in our SDK:

* Creating ringing and meeting calls
* Joining calls by ID, link or invite
* Browsing calls for user
* Inviting people to calls
* Incoming call prompt
* Outgoing call UI
* Observing call state
* Background call support
* Picture-In-Picture mode support
* Enabling and disabling audio and video when in calls
* Flipping, Enabling and disabling camera in calls
* Enabling and disabling speakerphone in calls
* Rendering all call participants
* Push notification providers support
* Automatic Incoming/Outgoing call prompts based on state changes
* Call configuration customization (timeouts, automatic rejections, showing prompts...)
* Variable video quality streaming
* Call recording
* Screen sharing display
* Portrait and landscape UI configuration support


## Roadmap

### 0.2 milestone

- [ ] Deeplink support for video call demo & dogfooding app (skip auth for the video demo, keep it for dogfooding)
- [ ] Chat Integration
- [ ] XML version of VideoRenderer
- [X] Reactions

### 0.3 milestone

- [ ] Test coverage
- [ ] Testing on more devices
- [ ] Audio & Video filters
- [ ] SDK development guide for all teams
- [ ] Android SDK development.md cleanup

### 0.4 milestone

- [ ] Analytics integration
- [ ] Screensharing