
## Timeline

*  Week 1: Refactor LLC & State. Setup testing
*  Week 2: LLC & State Stability. Compose testing & previews
*  Week 3: Sample app, update compose to LLC & State changes. New events, socket & token provider. Call UI. Guest users & moderation endpoints
*  Week 4: LLC & state test coverage + Demo & Dogfooding apps + RTC & Media fixes
*  Week 5: Sample app stability, S23 sdp munging, dynascale, reconnect, cleanup for reconnect
*  Break
*  Week 6: Docs, docs & sample app. Dynascale, SDP parsing
*  Week 7: Reconnect, server side issues checklist, compose docs, fast join flow
*  Break
*  Week 8: Docs & Stability

### Essentials pre BETA launch

- Android volume is too low when calling from react
- Join is called in the preview screen, this is wrong
- Memory leak on Coil
- Open deeplinks from video call demo
- Testing: run the test app often (Thierry, Caleb & Jared)
- Android: Docs (Jaewoong)
- Frontend: a way to host the docs (Lennart)
- SFU: deploy process (Marcelo & Suchit)
- SFU: reconnect flow (Marcelo & Suchit)
- SFU: duplicate participant bug (related to the above)
- Both: Calling support (Tommaso)
- Ensure iOS, React, Flutter and Android are consistent (Tommaso)
- Dashboard: Push & Call Types (Eric & Sacha)
- Beta announcement email (Thierry) & Decide on limits & Pricing (Tommaso)

### Docs ~3 weeks left

- [ ] Video calling tutorial & Feedback cycle with team
- [ ] Livestream tutorial & Feedback cycle with team
- [ ] Audio room tutorial & Feedback cycle with team
- [X] Low level docs
- [ ] UI cookbook
- [ ] UI components

### TODO

- [ ] Video disconnects sometimes. (BufferQueue has not connected producer)
- [ ] HTTP requests fail in interceptor, which is wrong. (see https://console.firebase.google.com/project/stream-video-9b586/crashlytics/app/android:io.getstream.video.android.dogfooding.debug/issues?state=open&time=last-seven-days&tag=all&sort=eventCount)
- [ ] Go through all tests & TODOs and update with latest server changes.
- [ ] Fine tune the quality on S23 (so it's not at Q). Upload quality, make it easy to change
- [ ] Use the right date time class everywhere
- [ ] Add a timeout for waiting on the socket authentication events
- [ ] permission handling on the intro/preview screen is wrong. it starts out as video enabled
- [ ] Test the dogfooding app and iterate on things that don't work yet
- [ ] RED error
- [ ] Own permissions should be a sealed class
- [ ] use standard debug, verbose, info, warning and error debug levels on StreamVideoBuilder
- [ ] Improve our error classes. Right now there are 4 different formats. 
- [X] Full test coverage for retries + some more logging
- [X] Only run 1 connection retry flow at once. We should keep on retrying.
- [X] Participant sorting rules. See Call sortedParticipants
- [X] Pinning of participants. You pin/unpin and it sets pinnedAt and sorting takes it into account
- [X] Pause option on camera & microphone & speaker
- [X] When the phone is locked, we shouldn't be upload video
- [X] Background usage causes the camera to freeze and not recover when in foreground
- [X] When the app is in background (and PIP is disabled), what should the behaviour be?

### Server issues

#### Livestream

- [ ] Failure(value=NetworkError(message=GetOrCreateCall failed with error: "User 'thierry' with role 'user' is not allowed to perform action CreateCall in scope 'video:livestream'", serverErrorCode=17, statusCode=403, cause=java.lang.Throwable: https://getstream.io/chat/docs/api_errors_response))
- [ ] hlsPlaylistUrl vs ingress/egress
- [ ] Livestreaming where do i see when the call started

#### Audio room

- [ ] queryCalls should support Any? for the filter, otherwise you can't search for filters matching null
- [ ] socket connection hangs when joining as an anon user

#### Regular calls

- [ ] ICE restarts & join flow polish
- [ ] What about graceful SFU shutdown/ an event to make clients move SFU?
- [ ] Ringing call support
- [ ] getCall doesn't support member limits
- [ ] Events for creating a channel on chat. so you lazy load the chat when the first person opens it

### App & Compose

- [ ] Deeplink support for video call demo & dogfooding app
- [ ] Telemetry to firebase/crashlytics (with an opt out, or research alternatives)
- [ ] Preview components (CallLobbyContent + CallLobbyViewModel)
- [ ] Reactions don't show up
- [ ] Chat integration (we need an event from the server though)
- [ ] PIP
- [ ] When state._connection.value = RtcConnectionState.Reconnecting we should show a little transparent. “Reconnecting” UI element
- [ ] Ringing calls + CallService + foreground services/notifications (wait for push and updated endpoints from server)
- [X] Screensharing doesn't show up
- [X] Improve screensharing (zoomable + fallback)


### Reconnect

- [X] Retry on joining a call
- [X] Session.reconnect
- [X] Session.switchSfu
- [X] Connection state for UI indicators. state.connection
- [X] Monitoring that determines when to reconnect, or switchSfu


### High level issues

- [X] Join flow is too slow
- [X] Call id should probably be optional and default to a random UUID


### Available tasks up for grabs (little things)

- [X] Currently we use UserPreferencesManager. Jaewoong mentioned we should perhaps explore https://developer.android.com/topic/libraries/architecture/datastore



### Reconnect & Cross device testing ~2 weeks

- [X] Max resolution, simulcast and codec preferences should be easy to change
- [X] SFU needs to support sending some testing tracks for integration testing
- [X] We might need overwrites for the defaults at the device level
- [X] Track the FPS messages from webrtc camera capture



### RTC & Media TODO

- [X] Improve how we mangle SDP tokens. (edit the audio line and video line, don't swap lines)
- [X] Media manager tests & decide on microphone/speaker split
- [X] Leave & End flows
- [X] Enable & Test dynascale
- [X] Opus Red
- [X] Opus DTX
- [X] Clean up the media manager class Mic management
- [X] Move enabling/disabling/state and clean it up
- [X] Move setCameraEnabled & setMicrophoneEnabled
- [X] Clean up the media manager class Camera management
- [X] Ensure errors from sfu are bubbled up
- [X] Media manager class to enable easy testing of all audio/video stuff
- [X] Tests that verify the local track is working
- [X] Tests that verify we are sending our local track
- [X] setLocalTrack is not called



### State TODO

- [X] Permissions requests need an accept/reject flow
- [X] Call settings need to be used everywhere. There are still some hardcoded settings
- [X] Member state isn't implemented fully. Could be either a state or just a data class
- [X] Call state isn't setup fully on join
- [X] Member state isn't updated correctly or implemented

### LLC TODO

- [X] Test coverage
- [X] Clean up tests
- [X] Support for accepting/rejecting calls etc. HTTP endpoints seem cleaner
- [X] Directly use the events from openAPI to prevent things being out of sync
- [X] List of backend changes
- [X] Make call level client methods internal
- [X] Moderation API endpoints
  https://www.notion.so/stream-wiki/Moderation-Permissions-for-video-37a3376268654095b9aafaba12d4bb69
  https://www.notion.so/stream-wiki/Call-Permissions-832f914ad4c545cf8f048012900ad21d
- [X] Guest and anon user support

### Server wishlist

- [X] queryChannels doesn’t return members but CallUsers, this is wrong
- [X] Update call endpoints doesn’t expose team or startsAt. I get why we don’t expose team, not sure about startsAt
- [X] Get/Create etc don’t specify connection_id, this breaks the ability to watch
- [X] Not being able to edit settings on a call you created seems like the wrong default: “”User ‘thierry’ with role ‘user’ is not allowed to perform action UpdateCallSettings in scope ‘video:default’“, serverErrorCode=17, statusCode=-1, cause=java.lang.Throwable: ))”
- [X] Participant.online field is weird. Aren't you always online as a participant?
- [X] Participant count & Anonymous Participant Count (for livestreams you cant rely on the list). Returned by SFU. Updated by
- [X] ConnectionQualityInfo is a list, audio levels is a map. Lets standardize


### Server side - Call type settings

- [X] target resolution / max resolution (default 960) (if you're livestreaming you want to publish at 1080p or 4k typically)
- [X] if we should default to front or back camera
- [X] should video be default on or off. I join a call, should i be publishing video?
- [X] should audio be default on or off. I join a call, should i publishing audio?
- [X] am i allowed to publish (IE should i create the publisher peer connection) (it's in own capabilitiy. send video and send audio)
- [X] should volume be enabled by default (for livestreams and audio rooms its typically off by default)

### Review each file, fix TODOS and document

- [X] StreamVideoBuilder
- [X] ParticipantState
- [X] ClientState
- [X] MemberState
- [X] ConnectionModule
- [X] Call
- [X] StreamVideoImpl
- [X] CallState

### Testing

** Basics **
- [X] Truth
- [X] Mockk
- [X] Build vars (run locally)
- [X] Ability to run against local go codebase
- [X] Make call level client API methods internal
- [X] Build vars (valid token generation)

** Use cases **

- [X] Audio rooms
- [X] Livestreaming
- [X] Calling

** Protocol **

- [X] Responding to events
- [X] Join flow
- [X] Active Session

### Refactoring

- [X] Client builder refactoring. see https://www.notion.so/stream-wiki/Android-Changes-Discussion-10c5a9f303134eb786bdebcea55cf92a
- [X] Stop hiding backend errors (in progress)
- [X] Merge SFU Client and CallClientImpl into 1 concept
- [X] Call refactoring
- [X] Ensure we always use DispatcherProvider.IO
- [X] Support query calls
- [X] Cleanup The Network connection module
- [X] Cleanup event handling
- [X] Update to the latest events from the protocol
- [X] Easily see if the coordinator is connected
- [X] VideoSocket Impl shouldn't hide errors
- [X] Easily see if the SFU is connected
- [X] Merge the two concepts of Call. Call2 and model/Call.kt
- [X] Use the new state in the view model
- [X] User a map for sessionId -> ParticipantState. Fixes several bugs with userId vs sessionId
- [X] Verify all events are handled
- [X] Media manager class to abstract all the local audio/video stuff. Also makes it easy to test the codebase if you can swap out the media & webrtc stuff.
- [X] Clean up the active SFU session]
- [X] Socket implementation should be simple
- [X] Token provider

### Features

- [X] Event subscriptions (listening)
- [X] Event subscriptions (sending)
- [X] Permissions: https://www.notion.so/stream-wiki/Call-Permissions-832f914ad4c545cf8f048012900ad21d
- [X] Faster latency measurements (run in parallel)
- [X] Sending reactions
- [X] Support for listening to events at the call level
- [X] Opus RED
- [X] Opus DTX
- [X] Muting other users/ Moderation
- [X] Token provider

### Out of scope for initial release

- [ ] Screensharing (from mobile, display should work)
- [ ] Talking while muted notification
- [ ] If you answer a phone call while you're on this call, your audio and video should be muted automatically.]
- [ ] State for: Speaking while muted, Network issue (your own connection)
- [ ] Audio filter example
- [ ] Video filter example
- [ ] Build vars to generate tokens for testing
- [ ] What about codec switching?
- [ ] Events for updating users
