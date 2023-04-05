
## This week: LLC Stability. Basics, not refactoring the sockets yet

- Cleanup the test suite and make it faster
- Openapi/Event integration. Events being out of sync is a large issue
- 
- Clean up the active SFU session

## Phase 2, find a way to make Call/SFUSession testable

This i'm still somewhat unsure about

- MediaManager that has a dummy version of devices for test
- Webrtc dummy version. Shares participants with dummy video tracks

## Phase 3, Test using the UI




### Review each file, fix TODOS and document

- [X] StreamVideoBuilder
- [ ] StreamVideoImpl
- [ ] ConnectionModule
- [ ] Call
- [ ] CallState
- [ ] ParticipantState

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
- [ ] Reconnect flow (https://www.notion.so/Reconnection-Failure-handling-f6991fd2e5584380bb2d2cb5e8ac5303)
- [ ] Join flow performance

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
- [ ] Verify all events are handled
- [ ] Local participants, maybe data class, something to get stateflow to play nice
- [ ] Use the new state in the view model
- [ ] Media manager class to abstract all the local audio/video stuff. Also makes it easy to test the codebase if you can swap out the media & webrtc stuff.

### Features

- [X] Event subscriptions (listening)
- [X] Event subscriptions (sending)
- [X] Permissions: https://www.notion.so/stream-wiki/Call-Permissions-832f914ad4c545cf8f048012900ad21d
- [X] Faster latency measurements (run in parallel)
- [X] Sending reactions
- [X] Support for listening to events at the call level
- [ ] Muting other users
- [ ] Participant sorting rules
- [ ] Audio filter example
- [ ] Video filter example

### Documentation (code level)

- [ ] Client docs
- [ ] Call docs

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

### Available tasks up for grabs

- Currently we use UserPreferencesManager. Jaewoong mentioned we should perhaps explore https://developer.android.com/topic/libraries/architecture/datastore
- Measure latency isn't 100% ok. You can't set a timeout using withTimeout and collect the measurements that we have. This relates to threading vs coroutines and withTimeout not working
- Logging needs to be reworked to correctly pass down from client to retrofit, also logs should work in the test suite
- Disconnect/ garbage collect flow needs a full round of review