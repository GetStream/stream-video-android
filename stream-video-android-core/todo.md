
## Timeline

### Week 1: Refactor LLC & State. Setup testing
### Week 2: LLC & State Stability. Compose testing & previews
### Week 3: Sample app, update compose to LLC & State changes

- Audio/video review
- Tommaso to fix the event openapi thing, and we merge it
- Jaewoong and Thierry to review the codebase. Monday evening
- Make the UI work with the new LLC & State
- Step 1: Render local video using the new API and compose
- Step 2: Upload local video to the SFU
- Step 3: Join a call and render participants

Other

- Cleanup the test suite

### Review each file, fix TODOS and document

- [X] StreamVideoBuilder
- [X] ParticipantState
- [X] ClientState
- [ ] CallState
- [ ] ConnectionModule
- [ ] StreamVideoImpl
- [ ] Call

### TODOs

- Review the 200 todos

### Testing

** Basics **
- [X] Truth
- [X] Mockk
- [X] Build vars (run locally)
- [X] Ability to run against local go codebase
- [ ] Build vars (valid token generation)
- [ ] Make call level client API methods internal

** Use cases **

- [X] Audio rooms
- [X] Livestreaming
- [X] Calling

** Protocol **

- [X] Responding to events
- [X] Join flow
- [X] Active Session

### Websockets

- Review WS requirements for SFU socket connection
- Test coverage for WS connection
- Have 1 class for Coordinator and SFU persistent WS connection

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

### Documentation (code level)

- [ ] Client docs
- [ ] Call docs

### Documentation Markdown

- [ ] 3 tutorials
- [ ] Custom compose example
- [ ] Authentication example
- [ ] Docs on client setup


### LLC TODO

- [ ] Directly use the events from openAPI to prevent things being out of sync
- [ ] List of backend changes
- [ ] Join flow performance
- [ ] Reconnect flow (https://www.notion.so/Reconnection-Failure-handling-f6991fd2e5584380bb2d2cb5e8ac5303)
- [ ] Audio filter example
- [ ] Video filter example

### State TODO

- [ ] Call settings need to be used everywhere. There are still some hardcoded settings

### RTC TODO

- [ ] Media manager class to enable easy testing of all audio/video stuff
- 

### Disconnect suggestion

- supervisorJob at the call and client level
- call.leave() method and call.end() method (which calls leave and then ends for everyone)
- leave method shuts down the supervisorJob, remove local objects, cleans up any media tracks
- client.disconnect() (leaves all calls, cleans up state)
- call is connected to the call view model lifecycle methods. client to the application lifecycle

### Muting/unmuting & permissions

- There is significant complexity around muting

### Server wishlist

- Events as a sealed class
- Tokens for calls
- RTMP. How will the API interactions work?
- HLS
- Events for updating users
- Participant count (for livestreams you cant rely on the list)
- Participant.online field is weird. Aren't you always online as a participant?
- ConnectionQualityInfo is a list, audio levels is a map. Lets standardize
- Push setup
- Listening to events for a list of calls

### Available tasks up for grabs

- Participant sorting rules. See Call sortedParticipants
- Pinning of participants. You pin/unpin and it sets pinnedAt and sorting takes it into account
- Currently we use UserPreferencesManager. Jaewoong mentioned we should perhaps explore https://developer.android.com/topic/libraries/architecture/datastore
- Hash/copy/equality methods for participantstate (otherwise the participants stateflow will have bugs)
- Measure latency isn't 100% ok. You can't set a timeout using withTimeout and collect the measurements that we have. This relates to threading vs coroutines and withTimeout not working
- Logging setting needs to be passed to retrofit
- Token refresh flow
- Disconnect/ garbage collect flow needs a full round of review
- MediaManager needs a full review and cleanup
- Socket connection system needs a full review, cleanup and test coverage
- Ringing support isn't implemented yet
