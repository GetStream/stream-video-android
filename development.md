
## Build process

- Snapshots build are created automatically from the "development" branch. Configuration.kt has the version number
- Testing app. Merge code into "main". Github will build a new zipped apk. You can upload a new testing app here: https://console.firebase.google.com/project/stream-video-9b586/appdistribution/app/android:io.getstream.video.android.dogfooding/releases
- Release versions are created when you create a new release tag on github
- TODO: Updating the sample app in play store?

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

Clone the protocol
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

### Advanced use cases (Most people don't need this)
* If you want to use the locally checked out `protocol` repository, then use `LOCAL_ENV=1` with the generate command.
* (For stream developers only) If you want to directly get it from the coordinator repository then use `OPENAPI_ROOT_USE_COORDINATOR_REPO=1`
  switch along with `LOCAL_ENV=1` with the generate command. This is a shortcut.

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
* StreamVideoImpl.developmentMode determines if we should log an error or fail fast. 
Typically for development you want to fail fast and loud. For production you want to ignore most non-critical errors.
* PersistentSocket is subclassed by CoordinatorSocket and SfuSocket. It keeps a websocket connection

### State management

* All events are routed via the StreamVideoImpl.subscribe method. Both the SFU & Coordinator events
* Based on these events the following state is updated: client.state, call.state, member and participant state
* client.fireEvent is used for firing local events and testing
* client.handleEvent updates client state, call state and after that calls any listeners

## WebRTC layer

* RtcSession maintains all the tracks and the webrtc logic
  https://www.notion.so/stream-wiki/WebRTC-SFU-Setup-37b5a4a260264d3f92332774e5ec9ee9#ee96064deb73480383f6be2a6a36a315

### RTC offer/answer cycle

* sessionId is created locally as a random UUID
* create two peer connections (publisher and subscriber)
* capture audio and video (if we're not doing so already, in many apps it should already be on for the preview screen)
* execute the join request
* add the audio/video tracks which triggers onNegotiationNeeded
* onNegotiationNeeded(which calls SetPublisherRequest)
* JoinCallResponseEvent returns info on the call's state

Camera/device changes -> listener in ActiveSFUSession -> updates the tracks.

### RTC dynascale

** Part one **
We try to render the video if participant.videoEnabled is true
VideoRenderer is responsible for marking:
- the video as visible
- the resolution that the video is rendered at
And calls RtcSession.updateTrackDimensions

All resolutions are stored in RtcSession.trackDimensions. 
Which is a map of participants, track types and resolutions that the video is displayed at.

Whenever visibility or resolutions change, we call updateParticipantSubscriptions
- It runs debounced (100ms delay)
- It only calls update subscriptions if the list of tracks we sub to changed

This ensures that we can run large tracks (since video is lazy loaded at the right resolution)

** Part two **

The SFU tells us what resolution to publish using the ChangePublishQualityEvent event.
So it will say: disable "f" (full) and only send h and q (half and quarter)


### ParticipantState

* Participants have a session id. the session id is unique
* Each participant has a trackPrefix
* New media streams have a streamID, which starts with the trackPrefix
  val (trackPrefix, trackType) = mediaStream.id.split(':');
* Note that members are unique per user, not per session

## Compose

Some of our customers will include the SDK and don't customize things.
But the majority will either customize our UI components and partially or entirely build their own UI. 

Because of this we need to make sure that our examples don't hide how the SDK works.

For example this is bad:

```
CallComposable() 
```

A better approach is to show the underlying components, so people understand how to swap them out

```
Call {
  ParticipantGrid(card= { ParticipantCard() })
  CallControls {
    ChatButton()
    FlipVideoButton()
    MuteAudioButton()
    MuteVideoButton()
  }
}
```

The second approach is better since:

* It clearly shows how to change the buttons if you want to
* It shows how to change the participant card. Let's say you don't want to show names, or hide the network indicator etc.
* Or if you want an entirely different layout of the participants
* Or perhaps have the buttons in a bottom bar instead of an overlay

With the second approach everything is easy to understand and customize.

### Ringing

* Push notifications or the coordinator WS can trigger a callCreatedEvent with ring=true
* The UI should show an incoming call interface
* Clicking accept or reject fires triggers the accept/reject API endpoints
* Call members have an accepted_at, rejected_at field

### Media Manager

The media manager should support capturing local video with or without joining a call

* Camera2Capturer starts capture the video
* It captures it on SurfaceTextureHelper
* The webrtc part needs to know the video capture resolution

### Dogfooding vs Demo App

* dogfooding has google authentication. demo app has no authentication
* demo app allows you to type in the call id and join, or create a new
* dogfooding joins via a url deeplink

### Dynascale

Sfu -> Client
- ChangePublishQualityEvent triggers RtcSession.updatePublishQuality. It enables/disables different resolutions in the simulcast

Client -> Sfu
- updateParticipantsSubscriptions subscribes to the participants we want to display
- call.initRender tracks the resolution we render the video at. It writes it to updateParticipantTrackSize

Question:
- How do we know if a given video element is visible or not?
- https://developer.android.com/jetpack/compose/side-effects#disposableeffect
- https://proandroiddev.com/the-life-cycle-of-a-view-in-android-6a2c4665b95e (onDetachedFromWindow)
- Our VideoRenderer removes the sink. But it doesn't unsubcribe from the SFU.
- Requirements for any custom video renderer: Informs the state of the size it's rendering at, informs if it's visible or not

Current issues
- The view layer doesn't update the state to mark a participant as not displayed
- updateParticipantsSubscriptions should run whenever the UI changes with a debounce of 100ms or so

Goals
- A customer integrating without the SDK knowing the size and visibility of video elements will lead to crashes in large calls. So we should prevent that.

### Knowing if participant is publishing tracks

- JoinCallResponse, ParticipantJoinedEvent contain published_tracks
- TrackPublishedEvent, TrackUnpublishedEvent
- Locally you call updateMuteState. It's not clear what this does. 
Is muting connected to track publish/unpublish?

### Retry Behaviour

There are many things that can go wrong in a video call
- The websocket connections with the SFU or coordinator can fail
- Any API call to the coordinator can fail
- Any API call to the SFU can fail
- Camera and Audio can fail and require a restart
- The publisher or subscriber peer connection can fail
- Video codec can fail

If failures are temporary we should retry them.
If the SFU is down, we need to switch to a different SFU.
Alternatively when a SFU is gracefully shutting down we should receive an event

Detecting SFU outages
- If I can reach the Coordinator but not the SFU that's clear sign the SFU is down
- What's the easiest way to detect this?

In most cases the subscriber peer connection will fail first since we have constant traffic on it
Detecting a broken peer connection

TODO:

[ ] - Socket retry behaviour check
[ ] - Logic to switch to a new SFU
[ ] - Retry camera on error
[ ] - Detect SFU outage

### Participant State

Basically there are 2 possible approaches

Option 1 - Call has a participants stateflow & the participant state is a data class
Option 2 - Call has a participants stateflow, and each participant state has stateflows for its properties

So participant.audioLevel can be a stateflow or a float property.

Option 1:
- Pro: less stateflow objects
- Con: frequent updates to the "participants" stateflow

Option 2:
- Pro: more finegrained UI changes. only the audio level changes if that's the one thing that changes
- Con: more stateflow objects

What's better seems to depend on internal optimizations in Compose
- How well does it's diffing algorithm work
- How well optimized are multiple stateflows & observing them

### Debugging video quality

- If you search for "video quality" in the logs you'll see the selected and max resolution
- Next if you search for input_fps in the logs, you'll see any reasons the quality is limited by webrtc
- Call:WebRTC has more data
- Peer connection stats also have more info
- Adding a debugger in DebugInfo can also help
- Also see MediaManager.selectDesiredResolution

### Speaker/ audio playback

- StreamPeerConnectionFactory calls peerconnection.setAudioDeviceModule (this enables some noise removal if hardware supports it)
- MediaManager wraps the audio handler
- AudioHandler starts the audio switch
- AudioSwitch does most of the work
- AudioManagerAdapterImpl. not sure

When we have time we should rebuild this with:
- Stateflow
- Coroutines

### Coroutines & Tests

There are 2 common ways how you can get your tests that use coroutines to hang forever

Scenario 1 - Tests block:
- Never use Dispatchers.Main/IO directly. Always use DispatcherProvider.IO/Main
- Solution: Replace Dispatchers.Main with DispatcherProvider.Main

Scenario 2 - Tests block:
- Inside of runTest the coroutine dispatcher waits for all coroutines to finish
- The video client creates 3 health monitors. The healthmonitors run continously (while(true))
- So if you create a client without calling client.cleanup() inside of a test
- Your tests will wait endlessly for the health monitors to finish
- Solutions: Call client.cleanup() at the end of your test. See ClientAndAuthTest

### Docs

Run this for a local preview of the docs:
npx stream-chat-docusaurus -i -s