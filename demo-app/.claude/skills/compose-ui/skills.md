---
name: demo-app-compose-ui-skill
description: >
  Use this skill whenever writing or modifying Jetpack Compose UI inside the demo-app (dogfooding)
  module of the GetStream Video Android project. Triggers on any request to build, create, design,
  or update a Compose screen, component, composable, or UI element. Also triggers for requests
  involving UI layout, styling, theming, colors, or typography. Always use this skill — do not
  rely on general Compose knowledge alone — when working inside the dogfooding module.
---

# Demo-app — Compose UI Skill

Follow every rule in this skill without exception when generating or modifying Compose UI in the
dogfooding module.

---

## Scope assertion

This skill is scoped exclusively to the **demo-app (`dogfooding`) module**.

If the requested UI belongs in the **core SDK** (`stream-video-android-ui-compose`) or any other
module, stop immediately and say:

> "This looks like it belongs in the SDK module, not the demo app. Switch to the SDK Compose UI
> skill for that."

Do not write any code until scope is confirmed as demo-app.

---

## Theming and styling rules

| Token type        | Source file                                                                     |
|-------------------|---------------------------------------------------------------------------------|
| Colors            | `stream-video-android-ui-compose/src/main/kotlin/.../theme/StreamColors.kt`     |
| Typography        | `stream-video-android-ui-compose/src/main/kotlin/.../theme/StreamTypography.kt` |
| Shapes            | `stream-video-android-ui-compose/src/main/kotlin/.../theme/StreamShapes.kt`     |
| Dimensions        | `stream-video-android-ui-compose/src/main/kotlin/.../theme/StreamDimens.kt`     |
| Theme entry point | `stream-video-android-ui-compose/src/main/kotlin/.../theme/VideoTheme.kt`       |

### Token lookup rule

For every color, dimension, shape, and typography value:

1. **Check the relevant theme file first** — if a matching token exists, use it via `VideoTheme`
2. **If no token exists** — hardcode the value directly, no comment needed

```kotlin
// Token exists → use it
Text(
    style = VideoTheme.typography.bodyBold,
    color = VideoTheme.colors.textHighEmphasis,
)

// No token exists → hardcode cleanly
Box(
    modifier = Modifier
        .size(300.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(Color.Black)
)
```

> ℹ️ Typography is the exception — always use a `VideoTheme.typography` token. Never hardcode
> `fontSize`, `fontWeight`, or `lineHeight` directly.

---

## Architecture rules

> ℹ️ These rules apply project-wide. They are restated here for completeness but live
> canonically in the root base skill at `/.claude/skills/compose-ui-base/SKILL.md`.

### Not used anywhere in this project
- No `ViewModel`
- No `UseCase` classes
- No `Repository` classes

### Unidirectional Data Flow (UDF)

State flows **down**, events flow **up**.

#### Pattern A — StateFlow hoisted to a plain class or coordinator
```kotlin
data class LivestreamViewerState(
    val isLive: Boolean = false,
    val viewerCount: Int = 0,
    val durationSeconds: Int = 0,
    val isMuted: Boolean = false,
)

@Composable
fun LivestreamViewerScreen(
    state: LivestreamViewerState,
    onMuteToggle: () -> Unit,
    onLeave: () -> Unit,
) { ... }
```

#### Pattern B — Compose State with hoisted lambdas (purely local UI state)
```kotlin
@Composable
fun PipPlayerCard(
    isVisible: Boolean,
    onExpand: () -> Unit,
    onDismiss: () -> Unit,
) {
    var isDragging by remember { mutableStateOf(false) }
}
```

#### Rules
- Hoist state to the caller — never own shared state inside a leaf composable
- `LaunchedEffect` / `DisposableEffect` are allowed for lifecycle-scoped work
- Use `collectAsStateWithLifecycle()` as the `StateFlow` → Compose bridge
- `StateFlow` state at screen level; `remember`/`mutableStateOf` for ephemeral local UI state only

---

## String resources

> ⚠️ Never use hardcoded string literals inside `Text()` or any other composable.
> All user-visible strings must come from `dogfooding/src/main/res/values/strings.xml`.

```kotlin
// Correct
Text(text = stringResource(R.string.pip_video_player_minimized))

// Wrong — never do this
Text(text = "Video player is minimized")
```

If the required string does not exist in `strings.xml`, **create it** there before using it.
Follow the existing naming convention in the file (e.g. `screen_name_description`).

---

## File location

All new files must go under:

```
dogfooding/src/main/kotlin/     ← Kotlin sources
dogfooding/src/main/res/        ← Resources (if any)
```

Never create files in `stream-video-android-ui-compose`, `stream-video-android-core`, or any
other module for demo-app UI.

---

## Code generation checklist

Before outputting any code:

- [ ] Scope asserted as demo-app
- [ ] Colors — token used if present in `StreamColors.kt`, else hardcoded cleanly
- [ ] Dimensions — token used if present in `StreamDimens.kt`, else hardcoded cleanly
- [ ] Shapes — token used if present in `StreamShapes.kt`, else hardcoded cleanly
- [ ] Typography — always from `StreamTypography.kt`, never hardcoded
- [ ] No `ViewModel`, `UseCase`, or `Repository` introduced
- [ ] State hoisted appropriately — shared state at screen level, local state with `remember`
- [ ] All event callbacks are lambdas passed from the caller
- [ ] File path is inside `dogfooding/src/main/kotlin/`
- [ ] All strings via `stringResource(R.string.*)` — none hardcoded inline
- [ ] New strings added to `dogfooding/src/main/res/values/strings.xml` if not already present
- [ ] `@Preview` included, wrapped in `VideoTheme {}`

---

## Preview conventions

```kotlin
@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun LivestreamViewerScreenPreview() {
    VideoTheme {
        LivestreamViewerScreen(
            state = LivestreamViewerState(isLive = true, viewerCount = 3),
            onMuteToggle = {},
            onLeave = {},
        )
    }
}
```

---

## Clarification triggers

| Situation                          | Ask                                                                                                           |
|------------------------------------|---------------------------------------------------------------------------------------------------------------|
| Correct theme token is unclear     | "Checked `StreamColors` / `StreamTypography` / `StreamDimens` — no matching token found, hardcoding directly" |
| State ownership is ambiguous       | "Who owns this state — the parent screen, or is it local to this composable?"                                 |
| String key naming is ambiguous     | "What should this string key be named in `strings.xml`? Suggest: `[proposed_key]`"                            |
| New screen needs navigation wiring | "How is this screen reached — direct composable call or nav graph entry?"                                     |