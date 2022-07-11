# Stream Video Android

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


## WebRTC sample app

