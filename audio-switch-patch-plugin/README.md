# AudioSwitch patch plugin

This included build produces `io.getstream.audioswitch-patch`. The plugin uses
AGP's `ScopedArtifact.CLASSES` API to replace the complete Twilio AudioSwitch
1.2.0 class family in final Android applications.

The transform deliberately fails when the original class inventory or the
`AudioSwitch.class` SHA-256 does not match the pinned 1.2.0 artifact. Apply the
plugin only to an Android application:

```kotlin
plugins {
    id("io.getstream.audioswitch-patch")
}
```

The patched source is compiled from the pinned upstream 1.2.0 source archive.
Only `AudioSwitch.kt` is replaced. Generated patch bytecode is embedded in the
Gradle plugin before publication.
