# Official Android SDK for [Stream Video](https://getstream.io/video/docs/)

<img src=".readme-assets/Github-Graphic-Android.jpg" alt="Stream Video for Android Header image" style="box-shadow: 0 3px 10px rgb(0 0 0 / 0.2); border-radius: 1rem" />

<p align="center">
  <a href="https://github.com/GetStream/stream-video-android/actions"><img src="https://github.com/GetStream/stream-video-android/workflows/App%20Distribute%20CI/badge.svg" /></a>
  <a href="https://android-arsenal.com/api?level=24"><img alt="API" src="https://img.shields.io/badge/API-24%2B-brightgreen.svg?style=flat"/></a>
  <a href="https://search.maven.org/search?q=stream-video-android"><img src="https://img.shields.io/maven-central/v/io.getstream/stream-video-android-core.svg?label=Maven%20Central" /></a>
</p>

This is the official Android SDK for [Stream Video](https://getstream.io/video?utm_source=Github&utm_medium=Github_Repo_Content_Ad&utm_content=Developer&utm_campaign=Github_Android_Video_SDK&utm_term=DevRelOss), a service for building video calls, audio rooms, and live-streaming applications. This library includes both a low-level video SDK and a set of reusable UI components.
Most users start with the Compose UI components and fall back to the lower-level API when they want to customize things.

<a href="https://getstream.io?utm_source=Github&utm_medium=Github_Repo_Content_Ad&utm_content=Developer&utm_campaign=Github_Android_Video_SDK&utm_term=DevRelOss">
<img src="https://user-images.githubusercontent.com/24237865/138428440-b92e5fb7-89f8-41aa-96b1-71a5486c5849.png" align="right" width="12%"/>
</a>

## 🛥 What is Stream?

Stream allows developers to rapidly deploy scalable feeds, chat messaging and video with an industry leading 99.999% uptime SLA guarantee.

Stream provides UI components and state handling that make it easy to build video calling for your app. All calls run on Stream's network of edge servers around the world, ensuring optimal latency and reliability.

## 📕 Tutorials

With Stream's video components, you can use their SDK to build in-app video calling, audio rooms, audio calls, or live streaming. The best place to get started is with their tutorials:

- **[Video & Audio Calling Tutorial](https://getstream.io/video/docs/android/tutorials/video-calling/)**
- **[Audio Rooms Tutorial](https://getstream.io/video/docs/android/tutorials/audio-room/)**
- **[Livestreaming Tutorial](https://getstream.io/video/docs/android/tutorials/livestream/)**

If you're interested in customizing the UI components for the Video SDK, check out the **[UI Cookbook](https://getstream.io/video/docs/android/ui-cookbook/overview/)**.

## 📱 Previews

<p align="center">
<img src="https://github.com/GetStream/stream-video-android/assets/24237865/b7ffc91c-10c5-4d02-8714-d18a5b4c9e00" width="32%"/>
<img src="https://github.com/GetStream/stream-video-android/assets/24237865/a5741658-8f51-4506-92b7-c0e8098725f3" width="32%"/>
<img src="https://github.com/GetStream/stream-video-android/assets/24237865/3cc08121-c8c8-4b71-8a96-0cf33b9f2c68" width="32%"/>
</p>

## 👩‍💻 Free for Makers 👨‍💻

Stream is free for most side and hobby projects. To qualify, your project/company needs to have < 5 team members and < $10k in monthly revenue. Makers get $100 in monthly credit for video for free.
For more details, check out the [Maker Account](https://getstream.io/maker-account?utm_source=Github&utm_medium=Github_Repo_Content_Ad&utm_content=Developer&utm_campaign=Github_Android_Video_SDK&utm_term=DevRelOss).

## 💡Supported Features💡

Here are some of the features we support:

* Developer experience: Great SDKs, docs, tutorials and support so you can build quickly
* Edge network: Servers around the world ensure optimal latency and reliability
* Chat: Stored chat, reactions, threads, typing indicators, URL previews etc
* Security & Privacy: Based in USA and EU, Soc2 certified, GDPR compliant
* Dynascale: Automatically switch resolutions, fps, bitrate, codecs and paginate video on large calls
* Screensharing
* Picture in picture support
* Active speaker
* Custom events
* Geofencing
* Notifications and ringing calls
* Opus DTX & Red for reliable audio
* Webhooks & SQS
* Backstage mode
* Flexible permissions system
* Joining calls by ID, link or invite
* Enabling and disabling audio and video when in calls
* Flipping, Enabling and disabling camera in calls
* Enabling and disabling speakerphone in calls
* Push notification providers support
* Call recording
* Broadcasting to HLS

## 🗺️ Roadmap

Video roadmap and changelog is available [here](https://github.com/GetStream/protocol/discussions/127). 

### 0.2.0 milestone

- [ ] Call Analytics stateflow (Thierry)
- [ ] Ringing: Make it easy to test
- [ ] Example Button to switch speakerphone/earpiece
- [ ] Ringing: Make a list of what needs to be configurable
- [ ] Ringing: Sounds for incoming, outgoing, call timed out. Sound for someone joining a call (Disabled by default). Docs on how to change them
- [ ] Publish app on play store
- [ ] Bug: Screensharing on Firefox has some issues when rendering on android (Daniel)
- [X] Chat Integration (Jaewoong)
- [X] Automatically handle pagination and sorting on > 6 participants in the sample app (Daniel)
- [X] Buttons to simulate ice restart and SFU switching (Jaewoong)
- [X] Bug: java.net.UnknownHostException: Unable to resolve host "hint.stream-io-video.com" isn't throw but instead logged as INFO (Daniel)
- [X] Bug: Call.join will throw an exception if error is other than HttpException
- [X] Report version number of SDK on all API calls (Daniel)
- [X] Bug: screensharing is broken. android doesn’t receive/render (not sure) the screenshare. video shows up as the gray avatar
- [X] support settings.audio.default_device (Daniel)
- [X] Bug: Sample app has a bug where we don't subscribe to call changes, we need to use call.get in the preview screen so we know the number of participants (Daniel)
- [X] Buttons to simulate ice restart and SFU switching (Jaewoong)
- [X] Local Video disconnects sometimes (ICE restarts issue for the publisher. we're waiting for the backend support) (Thierry)
- [X] Deeplink support for video call demo & dogfooding app (skip auth for the video demo, keep it for dogfooding) (Jaewoong)
- [X] XML version of VideoRenderer (Jaewoong)
- [X] sortedParticipants stateflow doesn't update accurately (Thierry)
- [X] Reactions
- [X] Bug: screenshare is not removed after it stops when a participant leaves the call (Thierry) (probably just dont update the state when the participant leaves)

### 0.3.0 milestone

- [ ] Ringing: Make it easy to test
- [ ] Pagination on query members & query channels
- [ ] Audio & Video filters (Daniel)
- [ ] Livestream tutorial (depends on RTMP support)
- [ ] H264 workaround on Samsung 23 (see https://github.com/livekit/client-sdk-android/blob/main/livekit-android-sdk/src/main/java/io/livekit/android/webrtc/SimulcastVideoEncoderFactoryWrapper.kt#L34 and
- https://github.com/react-native-webrtc/react-native-webrtc/issues/983#issuecomment-975624906)
- [ ] Dynascale 2.0 (codecs, f resolution switches, resolution webrtc handling)
- [ ] Test coverage
- [ ] Testing on more devices
- [ ] local version of audioLevel(s) for lower latency audio visualizations(Daniel)
- [ ] Android SDK development.md cleanup (Daniel)
- [ ] Logging is too verbose (rtc is very noisy), clean it up to focus on the essential for info and higher
- [X] Speaking while muted stateflow (Daniel)
- [X] Bluetooth reliability
- [X] Cleanup the retry behaviour in the RtcSession
- [X] SDK development guide for all teams

### 0.4.0 milestone

- [ ] Screensharing from mobile
- [ ] Tap to focus
- [ ] Camera controls
- [ ] Picture of the video stream at highest resolution
- [ ] Review foreground service vs backend for some things like screensharing etc

## 💼 We are hiring!

We've recently closed a [\$38 million Series B funding round](https://techcrunch.com/2021/03/04/stream-raises-38m-as-its-chat-and-activity-feed-apis-power-communications-for-1b-users/) and we keep actively growing.
Our APIs are used by more than a billion end-users, and you'll have a chance to make a huge impact on the product within a team of the strongest engineers all over the world.
Check out our current openings and apply via [Stream's website](https://getstream.io/team/#jobs).

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
