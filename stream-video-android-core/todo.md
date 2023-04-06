
## Timeline

### Week 1: Refactor LLC & State. Setup testing
### Week 2: LLC Stability

- Token refresh flow
- Sorted Map for participants
- Openapi/Event integration. Events being out of sync is a large issue
- Cleanup the test suite
- Clean up the active SFU session

## Phase 2, find a way to make Call/SFUSession testable

- MediaManager that has a dummy version of devices for test

## Phase 3, Test using the UI




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
- [ ] Verify all events are handled
- [ ] Media manager class to abstract all the local audio/video stuff. Also makes it easy to test the codebase if you can swap out the media & webrtc stuff.

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


### Client TODO

- [ ] Join flow performance
- [ ] Reconnect flow (https://www.notion.so/Reconnection-Failure-handling-f6991fd2e5584380bb2d2cb5e8ac5303)
- [ ] Audio filter example
- [ ] Video filter example

### State TODO

- [ ] Call settings need to be used everywhere

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
- RTMP
- HLS
- Events for updating users
- Participant count (for livestreams you cant rely on the list)
- Participant.online field is weird. Aren't you always online as a participant?
- ConnectionQualityInfo is a list, audio levels is a map. Lets standardize

### Available tasks up for grabs

- Participant sorting rules. See Call sortedParticipants
- Pinning of participants
- Currently we use UserPreferencesManager. Jaewoong mentioned we should perhaps explore https://developer.android.com/topic/libraries/architecture/datastore
- Measure latency isn't 100% ok. You can't set a timeout using withTimeout and collect the measurements that we have. This relates to threading vs coroutines and withTimeout not working
- Logging setting needs to be passed to retrofit
- Disconnect/ garbage collect flow needs a full round of review
- Hash/copy/equality methods for participantstate (otherwise the participants stateflow will have bugs)
