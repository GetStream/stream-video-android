# UNRELEASED CHANGELOG
## Common changes for all artifacts
### 🐞 Fixed

### ⬆️ Improved

### ✅ Added

### ⚠️ Changed

### ❌ Removed

## stream-video-android
### 🐞 Fixed
Fix `NullPointerException` logged from `SortedParticipantsState.init` on every `Call` construction. The internal sort coroutine read `Call.events` before the field was initialized, racing against the rest of `Call`'s constructor on production dispatchers. The error was captured by the call scope's exception handler — calls continued to work — but the call-event-driven resort path was effectively dead.

### ⬆️ Improved

### ✅ Added

### ⚠️ Changed

### ❌ Removed

## stream-video-android-ui-compose
### 🐞 Fixed
Fix R8/minified release builds failing with `Missing class io.getstream.video.android.mock.StreamPreviewDataUtils` (and related preview-data classes). All `@Preview` composables were moved out of the published artifact into a `debug`-only source set, so the `stream-video-android-previewdata` utilities are no longer referenced from release code. Consumers no longer need to add the preview module or extra ProGuard rules. Fixes [#1448](https://github.com/GetStream/stream-video-android/issues/1448).
Declare `kotlinx-datetime` as an explicit dependency so the livestream countdown UI no longer relies on a transitive dependency, fixing `Missing class kotlinx.datetime.*` R8 errors.
On CallContent make sure to pass onBackPressed to CallAppBar in order to make the back button work.

### ⬆️ Improved

### ✅ Added
Add `participantLabelContent` slot to `CallLobby` so the lobby participant label can be hidden (`{}`) or fully customized. Brings the Compose API in line with React's `VideoPreview` slot overrides.

### ⚠️ Changed
Deprecate the `CallLobby` overload that takes `labelPosition: Alignment`. Migrate to the new `participantLabelContent` slot; pass `{}` to hide the label or supply a composable to customize content and positioning.

### ❌ Removed

## stream-video-android-xml
### 🐞 Fixed

### ⬆️ Improved

### ✅ Added

### ⚠️ Changed

### ❌ Removed

## stream-video-android-pushprovider-firebase
### 🐞 Fixed

### ⬆️ Improved

### ✅ Added

### ⚠️ Changed

### ❌ Removed