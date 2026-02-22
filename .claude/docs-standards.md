# Documentation Standards for Stream Video Android SDK

Comprehensive standards for creating and updating documentation. Follow these rules to ensure consistent, high-quality documentation.

---

## Configuration

```yaml
docs_repo: https://github.com/GetStream/docs-content.git
docs_path: video/android/
sidebar_path: _sidebars/[video][android].json
sdk_repo: /Users/aapostol/projects/stream-video-android
sdk_branch: develop
clone_to: /tmp/docs-content-sync/
```

---

## 1. Page Structure

Every documentation page follows this structure:

```markdown
[Introduction paragraph - what this page covers, 1-3 sentences]

## Section Heading

[Content]

### Subsection (if needed)

[Content with code examples]

## Next Section

[Continue pattern]
```

**Rules:**
- **No frontmatter** - pages start directly with content (no `---` YAML blocks). Titles come from sidebar JSON, not from the markdown file. Frontmatter will be rendered as visible text.
- Introduction paragraph before first heading (no `## Introduction` heading needed)
- Use `##` for main sections, `###` for subsections
- Keep hierarchy shallow (rarely go beyond `###`)
- Each page should cover ONE topic thoroughly

**Section ordering pattern:**
1. Introduction/Overview (what it is)
2. Prerequisites (if any)
3. Basic usage (simplest case)
4. Configuration/Options (parameters, customization)
5. Advanced usage (complex scenarios)
6. Related links (if relevant)

---

## 2. Code Blocks

### Language Tags

Always specify the language:

```kotlin
// Kotlin code (most common)
```

```groovy
// Gradle Groovy DSL
```

```kotlin
// Gradle Kotlin DSL (build.gradle.kts)
```

```xml
// XML (layouts, strings, manifest)
```

```bash
# Shell commands
```

### Full Samples vs Fragments

**Full sample** - Include imports, show complete context:
```kotlin
import io.getstream.video.android.core.StreamVideoBuilder
import io.getstream.video.android.core.GEO
import io.getstream.video.android.model.User

val client: StreamVideo = StreamVideoBuilder(
    context = applicationContext,
    apiKey = "your-api-key",
    geo = GEO.GlobalEdgeNetwork,
    user = User(id = "user-id"),
    token = "user-token",
).build()
```

**Fragment** - Omit imports, focus on the specific API:
```kotlin
// Enable background blur
call.videoFilter = BlurredBackgroundVideoFilter()
```

**When to use which:**
| Scenario | Use |
|----------|-----|
| First example on a page | Full sample |
| Quickstart / Getting started | Full sample |
| Showing a specific API call | Fragment |
| Showing customization options | Fragment |
| Complete working example | Full sample |

### Code Verification Rules

| Rule | Description |
|------|-------------|
| Must compile | All code blocks must compile against SDK develop branch |
| Explicit types | Use `val client: StreamVideo = ...` not `val client = ...` |
| Real values | Use realistic placeholder values, not `"xxx"` or `"???"` |
| No internal APIs | Never use APIs not in `.api` files |

**Checking if an API is internal:**
```bash
# If a class/function is NOT in the .api file, it's internal
cat stream-video-android-*/api/*.api | grep "ClassName"
```

**Internal API alternatives:**
| Don't use | Use instead |
|-----------|-------------|
| `{ DefaultOnlineIndicator(...) }` | `{ /* default indicator */ }` |
| `{ LivestreamBackStage(call) }` | `{ /* default backstage UI */ }` |
| `DefaultModerationVideoFilter()` | Omit or use public alternative |
| Internal composables | Comment placeholder or public alternative |

---

## 3. Writing Style

### Voice and Tone

| Rule | Good | Bad |
|------|------|-----|
| Direct language | "The SDK provides..." | "In terms of the SDK..." |
| Active voice | "Call `startScreenSharing()` to begin" | "Screen sharing can be started by calling..." |
| User-centric | "You can customize..." | "The system allows..." |
| Imperative for instructions | "Add the dependency" | "You should add the dependency" |

### Banned Phrases

Remove these filler words and phrases:

- "Basically"
- "Simply" / "Just"
- "Obviously"
- "Actually"
- "In order to" → use "to"
- "In terms of"
- "It should be noted that"
- "As you can see"
- "Please note that" → use direct statement or Note callout

### Common Corrections

| Before | After |
|--------|-------|
| "the most easiest way" | "the easiest way" |
| "you can available the" | "you can use the" |
| "support to show" | "can display" |
| "by your taste" | "as needed" |
| "custom the video" | "customize the video" |
| "allows to" | "allows you to" or rephrase |
| "In case you want" | "To" or "If you want" |

### Terminology Dictionary

Use consistent terminology throughout:

| Use | Don't use |
|-----|-----------|
| customize | custom (as verb) |
| call | video call (unless distinguishing from audio) |
| participant | user (in call context) |
| local participant | current user / self |
| remote participant | other user |
| SDK | sdk / Sdk |
| API | api / Api |
| WebRTC | webrtc / WebRtc |
| WebSocket | websocket / Websocket |
| Jetpack Compose | jetpack compose |
| Android | android (in prose) |

---

## 4. Notes, Warnings, and Callouts

### Inline Notes

For brief notes within flow:
```markdown
> **Note:** Screen audio sharing requires the microphone to be unmuted.
```

### Admonition Blocks

For important callouts that need emphasis:

```markdown
<admonition type="note">
This is informational content the reader should know.
</admonition>

<admonition type="caution">
**Important:** This warns about potential issues or gotchas.
</admonition>

<admonition type="warning">
This is a serious warning about destructive or dangerous operations.
</admonition>
```

**Do NOT use `:::note` syntax** - Android docs don't support it. The `:::` syntax will be rendered as literal text. Always use `<admonition>` tags.

**When to use which:**
| Type | Use for |
|------|---------|
| `note` | Helpful information, tips, clarifications |
| `caution` | Potential issues, common mistakes, gotchas |
| `warning` | Data loss, security issues, breaking changes |

---

## 5. Android-Specific Conventions

### API Level Notes

Always specify minimum API level for features:
```markdown
Screen audio sharing is available on Android 10 (API level 29) and above.
```

Format: "Android X (API level Y)"

### Permissions

Document required permissions clearly:
```markdown
For Android 13+ (API level 33+), you need to request the `POST_NOTIFICATIONS` permission:

```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_CODE)
}
```
```

### Dependencies

Show both Groovy and Kotlin DSL when relevant:
```markdown
Add the dependency to your app-level `build.gradle.kts`:

```kotlin
dependencies {
    implementation("io.getstream:stream-video-android-ui-compose:$version")
}
```
```

Use `$version` or `x.x.x` as placeholder, link to version source.

---

## 6. Links and Navigation

### Internal Links

Use absolute paths from docs root:
```markdown
[CallContent](/video/docs/android/ui-components/call/call-content/)
[Camera & Microphone guide](/video/docs/android/guides/camera-and-microphone/)
```

**Pattern:** `/video/docs/android/[section]/[page]/`

### External Links

```markdown
[Android Media Projection API](https://developer.android.com/guide/topics/large-screens/media-projection)
```

### Cross-References

When referencing related content:
```markdown
For more details, see the [Video Theme guide](/video/docs/android/ui-components/video-theme/).
```

---

## 7. Sidebar and Navigation

### Sidebar Structure

The sidebar is defined in `_sidebars/[video][android].json`:

```json
{
  "baseURL": "/video/docs/android",
  "items": [
    {
      "label": "section-name",
      "children": [
        {
          "title": "Page Title",
          "slug": "/section/page-name/",
          "markdown": "video/android/XX-section/YY-page-name.md"
        }
      ]
    }
  ]
}
```

### File Naming

Files use numbered prefixes for ordering:
```
01-basics/
  01-introduction.md
  02-installation.md
  03-quickstart.md
03-guides/
  01-client-auth.md
  02-joining-creating-calls.md
```

### When to Create New Pages vs Update Existing

| Scenario | Action |
|----------|--------|
| New major feature | Create new page |
| New parameter to existing feature | Update existing page |
| New optional capability | Update existing + consider new page if complex |
| Bug fix / behavior change | Update existing page |
| Deprecation | Update existing + add deprecation notice |

**Creating new pages:**
1. Create `.md` file with correct number prefix
2. Add entry to sidebar JSON
3. Follow existing section's page structure

---

## 8. Deprecations and Breaking Changes

### Deprecation Notice Format

```markdown
<admonition type="caution">

**Deprecated:** `oldMethod()` is deprecated and will be removed in version X.Y.Z. Use `newMethod()` instead.

</admonition>
```

### Migration Guide Format

For breaking changes, provide clear migration:

```markdown
## Migrating from X to Y

### Before (deprecated)

```kotlin
// Old way - deprecated
call.oldMethod(param)
```

### After

```kotlin
// New way
call.newMethod(newParam)
```

### Changes
- `oldMethod` → `newMethod`
- Parameter `param` renamed to `newParam`
- Return type changed from `Unit` to `Result<Unit>`
```

### Removal Documentation

When API is removed:
1. Remove code examples using it
2. Remove or update any references
3. Update related pages that linked to it
4. Do NOT leave "removed in version X" notices indefinitely

---

## 9. SDK to Docs Mapping

### Detecting Changes

| Change Type | Detection Method | Doc Impact |
|-------------|------------------|------------|
| New public class | Added to `.api` file | New section or page |
| Changed signature | Modified in `.api` file | Update code examples |
| New parameter | Function signature change | Update examples + parameters table |
| Deprecated API | `@Deprecated` annotation | Add deprecation notice |
| Removed API | Removed from `.api` file | Remove from docs |
| Behavior change | PR description/commits | Update descriptions |

### API Files to Check

```bash
stream-video-android-core/api/*.api
stream-video-android-ui-core/api/*.api
stream-video-android-ui-compose/api/*.api
stream-video-android-filters-video/api/*.api
```

### Common Mappings

| SDK Area | Doc Location |
|----------|--------------|
| `StreamVideoBuilder` | `01-basics/`, `03-guides/01-client-auth.md` |
| `StreamVideo` interface | `03-guides/01-client-auth.md` |
| `Call` class | `03-guides/02-joining-creating-calls.md` |
| `call.state.*` | `03-guides/03-call-and-participant-state.md` |
| `call.camera`, `call.microphone` | `03-guides/04-camera-and-microphone.md` |
| `call.screenShare` | `06-advanced/04-screen-sharing.md` |
| `CallContent` | `04-ui-components/04-call/01-call-content.md` |
| `ParticipantVideo` | `04-ui-components/05-participants/01-participant-video.md` |
| `VideoTheme` | `04-ui-components/03-video-theme.md` |
| Video filters | `06-advanced/05-apply-video-filters.md` |
| Audio filters | `03-guides/05-noise-cancellation.md` |
| Push notifications | `06-advanced/00-incoming-calls/03-push-notifications.md` |
| Ringing calls | `06-advanced/00-incoming-calls/02-ringing.md` |
| Recording | `06-advanced/09-recording.md` |
| Livestreaming | `03-guides/12-livestreaming.md` |
| Picture-in-Picture | `06-advanced/03-enable-picture-in-picture.md` |
| Telecom integration | `06-advanced/12-telecom.md` |

### Search Strategy

When mapping SDK changes to docs:

1. **Grep for class/function names:**
   ```bash
   grep -r "ClassName\|functionName" video/android/ --include="*.md" -l
   ```

2. **Check the mapping table above**

3. **Check sidebar for section structure**

4. **Look at related pages** - changes often affect multiple files

---

## 10. Cross-Platform Awareness

### Reference Other Platforms

```
React docs: video/react/
iOS docs: video/ios/
Flutter docs: video/flutter/
```

### When to Check Other Platforms

| Scenario | Action |
|----------|--------|
| Writing new feature docs | Check if React/iOS has it, adapt good explanations |
| Unclear how to explain concept | See how other platforms document it |
| Android-specific behavior | Note the difference explicitly |
| API differs from other platforms | Document the Android way clearly |

### Documenting Platform Differences

```markdown
> **Note:** On Android, screen sharing requires explicit user permission via the Media Projection API. This differs from web browsers where permission is handled differently.
```

---

## 11. PR and Commit Standards

### Commit Messages

```
docs(android): [description]

[Optional body with more details]
```

**Examples:**
- `docs(android): add connectOnInit parameter to client auth guide`
- `docs(android): update screen sharing with audio capture feature`
- `docs(android): fix code example in video filters page`

### PR Description Template

```markdown
## Summary

[Brief description of doc changes]

## SDK Changes

Related to GetStream/stream-video-android#[PR_NUMBER]

- [Change 1 from SDK PR]
- [Change 2 from SDK PR]

## Doc Updates

- [File 1]: [what changed]
- [File 2]: [what changed]
```

### What NOT to Include

| Don't include | Why |
|---------------|-----|
| "Generated with Claude" | No AI attribution |
| "Co-Authored-By: Claude" | No AI co-author |
| Test checklists | Keep PR focused on changes |
| Emoji in commits | Keep professional |

---

## 12. Examples: Good vs Bad

### Example 1: Introduction Paragraph

**Bad:**
```markdown
## Introduction

In this document, we will basically be looking at how you can simply implement screen sharing in your Android application. As you probably know, screen sharing is a feature that allows users to share their screen.
```

**Good:**
```markdown
The Stream Video Android SDK supports screen sharing using the Android Media Projection API. Users with the `screenshare` capability can share their device screen with other call participants.
```

### Example 2: Code with Types

**Bad:**
```kotlin
val client = StreamVideoBuilder(
    context = context,
    apiKey = apiKey,
).build()
```

**Good:**
```kotlin
val client: StreamVideo = StreamVideoBuilder(
    context = applicationContext,
    apiKey = "mmhfdzb5evj2",
    geo = GEO.GlobalEdgeNetwork,
    user = user,
    token = token,
).build()
```

### Example 3: Parameter Documentation

**Bad:**
```markdown
The function takes some parameters that you can use.
```

**Good:**
```markdown
| Parameter | Description | Default |
|-----------|-------------|---------|
| `blurIntensity` | Intensity of the blur effect: `LIGHT`, `MEDIUM`, or `HEAVY` | `MEDIUM` |
| `foregroundThreshold` | Confidence threshold for foreground detection (0.0 to 1.0) | `0.99999` |
```

### Example 4: API Level Documentation

**Bad:**
```markdown
This feature requires a newer Android version.
```

**Good:**
```markdown
Screen audio sharing is available on Android 10 (API level 29) and above. On older devices, only screen video is captured.
```

### Example 5: Note vs Inline

**Bad (overusing notes):**
```markdown
> **Note:** Call `startScreenSharing()` to begin.

> **Note:** Pass the intent data from the permission result.

> **Note:** The user must have the screenshare capability.
```

**Good (notes for important info only):**
```markdown
Call `startScreenSharing()` with the intent data from the Media Projection permission result. The user must have the `screenshare` capability.

> **Note:** Screen audio sharing requires the microphone to be unmuted. When the microphone is muted, both microphone and screen audio are silenced.
```

---

## 13. Verification Checklist

Before creating or updating a docs PR:

### Content
- [ ] All code blocks compile against SDK develop
- [ ] Explicit types used in code examples
- [ ] No internal APIs exposed
- [ ] Correct language tags on code blocks

### Writing
- [ ] Direct writing style (no filler phrases)
- [ ] Consistent terminology
- [ ] API levels documented for version-specific features
- [ ] Required permissions documented

### Structure
- [ ] Page follows standard structure
- [ ] Proper heading hierarchy
- [ ] Links use correct format

### PR
- [ ] Commit message follows format
- [ ] PR description follows template
- [ ] SDK PR referenced
- [ ] No AI attribution anywhere

### Navigation (if new page)
- [ ] File has correct number prefix
- [ ] Sidebar JSON updated
- [ ] Related pages link to new page

---

*Comprehensive standards derived from Android Video Documentation Audit (v1.0, v1.1) and cross-platform analysis.*
