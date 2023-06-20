# Official Android SDK for [Stream Video](https://getstream.io/video/docs/)

// TODO - image for the SDK

<p align="center">
  <a href="https://github.com/GetStream/stream-video-android/actions"><img src="https://github.com/GetStream/stream-video-android/workflows/App%20Distribute%20CI/badge.svg" /></a>
  <a href="https://android-arsenal.com/api?level=21"><img alt="API" src="https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat"/></a>
  <a href="https://search.maven.org/search?q=g:%22io.getstream%22%20AND%20a:%22stream-video-android%22"><img src="https://img.shields.io/maven-central/v/io.getstream/stream-video-android-core.svg?label=Maven%20Central" /></a>
</p>

This is the official Android SDK for Stream Video.

## What is Stream?

Stream allows developers to rapidly deploy scalable feeds, chat messaging and video with an industry leading 99.999% uptime SLA guarantee.

With Stream's video components, you can use their SDK to build in-app video calling, audio rooms, audio calls, or live streaming. The best place to get started is with their tutorials:

- Video & Audio Calling Tutorial
- Audio Rooms Tutorial
- Livestreaming Tutorial

Stream provides UI components and state handling that make it easy to build video calling for your app. All calls run on Stream's network of edge servers around the world, ensuring optimal latency and reliability.

## 👩‍💻 Free for Makers 👨‍💻

Stream is free for most side and hobby projects.
To qualify, your project/company needs to have < 5 team members and < $10k in monthly revenue.
Makers get $100 in monthly credit for video for free.

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

## Roadmap

Video roadmap and changelog is available [here](https://github.com/GetStream/protocol/discussions/127). 

### 0.2 milestone

- [ ] Livestream tutorial
- [ ] Deeplink support for video call demo & dogfooding app (skip auth for the video demo, keep it for dogfooding)
- [ ] Chat Integration
- [ ] XML version of VideoRenderer
- [ ] Call Analytics stateflow
- [ ] Automatically handle pagination and sorting on > 6 participants 
- [X] Reactions

### 0.3 milestone

- [ ] Dynascale 2.0 (codecs, f resolution switches, resolution webrtc handling)
- [ ] Test coverage
- [ ] Testing on more devices
- [ ] Audio & Video filters
- [ ] Android SDK development.md cleanup
- [X] SDK development guide for all teams

### 0.4 milestone

- [ ] Analytics integration
- [ ] Screensharing from mobile
- [ ] Tap to focus
- [ ] Camera controls
- [ ] Picture of the video stream at highest resolution