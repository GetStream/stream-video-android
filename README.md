# Official Android SDK for Stream Video

This is the official Android SDK for [Stream Video](https://getstream.io/video/docs/android/basics/tutorial/), a service for building powerful Audio and Video communication applications. The library includes several layers of control and customizability. Most users will want to start with our higher level components that let you put together UI, services like push notifications, Picture-In-Picture mode, background support and more.

Once you're familiarized with the SDK, you can rely on the low-level-client and build fully custom UI and behavior based on the state we provide.

The Android SDK supports both Kotlin and Java, as well as Jetpack Compose and XML for UI. We _strongly recommend_ using Kotlin and Jetpack Composem as they provide the most flexibility, customizabilit< and the best developer experience.

### Quick Links

* [Register](getstream.io/video/trial): Create an account and get an API key for Stream Video and (optionally) Stream Chat.
* [Video Tutorial](https://getstream.io/video/docs/android/basics/tutorial/): Learn what the easiest way to integrate our Video SDK is and how to build a Video app in just a few minutes.
* [SDK Documentation](https://getstream.io/video/docs/android/): Read through our documentation to learn about different ways of using and customizing the Video SDK.
* [Chat + Video Template project](https://github.com/GetStream/stream-video-android/examples/chat-with-video/chat-with-video-final): Use our template Chat + Video project as a framework for any apps that focus on chat primarily and video second. Examples of apps are Messenger, WhatsApp, Telegram, Viber and more.
* Video + Chat Template project: Use our template Video + Chat project as an alternate framework, where your app focuses primarily on Video, with secondary Chat features. Examples of apps are Zoom, Google Meet and similar.
* API Docs: All of our code has well maintained documentation which is published using Dokka. 
* [Video Team Planning project](https://github.com/orgs/GetStream/projects/15): All of our sprint planning, feature requests, backlog, blocker and in progress items are visible in our GitHub project. 

## Overview & Documentation

The SDK consists of several artifacts, based on the depth of customization and manual control you want to achieve:

* `stream-video-android-core`: Represents the core functionality of the SDK, such as establishing communication with the main API, creating and joining `Call`s, looking up `User`s and receiving various events.
* `stream-video-android-compose`: The Jetpack Compose Components artifact that contains all out-of-the-box screens as well as smaller building blocks that let you implement rich UI features based on the `stream-video-android-core` SDK state.
* `stream-video-android-xml`: The XML Components artifact that contains all out-of-the-box screens as well as smaller building blocks that let you implement rich UI features based on the `stream-video-android-core` SDK state.

## Installation & Getting Started

You can find the easiest way to get started with our SDK in our [Basics](https://getstream.io/video/docs/android/basics) documentation section.

## Sample Apps

## Template Projects

## Supported Features

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
* Flipping camera in calls
* Enabling and disabling speakerphone in calls
* Seeing all call participants
* Push notification providers support
* Automatic Incoming/Outgoing call prompts based on state changes
* Call configuration customization (timeouts, automatic rejections, showing prompts...)
* Deeplinking support