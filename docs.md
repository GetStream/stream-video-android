

### Client setup

The basic way to create a client is here:

```
val client = StreamVideoBuilder(
    context = context,
    apiKey = apiKey,
    geo = GEO.GlobalEdgeNetwork,
    user,
    token,
).build()
```

Other options include

* tokenProvider (for expired tokens)
* audio/video filters

You can leave out the token, if you want to use call level tokens (for livestreams this will be common)

### Create and join a call

```kotlin
val call = client.call("default", "123", optionalToken)
val result = call.create()
val result2 = call.join()
```

### Listening to events

You can listen to events for both the coordinator & SFU in 1 place, which is nice and easy

```kotlin
client.subscribe { event -> {
  println(event)  
}}
```

subscribeFor<ConnectedEvent> also works to sub for a single event

### State

```kotlin
call.state.participants
```

Client state, call state and participant state are updated automatically from the events

### Audio/video

```
call.camera.devices
val cameraId = call.camera.devices.value.first()
call.camera.flip()
call.camera.disable()
call.camera.enable()
call.camera.status
call.camera.select(cameraId)
```

### Viewmodel

The call view model is constructed from the client, call and a permission manager

```
// pseudo code
callViewModel(client, call, permisionManager)
```