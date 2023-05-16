
## Timeline

*  Week 1: Refactor LLC & State. Setup testing
*  Week 2: LLC & State Stability. Compose testing & previews
*  Week 3: Sample app, update compose to LLC & State changes. New events, socket & token provider. Call UI. Guest users & moderation endpoints
*  Week 4: LLC & state test coverage + Demo & Dogfooding apps + RTC & Media fixes
*  Week 5: Sample app stability, S23 sdp munging, dynascale, reconnect, cleanup for reconnect
*  Break
*  Week 6: Docs, docs & sample app. Dynascale, SDP parsing
*  Week 7: Reconnect, server side issues checklist, compose docs, fast join flow

### Reconnect

- [X] Retry on joining a call
- [X] Session.reconnect
- [X] Session.switchSfu
- [X] Connection state for UI indicators. state.connection
- [X] Monitoring that determines when to reconnect, or switchSfu
- [ ] Only run 1 retry flow at once. We should keep on retrying. See the socket health monitor
- [ ] Full test coverage

### High level issues

- [X] Join flow is too slow
- [X] Call id should probably be optional and default to a random UUID
- [ ] Chat integration needs a good review to see what we can simplify

### Available tasks up for grabs (little things)

- [ ] use standard debug, verbose, info, warning and error debug levels on StreamVideoBuilder
- [ ] Participant sorting rules. See Call sortedParticipants
- [ ] Pinning of participants. You pin/unpin and it sets pinnedAt and sorting takes it into account
- [X] Currently we use UserPreferencesManager. Jaewoong mentioned we should perhaps explore https://developer.android.com/topic/libraries/architecture/datastore

### Docs ~4 weeks left

- [ ] Video calling tutorial & Feedback cycle with team
- [ ] Livestream tutorial & Feedback cycle with team
- [ ] Audio room tutorial & Feedback cycle with team
- [X] Low level docs
- [ ] UI cookbook
- [ ] UI components

### Reconnect & Cross device testing ~2 weeks

- [ ] Max resolution, simulcast and codec preferences should be easy to change
- [ ] SFU needs to support sending some testing tracks for integration testing
- [ ] We might need overwrites for the defaults at the device level
- [ ] Track the FPS messages from webrtc camera capture

### App & Compose

- [ ] Reactions don't show up
- [ ] Chat integration (we need an event from the server though)
- [ ] PIP
- [ ] When state._connection.value = RtcConnectionState.Reconnecting we should show a little transparent. “Reconnecting” UI element
- [ ] Ringing calls (wait for push and updated endpoints from server)
- [X] Screensharing doesn't show up


### RTC & Media TODO

- [ ] Error classes for Media/Camera/Mic & Joining a call. That wrap the many things that can go wrong.
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

- [ ] Error.NetworkError vs ErrorResponse. Having 2 classes is confusing. Error format is slightly differences in 4 places. 
- [ ] Remove unused code
- [ ] Move SFU event to swap between SFUs and handle failure
- [ ] Reconnect after SFU breaks (https://www.notion.so/Reconnection-Failure-handling-f6991fd2e5584380bb2d2cb5e8ac5303)
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

### Other & Process

- [ ] Review the 104 todos
- [ ] Coverage reporting
- [ ] Better Mocks for testing RtcSession
- [ ] Build vars to generate tokens for testing

### Server wishlist

- [X] queryChannels doesn’t return members but CallUsers, this is wrong
- [X] Update call endpoints doesn’t expose team or startsAt. I get why we don’t expose team, not sure about startsAt
- [X] Get/Create etc don’t specify connection_id, this breaks the ability to watch
- [X] Not being able to edit settings on a call you created seems like the wrong default: “”User ‘thierry’ with role ‘user’ is not allowed to perform action UpdateCallSettings in scope ‘video:default’“, serverErrorCode=17, statusCode=-1, cause=java.lang.Throwable: ))”
- [X] Participant.online field is weird. Aren't you always online as a participant?
- [ ] Events for updating users
- [ ] Participant count (for livestreams you cant rely on the list)
- [ ] ConnectionQualityInfo is a list, audio levels is a map. Lets standardize
- [ ] Accept/reject call endpoints
- [ ] What about codec switching?
- [ ] What about graceful SFU shutdown/ an event to make clients move SFU?
- [ ] Events for creating a channel on chat. so you lazy load the chat when the first person opens it
- [ ] List of error codes via openapi
- [ ] getCall doesn't support member limits
- [ ] CallMemberUpdatedPermissionEvent. Weird that call and members are included
- [ ] message=GetOrCreateCall failed with error: "The following users are involved in call create operation, but don't exist: [jaewoong]. Please create the user objects before setting up the call.
- [ ] review QueryMembersRequest
- [ ] health check http request on the SFU (no auth, nothing that can give errors, just health) (for the recovery flow). not sure about this one, tbd

### Server side - Call type settings

- [ ] target resolution / max resolution (default 960) (if you're livestreaming you want to publish at 1080p or 4k typically)
- [ ] if we should default to front or back camera
- [ ] should video be default on or off. I join a call, should i be publishing video?
- [ ] should audio be default on or off. I join a call, should i publishing audio?
- [ ] am i allowed to publish (IE should i create the publisher peer connection)
- [ ] should volume be enabled by default (for livestreams and audio rooms its typically off by default)

### Out of scope for initial release

- [ ] Screensharing (from mobile, display should work)
- [ ] Talking while muted notification
- [ ] If you answer a phone call while you're on this call, your audio and video should be muted automatically.]
- [ ] State for: Speaking while muted, Network issue (your own connection)
- [ ] Audio filter example
- [ ] Video filter example

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

