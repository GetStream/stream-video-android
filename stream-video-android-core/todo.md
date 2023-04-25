
## Essentials for a good Beta

Important
* Docs & developer experience
* Sample /demo app
* Excellent stability

Not as important
* Supporting all features

## Timeline

### Week 1: Refactor LLC & State. Setup testing
### Week 2: LLC & State Stability. Compose testing & previews
### Week 3: Sample app, update compose to LLC & State changes. New events, socket & token provider. Call UI. Guest users & moderation endpoints
### Week 4: LLC & state test coverage + Demo & Dogfooding apps + RTC & Media fixes
### Week 5: Sample app stability

- Dynascale... visibility isn't handled well in CallSingleVideoRendered & VideoRenderer
- Publishing: The video doesn't render in react, I suspect simulcast issue
- Receiving

### High level issues

- [ ] Join flow is too slow
- [ ] Reconnection support needs work
- [ ] Docs need a lot of work
- [ ] SDK doesn't handle all edge cases yet



### App & Compose

- Camera mute button doesn't work
- Token expiration isn't handled well in dogfooding app
- Crashlytics for sample and dogfooding apps
- PIP
- Ringing calls (wait for push and updated endpoints from server)
- Key "thierry@getstream.io" was already used. If you are using LazyColumn/Row please make sure you provide a unique key for each item
- Make buttons easier to customize. right now there is a list of items. something like this would be nicer:
  <CallControls><VolumeControl /><CameraMuteControl> </CallControls>etc
- Fix the buttons, right now they don't all work

### RTC & Media TODO

- [ ] Improve how we mangle SDP tokens. (edit the audio line and video line, don't swap lines)
- [ ] Review how UI changes & pagination are connected to the video tracks. See call initRenderer and updateParticipantsSubscriptions
- [ ] Implement dynascale
- [ ] Tests for the media manager
- [ ] Screensharing
- [ ] Error classes for Media/Camera/Mic & Joining a call. That wrap the many things that can go wrong.
- [ ] Leave & End flows
- [ ] Talking while muted notification
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

### Other

- Review the 200 todos
- Unit test on Android that customer success can run to test the SDK/ edit things for a customer

### LLC TODO

- [ ] Error.NetworkError vs ErrorResponse. Having 2 classes is confusing. Error format is slightly differences in 4 places. 
- [ ] Remove unused code
- [ ] Move SFU event to swap between SFUs and handle failure
- [ ] Reconnect after SFU breaks (https://www.notion.so/Reconnection-Failure-handling-f6991fd2e5584380bb2d2cb5e8ac5303)
- [ ] Audio filter example
- [ ] Video filter example
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

### State TODO

- [X] Call settings need to be used everywhere. There are still some hardcoded settings
- [X] Member state isn't implemented fully. Could be either a state or just a data class
- [X] Call state isn't setup fully on join
- [X] Member state isn't updated correctly or implemented

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
- [ ] Build vars (valid token generation)

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

### Documentation (code level)

- [X] Client docs
- [X] Call docs

### Documentation Markdown

- [ ] 3 tutorials
- [ ] Custom compose example
- [ ] Authentication example
- [ ] Docs on client setup


 
### Disconnect suggestion

- supervisorJob at the call and client level
- call.leave() method and call.end() method (which calls leave and then ends for everyone)
- leave method shuts down the supervisorJob, remove local objects, cleans up any media tracks
- client.disconnect() (leaves all calls, cleans up state)
- call is connected to the call view model lifecycle methods. client to the application lifecycle

### Muting/unmuting & permissions

- There is significant complexity around muting

### Server wishlist

[X] queryChannels doesn’t return members but CallUsers, this is wrong
[X] Update call endpoints doesn’t expose team or startsAt. I get why we don’t expose team, not sure about startsAt
[X] Get/Create etc don’t specify connection_id, this breaks the ability to watch
[X] Not being able to edit settings on a call you created seems like the wrong default: “”User ‘thierry’ with role ‘user’ is not allowed to perform action UpdateCallSettings in scope ‘video:default’“, serverErrorCode=17, statusCode=-1, cause=java.lang.Throwable: ))”
[X] Participant.online field is weird. Aren't you always online as a participant?
[ ] Events for updating users 
[ ] Participant count (for livestreams you cant rely on the list)
[ ] ConnectionQualityInfo is a list, audio levels is a map. Lets standardize
[ ] Accept/reject call endpoints
[ ] What about codec switching?
[ ] What about graceful SFU shutdown/ an event to make clients move SFU?
[ ] Events for creating a channel on chat. so you lazy load the chat when the first person opens it
[ ] List of error codes via openapi
[ ] getCall doesn't support member limits
[ ] CallMemberUpdatedPermissionEvent. Weird that call and members are included
[ ] message=GetOrCreateCall failed with error: "The following users are involved in call create operation, but don't exist: [jaewoong]. Please create the user objects before setting up the call.
[ ] review QueryMembersRequest
[ ] target resolution / max resolution (default 960)
[ ] if we should default to front or back camera
[ ] should video be default on or off?
[ ] should audio be default on or off?
[ ] am i allowed to publish (IE should i create the publisher peer connection)

### Available tasks up for grabs

- use standard debug, verbose, info, warning and error debug levels on StreamVideoBuilder
- Participant sorting rules. See Call sortedParticipants
- Pinning of participants. You pin/unpin and it sets pinnedAt and sorting takes it into account
- Currently we use UserPreferencesManager. Jaewoong mentioned we should perhaps explore https://developer.android.com/topic/libraries/architecture/datastore
- Measure latency isn't 100% ok. You can't set a timeout using withTimeout and collect the measurements that we have. This relates to threading vs coroutines and withTimeout not working
- Disconnect/ garbage collect flow needs a full round of review
