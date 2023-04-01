
## Launch prep for the low level client

- Check how we see if a socket is connected (test for coordinator and SFU)
- Verify logging works well

### Testing

** Basics **
- [X] Truth
- [X] Mockk
- [X] Build vars (run locally)
- [X] Ability to run against local go codebase
- [ ] Build vars (valid token generation)
- [ ] Make client API methods internal

** Use cases **

- [X] Audio rooms
- [X] Livestreaming
- [X] Calling

** Protocol **

- [X] Responding to events
- [X] Join flow
- [ ] Reconnect flow (https://www.notion.so/Reconnection-Failure-handling-f6991fd2e5584380bb2d2cb5e8ac5303)
- [ ] Active Session

### Refactoring

- [X] Client builder refactoring. see https://www.notion.so/stream-wiki/Android-Changes-Discussion-10c5a9f303134eb786bdebcea55cf92a
- [X] Stop hiding backend errors (in progress)
- [X] Merge SFU Client and CallClientImpl into 1 concept
- [X] Call refactoring
- [X] Ensure we always use DispatcherProvider.IO
- [X] Support query calls
- [X] Cleanup The Network connection module
- [ ] Easily see if the coordinator is connected
- [ ] Easily see if the SFU is connected
- [X] Cleanup event handling
- [ ] Use the new state in the view model
- [ ] Update to the latest events from the protocol
- [ ] Perhaps a media manager class to abstract all the local audio/video stuff

### Features

- [X] Event subscriptions (listening)
- [X] Event subscriptions (sending)
- [X] Permissions: https://www.notion.so/stream-wiki/Call-Permissions-832f914ad4c545cf8f048012900ad21d
- [X] Faster latency measurements (run in parallel)
- [ ] Sending reactions
- [ ] Verify all events are handled
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

### Review each file, fix TODOS and document

- [ ] StreamVideoBuilder
- [ ] StreamVideoImpl
- [ ] ConnectionModule
- [ ] Call2
- [ ] CallState
- [ ] ParticipantState

### Available tasks up for grabs

- Currently we use UserPreferencesManager. Jaewoong mentioned we should perhaps explore https://developer.android.com/topic/libraries/architecture/datastore
- Measure latency isn't 100% ok. You can't set a timeout using withTimeout and collect the measurements that we have. This relates to threading vs coroutines and withTimeout not working