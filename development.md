
## For Go devs

```
cp env.properties.sample .env.properties
```

Edit the file and set CORE_TEST_LOCAL = 1 to run against your local infra.

## Build vars

You can copy environment variables like this:
```
cp env.properties.sample .env.properties
```

build.gradle.kts for each repo reads the .env.properties file and translated it into
build variables. So CORE_TEST_LOCAL in the stream-video-android-core is turned into

```kotlin
BuildConfig.CORE_TEST_LOCAL
```

## Style guidelines

* Keep it simple. Keep it simple. Keep it simple.
* Interfaces can help make your code easier to test. That doesn't mean you should create an interface for everything though.
* Factories are nice, but many times a constructor works just as well
* Only create an interface if you need it for testing, start without it. Note that android studio allows you to extract an interface.
* Integration testing and unit testing are important. It's often more productive to write tests than to try to validate things work by opening up the API
* Our customers should be able to build any type of video/audio experience. Keep that in mind when working on the APIs. They need to be flexible and easy to use
* Kotlin has excellent support for functional programming. The functional style is easy to write, but hard to read. Don't overdo it

## OpenAPI build

Clone the protocl
```bash
```

Run the generate openapi
```bash
./generate-openapi.sh
```

Note that now you have your generated files here:
* ~/workspace/generated/

The code for android is here
* ~/workspace/stream-video-android/

You can see the diff here

```bash
diff -rq ~/workspace/generated/src/main/kotlin/org/openapitools/client ~/workspace/stream-video-android/src/main/kotlin/org/openapitools/client
```

* Protocol is visible here: https://getstream.github.io/protocol/
* https://www.notion.so/Getting-Started-to-Video-for-Android-Developers-be5ae7e2e9584f78b757163ecff1033b

## Writing integration tests

The base for integration testing is the `IntegrationTestBase` class.
It offers convenient helpers for test data, client and clientImpl access, and testing events.

For unit test the `TestBase` class is used. Running the TestBase class is faster and more reliable than running the IntegrationTestBase class.
Typically a combination of integration testing and unit testing is best.

We use Truth and Mockk for testing

Here's an example test

```kotlin
@RunWith(RobolectricTestRunner::class)
public class MyTest : IntegrationTestBase() {
    @Test
    fun `create a call and verify the event is fired`() = runTest {
        // create the call
        val call = client.call("default", randomUUID())
        val result = call.create()
        assertSuccess(result)
        // Wait to receive the next event
        val event = waitForNextEvent<CallCreatedEvent>()
    }
}
```

Check the docs on TestBase, TestHelper and IntegrationTestBase for more utility functions for testing

## Architecture

### API calls

* StreamVideoImpl makes the API calls to the coordinator. Internally there are 4 retrofit APIs it calls
* CallClient makes the API calls to the SFU on the edge

### State management

* All events are routed via the StreamVideoImpl.subscribe method. Both the SFU & Coordinator events
* Based on these events the following state is updated: client.state, call.state, member and participant state
* client.fireEvent is used for firing local events and testing
* client.handleEvent updates client state, call state and after that calls any listeners

## WebRTC layer

* Ideally you setup your camera tracks and audio tracks before you join a call
* Otherwise you end up joining and immediately triggering onNegotiationNeeded after that

### RTC offer/answer cycle

* sessionId is created locally as a random UUID
* create the peer connections
* capture audio and video (if we're not doing so already, in many apps it should already be on for the preview screen)
* execute the join request
* add the audio/video tracks which triggers onNegotiationNeeded
* onNegotiationNeeded(which calls SetPublisherRequest)
* JoinCallResponseEvent returns info on the call's state

Camera/device changes -> listener in ActiveSFUSession -> updates the tracks.

### RTC dynascale

* We send what resolutions we want using UpdateSubscriptionsRequest. 
  * It should be triggered as we paginate through participants
  * Or when the UI layout changes
* The SFU tells us what resolution to publish using the ChangePublishQualityEvent event

### ParticipantState

* Participants have a session id. the session id is unique
* Each participant has a trackPrefix
* New media streams have a streamID, which starts with the trackPrefix
  val (trackPrefix, trackType) = mediaStream.id.split(':');
* Note that members are unique per user, not per session

## Compose

