# Stream Video Android

Stream Video repository for Android.


# Introduction

Stream Video Android contains the following parts below:

- [Square's wire](https://github.com/square/wire/), which is gRPC and protocol buffers for Android, Kotlin, and Java.
- LiveKit wrapper.
- Sample app using LiveKit.
- Sample app using lower level WebRTC client.

## Square's wire

[Square's wire](https://github.com/square/wire/) contains dedicated compiler that interprets and generates interfaces for our service RPCs defined in our protobuf schema. 
Following [their guide](https://square.github.io/wire/wire_grpc/), wire gPRC compiler will generate PRC interfaces easily by adding the wire plugin. <br>

By adding our protocol buffer schemes into the `stream-video-kotlin` module's [/src/main/proto](https://github.com/GetStream/video-android/tree/main/stream-video-kotlin/src/main/proto) package, it will generate codes in the `stream.video` package as the below:

![generated-codes](https://user-images.githubusercontent.com/24237865/177896853-32ef6d67-2566-49aa-b759-7b8f8a660007.png)


## Sample app with LiveKit


## WebRTC sample app

