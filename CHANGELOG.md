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