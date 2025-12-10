# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Stream Video SDK for Android - a Kotlin-based SDK providing video calling, audio rooms, and livestreaming capabilities. The SDK integrates WebRTC with Stream's SFU backend, offering both low-level APIs and pre-built Compose/XML UI components.

## Essential Build Commands

```bash
# Build the SDK
./gradlew assemble

# Run all checks (tests, lint, formatting)
./gradlew check

# Run unit tests only
./gradlew test

# Run tests for specific module
./gradlew :stream-video-android-core:test

# Run instrumented tests (requires emulator/device)
./gradlew connectedCheck

# Apply code formatting (required before commits)
./gradlew spotlessApply

# Install demo app
./gradlew :demo-app:installDebug

# Regenerate OpenAPI client code (requires env setup)
./generate-openapi.sh
```

## Module Architecture

**Core SDK modules:**
- `stream-video-android-core` - Core engine: WebRTC, signaling, state management, call orchestration
- `stream-video-android-ui-core` - Shared UI resources (colors, strings, drawables) for compose/xml modules
- `stream-video-android-ui-compose` - Jetpack Compose UI components (primary UI layer)
- `stream-video-android-ui-xml` - XML view components (legacy support)
- `stream-video-android-filters-video` - Video filters and effects pipeline
- `stream-video-android-previewdata` - Test fixtures and preview data for Compose
- `stream-video-android-bom` - Bill of materials for dependency alignment

**Support modules:**
- `demo-app/` - Demonstration application
- `tutorials/` - Tutorial implementations (video, audio, livestream, ringing)
- `benchmark/` - Performance benchmarking
- `metrics/` - SDK metrics collection

## Key Architectural Concepts

### State Management
- All events (coordinator + SFU) route through `StreamVideoImpl.subscribe()`
- Events update `client.state`, `call.state`, and participant state
- Use `client.fireEvent()` for local events and testing
- State exposed via Kotlin StateFlow for reactive UI updates

### WebRTC Layer
- `RtcSession` maintains tracks and WebRTC peer connections
- Two peer connections per call: publisher (outbound) and subscriber (inbound)
- Session lifecycle: create sessionId → create peer connections → capture media → join → add tracks → handle negotiation
- Camera/device changes flow through listener in `ActiveSFUSession` → updates tracks

### API Communication
- `StreamVideoImpl` handles coordinator API calls (via 4 Retrofit APIs)
- `ConnectionModule` handles SFU edge API calls
- `PersistentSocket` subclasses: `CoordinatorSocket` and `SfuSocket` maintain WebSocket connections
- Generated OpenAPI client code in `org.openapitools.client` package

### Dynascale (Adaptive Quality)
- **Client → SFU**: `VideoRenderer` marks visibility + resolution → `RtcSession.updateTrackDimensions` → `updateParticipantSubscriptions` (debounced 100ms)
- **SFU → Client**: `ChangePublishQualityEvent` triggers `RtcSession.updatePublishQuality` to enable/disable simulcast layers
- Resolution data stored in `RtcSession.trackDimensions` map

### Participant State
- Participants identified by unique session ID
- Each participant has a `trackPrefix` for media stream identification
- Media streams have `streamID` format: `trackPrefix:trackType`
- Members are unique per user, not per session

## Testing Guidelines

### Test Base Classes
- `TestBase` - Fast unit tests with mocked dependencies (preferred for speed)
- `IntegrationTestBase` - End-to-end tests with real API calls (use for integration scenarios)
- Both provide utilities for coroutines, event assertions, and test data

### Test Structure
```kotlin
@RunWith(RobolectricTestRunner::class)
class MyTest : IntegrationTestBase() {
    @Test
    fun `create a call and verify the event is fired`() = runTest {
        val call = client.call("default", randomUUID())
        val result = call.create()
        assertSuccess(result)
        val event = waitForNextEvent<CallCreatedEvent>()
    }
}
```

### Testing Best Practices
- Use Truth for assertions, MockK for mocking
- Use backtick test names for readability
- Always call `client.cleanup()` in tests to prevent health monitor threads from hanging
- Never use `Dispatchers.Main/IO` directly - use `DispatcherProvider.Main/IO` for testability
- Leverage `stream-video-android-previewdata` for fixtures instead of ad-hoc builders

## Development Configuration

### Environment Setup
1. Copy `env.properties.sample` to `.env.properties` (gitignored)
2. Build config reads `.env.properties` and exposes as `BuildConfig` properties
3. Never commit credentials or API keys

### Development Modes
- `StreamVideoImpl.developmentMode` controls error handling:
  - Development: fail fast and loud for debugging
  - Production: gracefully handle non-critical errors

## Important Implementation Patterns

### Compose UI Philosophy
Expose underlying components rather than hiding them in monolithic composables:

```kotlin
// Good - shows customization points
CallContent(
  videoRenderer = { ParticipantGrid(card = { ParticipantCard() }) },
  callControls = {
    CallControls {
      ChatButton()
      FlipVideoButton()
      MuteAudioButton()
    }
  }
)

// Bad - hides customization
CallComposable()
```

### Coroutines & Concurrency
- Keep RTC critical paths off main thread
- Use structured concurrency with proper scopes
- Prefer coroutines over callbacks
- Health monitors run continuously (while loops) - must cleanup properly

### Binary Compatibility
- SDK modules shipped to Maven Central must maintain binary compatibility
- Prefer additive API changes over breaking changes
- Coordinate with release team before modifying public APIs
- Modules use explicit API mode (`-Xexplicit-api=strict`) except core and metrics

## Code Style Specifics

### Kotlin Compiler Options
- Core modules use `-Xjvm-default=enable` and opt-in to `@InternalStreamVideoApi`
- Most SDK modules enforce explicit API visibility
- Generated code patterns excluded from Spotless

### Logging
- Use `LoggingLevel` configuration for controlling verbosity
- RTC logs can be verbose - search "video quality" or "input_fps" for diagnostics
- Never log sensitive data (JWTs, ICE tokens, call IDs in production)

## Common Development Tasks

### Running Single Test
```bash
./gradlew :stream-video-android-core:test --tests "io.getstream.video.android.core.MyTest"
```

### Debugging Video Quality
1. Search logs for "video quality" to see selected/max resolution
2. Search "input_fps" for WebRTC quality limitations
3. Check `MediaManager.selectDesiredResolution` for resolution selection logic
4. Use `DebugInfo` composable for real-time diagnostics
5. Review peer connection stats for detailed metrics

### OpenAPI Code Generation
- Regenerate when protocol changes: `./generate-openapi.sh`
- Generated files output to `~/workspace/generated/`
- Android code in: `~/workspace/stream-video-android/`
- Protocol docs: https://getstream.github.io/protocol/
- Advanced: Set `LOCAL_ENV=1` for local protocol repo, `OPENAPI_ROOT_USE_COORDINATOR_REPO=1` for coordinator repo

## Release Process

Version configuration in `buildSrc/src/main/kotlin/io/getstream/video/android/Configuration.kt`:
- Snapshots auto-build from `develop` branch
- Production releases via GitHub release tags on `main` branch
- Coordinate with release owner before version changes

## Critical Paths to Understand

### Call Join Flow
1. Create sessionId locally (random UUID)
2. Create publisher + subscriber peer connections
3. Capture audio/video (may already be running from preview)
4. Execute join request to coordinator
5. Add audio/video tracks (triggers onNegotiationNeeded)
6. onNegotiationNeeded calls SetPublisherRequest
7. JoinCallResponseEvent returns call state

### Ringing Flow
- Push notification or coordinator WebSocket triggers `CallCreatedEvent` with `ring=true`
- UI shows incoming call interface
- Accept/reject calls corresponding API endpoints
- Call members have `accepted_at` and `rejected_at` fields

### Screen Sharing
- Uses `StreamScreenShareService` foreground service
- Available for both mobile and remote participants
- Handled as special track type in participant state

## Notes for AI Assistants

- This is a production SDK with strict backwards compatibility requirements
- Always run tests and formatting before suggesting changes
- Prefer reading existing patterns over inventing new abstractions
- When modifying WebRTC or state management, consider thread safety carefully
- UI changes should support both Compose and XML layers where applicable
- See `AGENTS.md` for additional agent-specific guidance
