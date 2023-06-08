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

### 🔗 Quick Links

* [Register](getstream.io/video/trial): Create an account and get an API key for Stream Video and (optionally) Stream Chat.
* [Video Tutorial](https://getstream.io/video/docs/android/basics/tutorial/): Learn what the easiest way to integrate our Video SDK is and how to build a Video app in just a few minutes.
* [SDK Documentation](https://getstream.io/video/docs/android/): Read through our documentation to learn about different ways of using and customizing the Video SDK.
* [Chat + Video Template project](https://github.com/GetStream/stream-video-android/tree/main/examples/chat-with-video/chat-with-video-final): Use our template Chat + Video project as a framework for any apps that focus on chat primarily and video second. Examples of apps are Messenger, WhatsApp, Telegram and more.
* Video + Chat Template project: Use our template Video + Chat project as an alternate framework, where your app focuses primarily on Video, with secondary Chat features. Examples of apps are Zoom, Google Meet and similar.
* API Docs: All of our code has well maintained documentation which is published using `Dokka`. 
* [Video Team Planning project](https://github.com/orgs/GetStream/projects/15): All of our sprint planning, feature requests, backlog, blocker and in progress items are visible in our GitHub project. 

## 🗺️ Overview & Documentation 📚

The SDK consists of several artifacts, based on the depth of customization and manual control you want to achieve:

* `stream-video-android-core`: Represents the core functionality of the SDK, such as establishing communication with the main API, creating and joining `Call`s, looking up `User`s and receiving various events.
* `stream-video-android-compose`: The Jetpack Compose Components artifact that contains all out-of-the-box screens as well as smaller building blocks that let you implement rich UI features based on the `stream-video-android-core` SDK state.

## 🛠️ Installation & Getting Started 🚀

You can find the easiest way to get started with our SDK in our [Basics](https://getstream.io/video/docs/android/basics) documentation section.

## 🔮 Sample Apps

We have several different sample apps and example kits you can use to explore our SDK integration. These cater to both Jetpack Compose and XML UI toolkit. That way, you can choose which sample suits your preferred programming style and technology.

### 🧩 Jetpack Compose Sample App

Our Jetpack Compose implementation comes with its own [example app](https://github.com/GetStream/stream-video-android/tree/main/app) where you can explore our Video SDK using the Compose Components.

To run the sample app, clone this repository:

````shell
git clone git@github.com:GetStream/stream-video-android.git
````

Next, open [Android Studio](https://developer.android.com/studio) and open the cloned project. Then simply run the `app` module on a device or emulator.

### 🧱 XML Sample App

Similarly to Jetpack Compose, we have an XML implementation with its own [example app](https://github.com/GetStream/stream-video-android/tree/main/xml-app) where you can explore our Video SDK using the XML Components.

To run the sample app, clone this repository:

````shell
git clone git@github.com:GetStream/stream-video-android.git
````

Next, open [Android Studio](https://developer.android.com/studio) and open the cloned project. Then simply run the `xml-app` module on a device or emulator.

## 🏗️ Template Projects

To allow easier integration of our Chat and Video SDKs as well as some other features like deep linking, push notifications and more, we provided template projects that you can clone and use as a foundation for your app.

Currently we expose the following templates:

* [Chat + Video](https://github.com/GetStream/stream-video-android/tree/main/examples/chat-with-video/chat-with-video-final): Useful for Messenger, Telegram, WhatsApp and similar type of apps. It sets up our Chat SDK in a way that it supports video calling as custom attachments and integration within the UI, focusing on Chat primarily, with Video being a secondary aspect.
* [Video + Chat](https://github.com/GetStream/stream-video-android/tree/main/examples/video-with-chat/video-with-chat-final): As a contrast for Chat + Video, this template is useful for e-meeting applications, such as Zoom and Google Meet. It sets up the Video SDK in a way that users primarily create Video meetings, that as a secondary aspect allow members to engage in text communication using our Chat SDK.

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

## License

```
Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.

Licensed under the Stream License;
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   https://github.com/GetStream/stream-video-android/blob/main/LICENSE

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```