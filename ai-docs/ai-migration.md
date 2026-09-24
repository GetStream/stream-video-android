# Stream Video Android SDK: v1 to v2 AI Migration Guide (Compose UI)

## §0 Agent Instructions

> **Your training data is stale.** The Compose UI layer was rebuilt on a new design system between v1 and v2. Do NOT guess theme, color, typography or component names from memory. Follow this guide.

### Scope

This guide covers the **Compose UI changes** of v2: the `StreamDesign` token system, `VideoUiConfig`, the new base components, the design system icons, and the removed styling layer and resources.

It does **not** cover the non-UI changes of v2 yet (Kotlin toolchain, the `stream-android-core` dependency, the connection state API). If the project fails to compile on a symbol that is not listed here, read the public API files in §9 instead of guessing.

### Trust hierarchy

1. This guide (rename lists and patterns taken from the v2 source)
2. The source files listed in §9 (they are the ground truth)
3. Your training data (LAST resort, assume it is wrong for v2)

### Work order

Execute the sections in order. Each section assumes the previous one is done.

1. **§1 Detection**: scan the codebase to find which sections apply
2. **§2 Prerequisites**: update the dependency versions
3. **§3 Structural migrations**: `VideoTheme`, `StreamDesign`, `VideoUiConfig`, styling layer, `VideoComponentFactory`
4. **§4 Rename lists**: colors, typography, dimens, shapes
5. **§5 Component API changes**: buttons, call actions, dialog, text field, avatar, removed components
6. **§6 Resources**: icons, removed `ui-core` resources
7. **§7 Behavior changes**: no compile error, but different runtime result
8. **§8 Machine-readable rename block**: JSON for automated tools
9. **§9 Source paths to read**
10. **§10 Verification**: greps, build, smoke test

### Key facts

- v1 latest: check Maven Central for the latest `1.x` release of `io.getstream:stream-video-android-ui-compose`
- v2 latest: check Maven Central for the latest `2.x` release
- v2 branch: `develop-v2`. v1 branch: `develop`
- The Compose module (`stream-video-android-ui-compose`) is the only UI module with API changes. The XML module (`stream-video-android-ui-xml`) API did not change
- New theme package: `io.getstream.video.android.compose.theme.design` (`StreamDesign`, `StreamTokens`, `StreamPrimitiveColors`)
- New base components package: `io.getstream.video.android.compose.ui.components.base` (`StreamButton`, `StreamTextButton`, `StreamIconButton`, `StreamButtonStyle`, `StreamButtonStyleDefaults`, `StreamButtonSize`, `StreamDialog`, `StreamTextField`)
- `VideoComponentFactory` is NOT new in v2. It shipped in v1.32.0. v2 adds one method to it (§3.5)

### Platform note

Shell commands use Unix syntax (bash). On Windows, use `gradlew.bat` instead of `./gradlew`, and run the `find`/`rg`/`perl` commands through WSL or Git Bash.

---

## §1 Detection

Run these commands from the project root. Each match maps to a section you must apply.

```bash
# §3.1 VideoTheme parameters
rg "VideoTheme\s*\(" --type kotlin
rg "dimens\s*=|shapes\s*=|rippleConfiguration\s*=|reactionMapper\s*=|allowUIAutomationTest\s*=|styles\s*=" --type kotlin

# §3.2 / §4 Theme classes and accessors
rg "StreamColors|StreamDimens|StreamTypography|StreamShapes|StreamRippleConfiguration" --type kotlin
rg "VideoTheme\.(colors|typography|dimens|shapes|styles|reactionMapper|rippleConfiguration)\." --type kotlin

# §3.4 Styling layer
rg "io\.getstream\.video\.android\.compose\.ui\.components\.base\.styling" --type kotlin
rg "CompositeStyleProvider|ButtonStyles|StreamFixedSizeButtonStyle|StyleSize|DialogStyle|TextFieldStyle|BadgeStyle|IconStyle|StreamTextStyle" --type kotlin

# §5.1 Base components
rg "GenericStreamButton|GenericToggleButton|StreamToggleButton|StreamIconToggleButton|StreamDrawableButton|StreamDrawableToggleButton" --type kotlin
rg "StreamButton\s*\(|StreamIconButton\s*\(" --type kotlin
rg "StreamDialogPositiveNegative|StreamDialog\s*\(|StreamOutlinedTextField|StreamTextField\s*\(" --type kotlin
rg "StreamBadgeBox|GenericContainer" --type kotlin

# §5.2 Call action composables
rg "(Toggle\w*Action|GenericAction|ToggleAction|AcceptCallAction|DeclineCallAction|CancelCallAction|LeaveCallAction|FlipCameraAction|ChatDialogAction|ClosedCaptionsToggleAction|ScreenShareToggleAction|ToggleSettingsAction)\s*\(" --type kotlin

# §5.3 Avatars
rg "UserAvatar\s*\(|UserAvatarBackground\s*\(|textOffset\s*=" --type kotlin

# §5.4 ParticipantAction
rg "ParticipantAction\s*\(" --type kotlin

# §5.5 Removed participants menu and invite flow
rg "CallParticipantsInfoMenu|CallParticipantInfoMode|ParticipantInfoAction|InviteUsers\b|ChangeMuteState" --type kotlin

# §6.1 Icons
rg "stream_video_ic_(close|join_call|leave|live|message|options|play|reaction|screensharing|selected)\b" --type kotlin --type xml

# §6.2 Removed ui-core resources (overrides and direct references)
rg "@(color|dimen|string)/stream_video_|R\.(color|dimen|string|plurals)\.stream_video_" --type kotlin --type xml

# §7 Behavior changes: custom onCallAction handlers
rg "ClosedCaptionsAction|CancelCall\b|ChatDialog\b" --type kotlin
```

If a command returns no matches, skip the matching section.

---

## §2 Prerequisites

Update every Stream Video artifact to the same v2 version. If you use the BOM, update only the BOM version.

```kotlin
// build.gradle.kts
implementation(platform("io.getstream:stream-video-android-bom:<v2 version>"))
implementation("io.getstream:stream-video-android-ui-compose")
```

---

## §3 Structural Migrations

### §3.1 VideoTheme parameters

**v1 signature:**

```kotlin
VideoTheme(
    isInDarkMode = isSystemInDarkTheme(),
    colors = StreamColors.defaultColors(),
    dimens = StreamDimens.defaultDimens(),
    typography = StreamTypography.defaultTypography(colors, dimens),
    shapes = StreamShapes.defaultShapes(dimens),
    rippleConfiguration = StreamRippleConfiguration,
    reactionMapper = ReactionMapper.defaultReactionMapper(),
    allowUIAutomationTest = true,
    styles = CompositeStyleProvider(),
    componentFactory = DefaultVideoComponentFactory,
) { content() }
```

**v2 signature:**

```kotlin
VideoTheme(
    isInDarkMode = isSystemInDarkTheme(),
    config = VideoUiConfig(),
    colors = if (isInDarkMode) StreamDesign.Colors.defaultDark() else StreamDesign.Colors.default(),
    typography = StreamDesign.Typography.default(),
    componentFactory = DefaultVideoComponentFactory,
) { content() }
```

| Removed parameter | Replacement |
|---|---|
| `dimens` | Removed. Use `StreamTokens` (§4.3) |
| `shapes` | Removed. Use `StreamTokens.radius*` (§4.4) |
| `rippleConfiguration` | Removed. The ripple is derived from `colors` |
| `reactionMapper` | `config = VideoUiConfig(reactionMapper = ...)` |
| `allowUIAutomationTest` | `config = VideoUiConfig(allowUIAutomationTest = ...)` |
| `styles` | Removed with the styling layer (§3.4) |

`isInDarkMode` now has an effect. See §7.1.

**Accessor changes on `VideoTheme` (the `StreamTheme` interface):**

| v1 | v2 |
|---|---|
| `VideoTheme.colors` (`StreamColors`) | `VideoTheme.colors` (`StreamDesign.Colors`) |
| `VideoTheme.typography` (`StreamTypography`) | `VideoTheme.typography` (`StreamDesign.Typography`) |
| `VideoTheme.dimens` | Removed. Use `StreamTokens` |
| `VideoTheme.shapes` | Removed. Use `StreamTokens.radius*` |
| `VideoTheme.reactionMapper` | `VideoTheme.config.reactionMapper` |
| `VideoTheme.rippleConfiguration` | Removed |
| `VideoTheme.styles` | Removed |
| (none) | `VideoTheme.config` (`VideoUiConfig`) |
| `VideoTheme.componentFactory` | Unchanged |

`LocalVideoUiConfig` is a new public `CompositionLocal` that holds the `VideoUiConfig`.

### §3.2 StreamDesign colors and typography

#### Colors

```kotlin
// v1
VideoTheme(colors = StreamColors.defaultColors().copy(brandPrimary = Color(0xFF6200EE))) { ... }

// v2: light and dark palettes
val light = StreamDesign.Colors.default()
val dark = StreamDesign.Colors.defaultDark()
VideoTheme(colors = if (isSystemInDarkTheme()) dark else light) { ... }
```

To change the brand color, pass a custom `brand` scale. The accent, link and other brand-derived colors follow the scale.

```kotlin
val brand = StreamDesign.ColorScale(
    s50 = Color(0xFFF3E5F5),
    s100 = Color(0xFFE1BEE7),
    s150 = Color(0xFFD7A8DE),
    s200 = Color(0xFFCE93D8),
    s300 = Color(0xFFBA68C8),
    s400 = Color(0xFFAB47BC),
    s500 = Color(0xFF9C27B0),
    s600 = Color(0xFF8E24AA),
    s700 = Color(0xFF7B1FA2),
    s800 = Color(0xFF6A1B9A),
    s900 = Color(0xFF4A148C),
)
val light = StreamDesign.Colors.default(brand = brand)
val dark = StreamDesign.Colors.defaultDark(brand = brand.inverted())
```

`StreamDesign.Colors` is a data class, so single colors can also be changed with `copy(...)`. The full rename table is in §4.1.

#### Typography

```kotlin
// v1
StreamTypography.defaultTypography(colors, dimens)

// v2
StreamDesign.Typography.default(fontFamily = myFontFamily)
```

v2 text styles have **no color**. See §7.3. The rename table is in §4.2.

#### Removed theme classes

Delete all references to:

- `StreamColors` → `StreamDesign.Colors`
- `StreamTypography` → `StreamDesign.Typography`
- `StreamDimens` → `StreamTokens` (public object, `io.getstream.video.android.compose.theme.design`)
- `StreamShapes` → `RoundedCornerShape(StreamTokens.radius*)` or `CircleShape`
- `StreamRippleConfiguration` → no replacement

### §3.3 VideoUiConfig

`VideoUiConfig` is a new data class for the non-visual `VideoTheme` options.

```kotlin
VideoTheme(
    config = VideoUiConfig(
        allowUIAutomationTest = true,
        reactionMapper = ReactionMapper.defaultReactionMapper(),
    ),
) { content() }
```

### §3.4 Styling layer removed

The whole `io.getstream.video.android.compose.ui.components.base.styling` package is removed, together with `VideoTheme(styles = ...)` and `VideoTheme.styles`.

Removed types: `CompositeStyleProvider`, `StreamStyle`, `StyleSize`, `StyleState`, `ButtonStyles`, `ButtonStyleProvider`, `StreamButtonStyle` (the old open class), `StreamFixedSizeButtonStyle`, `ButtonDrawableStyle`, `ButtonDrawableStyles`, `ButtonDrawableStyleProvider`, `StreamButtonDrawableStyle`, `DialogStyle`, `DialogStyleProvider`, `StreamDialogStyles`, `TextFieldStyle`, `TextFieldStyleProvider`, `StreamTextFieldStyles`, `BadgeStyle`, `BadgeStyleProvider`, `StreamBadgeStyles`, `IconStyle`, `IconStyleProvider`, `IconStyles`, `StreamIconStyle`, `TextStyleProvider`, `TextStyleWrapper`, `StreamTextStyle`, `StreamTextStyles`.

**Button style presets:**

| v1 (`VideoTheme.styles.buttonStyles.*`) | v2 (`StreamButtonStyleDefaults.*`) |
|---|---|
| `primaryButtonStyle()` | `primarySolid` |
| `secondaryButtonStyle()` | `secondarySolid` |
| `tertiaryButtonStyle()` | `secondaryGhost` (closest match) |
| `alertButtonStyle()` | `destructiveSolid` |
| `toggleButtonStyleOn()` | `secondarySolid` |
| `toggleButtonStyleOff()` | `destructiveSolid` |
| `primaryIconButtonStyle()` | `primarySolid` |
| `secondaryIconButtonStyle()` | `secondarySolid` |
| `tertiaryIconButtonStyle()` | `secondaryGhost` (closest match) |
| `onlyIconIconButtonStyle()` | `secondaryGhost` (closest match) |
| `alertIconButtonStyle()` | `destructiveSolid` |
| `primaryDrawableButtonStyle()` | `primarySolid` |
| `drawableToggleButtonStyleOn()` / `Off()` | `secondarySolid` / `destructiveSolid` |

The other presets are `primaryOutline`, `primaryGhost`, `secondaryOutline`, `destructiveOutline` and `destructiveGhost`.

The size of a v1 style (`StyleSize`) is now a separate parameter: `size: StreamButtonSize` (`Small` 32dp, `Medium` 40dp, `Large` 48dp, `ExtraLarge` 64dp).

A custom style is a plain data class:

```kotlin
StreamButtonStyle(
    containerColor = VideoTheme.colors.accentPrimary,
    contentColor = VideoTheme.colors.textOnAccent,
    borderColor = null,
    disabledContainerColor = VideoTheme.colors.backgroundUtilityDisabled,
    disabledContentColor = VideoTheme.colors.textDisabled,
    disabledBorderColor = null,
)
```

Dialog, text field and badge styles have no replacement class. These components read `VideoTheme.colors` directly.

### §3.5 VideoComponentFactory

`VideoComponentFactory` exists since v1.32.0, and the composable slot parameters still work in v2. No migration is required.

v2 adds one method, used by the lobby, the audio room participants and the ringing avatars:

```kotlin
object MyFactory : VideoComponentFactory {
    @Composable
    override fun UserAvatar(params: UserAvatarParams) {
        // params.userImage, params.userName, params.modifier, params.isShowingOnlineIndicator
    }
}

VideoTheme(componentFactory = MyFactory) { ... }
```

---

## §4 Rename Lists

### §4.1 Colors (`VideoTheme.colors.*`)

"Used by the SDK" means the SDK itself made this change in v2. "Closest match" means v2 has no token with the same role, so pick it on purpose or use your own `Color`.

| v1 | v2 | Note |
|---|---|---|
| `brandPrimary` | `accentPrimary` | Used by the SDK |
| `brandPrimaryLt` | `brand.s400` | Closest match |
| `brandPrimaryDk` | `brand.s700` | Closest match |
| `brandSecondary` | `backgroundCoreSurfaceDefault` | Closest match |
| `brandSecondaryTransparent` | `backgroundUtilityDisabled` | Closest match |
| `brandCyan` | none | Use your own `Color` |
| `brandGreen` | `accentSuccess` | Used by the SDK |
| `brandYellow` | `accentWarning` | Used by the SDK |
| `brandRed` | `accentError` | Used by the SDK |
| `brandRedLt` | none | Use your own `Color` |
| `brandRedDk` | none | Use your own `Color` |
| `brandMaroon` | none | Use your own `Color` |
| `brandViolet` | none | Use your own `Color` |
| `basePrimary` | `textPrimary` | Used by the SDK. Use `textOnAccent` for text and icons drawn over video or dark overlays |
| `baseSecondary` | `textSecondary` | Used by the SDK |
| `baseTertiary` | `textTertiary` | Used by the SDK |
| `baseQuaternary` | `textTertiary` | Used by the SDK |
| `baseQuinary` | `textTertiary` (text), `borderCoreDefault` (borders) | Used by the SDK for a border |
| `baseSenary` | `borderCoreDefault` | Used by the SDK |
| `baseSheetPrimary` | `backgroundCoreApp` | Used by the SDK |
| `baseSheetSecondary` | `backgroundCoreElevation1` | Used by the SDK |
| `baseSheetTertiary` | `backgroundCoreSurfaceDefault` | Used by the SDK |
| `baseSheetQuarternary` | `backgroundCoreOverlayDarkStrong` | Used by the SDK |
| `buttonPrimaryDefault` | `backgroundCoreSurfaceDefault` | Or use `StreamButtonStyleDefaults.secondarySolid` |
| `buttonPrimaryPressed` | none | Pressed state is a ripple. Use a `StreamButtonStyle` |
| `buttonPrimaryDisabled` | `backgroundUtilityDisabled` | Closest match |
| `buttonBrandDefault` | `accentPrimary` | Used by the SDK. Or use `StreamButtonStyleDefaults.primarySolid` |
| `buttonBrandPressed` | none | Pressed state is a ripple |
| `buttonBrandDisabled` | `backgroundUtilityDisabled` | Closest match |
| `buttonAlertDefault` | `accentError` | Or use `StreamButtonStyleDefaults.destructiveSolid` |
| `buttonAlertPressed` | none | Pressed state is a ripple |
| `buttonAlertDisabled` | `backgroundUtilityDisabled` | Closest match |
| `iconDefault` | `textPrimary` | Used by the SDK |
| `iconPressed` | `textSecondary` | Closest match |
| `iconActive` | `accentPrimary` | Closest match |
| `iconAlert` | `accentError` | Closest match |
| `iconDisabled` | `textDisabled` | Closest match |
| `alertSuccess` | `accentSuccess` | Used by the SDK |
| `alertCaution` | `accentWarning` | Used by the SDK |
| `alertWarning` | `accentError` | Used by the SDK. **Name trap:** v1 "warning" was red. Do NOT map it to `accentWarning` |

### §4.2 Typography (`VideoTheme.typography.*`)

| v1 (size, weight) | v2 (size, weight) | Note |
|---|---|---|
| `titleL` (93sp, W500) | `headingLarge` (20sp, W600) | Used by the SDK. No v2 style is this large, use `headingLarge.copy(fontSize = ...)` to keep the size |
| `titleM` (48sp, W500) | `headingLarge` | Used by the SDK |
| `titleS` (24sp, W500) | `headingLarge` | Used by the SDK |
| `titleXs` (13sp, W600) | `headingExtraSmall` (12sp, W600) | Used by the SDK |
| `subtitleL` (24sp, W500) | `headingLarge` | Closest match |
| `subtitleM` (20sp, W500) | `headingSmall` (16sp, W600) | Used by the SDK |
| `subtitleS` (16sp, W500) | `bodyDefault` (16sp, W400) | Used by the SDK |
| `bodyL` (20sp, W400) | `bodyDefault` | Used by the SDK |
| `bodyM` (16sp, W400) | `bodyDefault` | Used by the SDK |
| `bodyS` (13sp, W400) | `captionDefault` (14sp, W400) | Used by the SDK |
| `labelL` (20sp, W600) | `headingLarge` | Used by the SDK |
| `labelM` (16sp, W600) | `bodyEmphasis` (16sp, W600) | Used by the SDK |
| `labelS` (13sp, W500) | `captionEmphasis` (14sp, W600) | Used by the SDK |
| `labelXS` (13sp, W500) | `metadataEmphasis` (12sp, W600) | Used by the SDK |

Other v2 styles: `headingMedium` (18sp, W600), `metadataDefault` (12sp, W400), `numericSmall`, `numericMedium`, `numericLarge`, `numericExtraLarge` (bold, 8sp to 14sp, for counters and badges).

### §4.3 Dimens (`VideoTheme.dimens.*` → `StreamTokens.*`)

`StreamTokens` is a public object in `io.getstream.video.android.compose.theme.design`. Mapped by value.

| v1 (value) | v2 |
|---|---|
| `genericMax` (100dp) | none, use `100.dp` |
| `generic3xl` (84dp) | `size80` (closest) |
| `genericXxl` (44dp) | `size48` (closest) or `44.dp` |
| `genericXl` (32dp) | `size32` |
| `genericL` (24dp) | `size24` |
| `genericM` (16dp) | `size16` |
| `genericS` (8dp) | `size8` |
| `genericXs` (4dp) | `size4` |
| `genericXXs` (2dp) | `size2` |
| `roundnessXl` (32dp) | `radius4xl` (`CornerSize`) |
| `roundnessL` (24dp) | `radius3xl` (`CornerSize`) |
| `roundnessM` (16dp) | `radiusXl` (`CornerSize`) |
| `roundnessS` (8dp) | `radiusMd` (`CornerSize`) |
| `spacingXl` (32dp) | `spacing2xl` |
| `spacingL` (24dp) | `spacingXl` |
| `spacingM` (16dp) | `spacingMd` |
| `spacingS` (8dp) | `spacingXs` |
| `spacingXs` (4dp) | `spacing2xs` |
| `spacingXXs` (2dp) | `spacing3xs` |
| `componentHeightL` (44dp) | `size48` (the SDK moved to 48dp) |
| `componentHeightM` (32dp) | `size32` |
| `componentHeightS` (24dp) | `size24` |
| `componentPaddingTop` / `Bottom` / `Fixed` (8dp) | `spacingXs` |
| `componentPaddingStart` / `End` (16dp) | `spacingMd` |
| `textSizeXxl` (93sp) | none |
| `textSizeXl` (48sp) | none |
| `textSizeL` (24sp) | `fontSize2xl` |
| `textSizeM` (20sp) | `fontSizeXl` |
| `textSizeS` (16sp) | `fontSizeMd` |
| `textSizeXs` (13sp) | `fontSizeXs` (12sp, closest) |
| `lineHeightXxl` (43sp) / `lineHeightXl` (28sp) | none |
| `lineHeightL` (24sp) | `lineHeightRelaxed` |
| `lineHeightM` (20sp) | `lineHeightNormal` |
| `lineHeightS` (16sp) | `lineHeightTight` |
| `lineHeightXs` (13sp) | none |

> **Name trap:** the spacing names shifted. v1 `spacingL` is 24dp, v2 `spacingXl` is 24dp. v1 `spacingS` is 8dp, v2 `spacingXs` is 8dp. Map by value, never by name.

`radius*` tokens are `CornerSize`, not `Dp`. Use them as `RoundedCornerShape(StreamTokens.radiusXl)`.

### §4.4 Shapes (`VideoTheme.shapes.*`)

| v1 | v2 |
|---|---|
| `circle` | `CircleShape` |
| `square` | `RectangleShape` |
| `button` | `RoundedCornerShape(StreamTokens.buttonRadiusFull)` |
| `input` | `RoundedCornerShape(StreamTokens.inputRadiusTextInput)` |
| `sheet` | `RoundedCornerShape(StreamTokens.radiusXl)` |
| `dialog` | `RoundedCornerShape(StreamTokens.radius3xl)` |
| `container` | `RoundedCornerShape(StreamTokens.radius4xl)` |
| `indicator` | `RoundedCornerShape(StreamTokens.radiusMd)` |

---

## §5 Component API Changes

### §5.1 Base components

**Icons are `Painter`, not `ImageVector`.** Every v2 base component and call action takes `Painter`. Use `painterResource(R.drawable.stream_design_ic_*)` (with `io.getstream.video.android.compose.R`), or keep a Material icon with `rememberVectorPainter(Icons.Default.Mic)`.

**Buttons:**

```kotlin
// v1
StreamButton(
    text = "Join",
    icon = Icons.Default.Call,
    style = VideoTheme.styles.buttonStyles.primaryButtonStyle(),
    onClick = { ... },
)

// v2: text button
StreamTextButton(
    onClick = { ... },
    text = "Join",
    leadingIcon = painterResource(R.drawable.stream_design_ic_phone_fill),
    style = StreamButtonStyleDefaults.primarySolid,
    size = StreamButtonSize.Medium,
)

// v2: icon-only button
StreamIconButton(
    onClick = { ... },
    icon = painterResource(R.drawable.stream_design_ic_settings),
    contentDescription = "Settings",
)

// v2: button with custom content
StreamButton(onClick = { ... }) { Text("Custom") }
```

| Removed v1 | v2 replacement |
|---|---|
| `StreamButton(text, icon: ImageVector, style, showProgress, ...)` | `StreamTextButton` or `StreamButton(onClick, content)` |
| `StreamIconButton(icon: ImageVector, style: StreamFixedSizeButtonStyle, ...)` | `StreamIconButton(onClick, icon: Painter, contentDescription, ...)` |
| `GenericStreamButton` | `StreamButton` |
| `StreamToggleButton`, `StreamIconToggleButton`, `GenericToggleButton` | `ToggleAction` (§5.2), or pick the style from your own state |
| `StreamDrawableButton`, `StreamDrawableToggleButton` | `StreamIconButton` with `painterResource(...)` |

The v1 `showProgress`, `textOverflow` and `textMaxLines` parameters have no v2 replacement.

**Dialog:**

```kotlin
// v1
StreamDialogPositiveNegative(
    title = "Leave call?",
    contentText = "You will leave the call.",
    positiveButton = Triple("Leave", ButtonStyles.alertButtonStyle(), { leave() }),
    negativeButton = Triple("Cancel", ButtonStyles.secondaryButtonStyle(), { dismiss() }),
    onDismiss = { dismiss() },
)

// v2: the buttons are content
StreamDialog(
    onDismissRequest = { dismiss() },
    title = "Leave call?",
    message = "You will leave the call.",
) {
    StreamTextButton(onClick = { leave() }, text = "Leave", style = StreamButtonStyleDefaults.destructiveSolid)
    StreamTextButton(onClick = { dismiss() }, text = "Cancel", style = StreamButtonStyleDefaults.secondaryOutline)
}
```

`StreamDialog(style: DialogStyle, dialogProperties, content: BoxScope.() -> Unit)` is removed. v2 `StreamDialog` requires a `title`, has `dismissOnBackPress` and `dismissOnClickOutside` instead of `DialogProperties`, and its content is `ColumnScope`.

**Text field:**

| v1 | v2 |
|---|---|
| `StreamTextField(modifier, value, onValueChange, ..., style, placeholder, error: Boolean, icon: ImageVector?, ...)` | `StreamTextField(value, onValueChange, modifier, ..., placeholder, errorText: String?, leadingIcon: Painter?, trailingIcon: Painter?, ...)` |
| `StreamOutlinedTextField(...)` | `StreamTextField(...)` |

`value` and `onValueChange` are now the first parameters. An error is shown when `errorText` is not null.

**Other base components:**

| v1 | v2 |
|---|---|
| `StreamBadgeBox(..., style: BadgeStyle, content)` | `StreamBadgeBox(..., content)`. The `style` parameter is removed |
| `GenericContainer(..., roundness: Dp = VideoTheme.dimens.roundnessL, ...)` | `GenericContainer(..., roundness: CornerSize = StreamTokens.radius3xl, ...)` |

### §5.2 Call action composables

All call action composables replaced their color, shape and style parameters with `StreamButtonStyle` and `StreamButtonSize`.

```kotlin
// v1
ToggleMicrophoneAction(
    isMicrophoneEnabled = enabled,
    shape = CircleShape,
    enabledColor = Color.DarkGray,
    disabledColor = Color.Red,
    enabledIconTint = Color.White,
    disabledIconTint = Color.White,
    onCallAction = onCallAction,
)

// v2
ToggleMicrophoneAction(
    isMicrophoneEnabled = enabled,
    onStyle = StreamButtonStyleDefaults.secondarySolid,
    offStyle = StreamButtonStyleDefaults.destructiveSolid,
    size = StreamButtonSize.Large,
    onCallAction = onCallAction,
)
```

| Composable | Removed parameters | New parameters |
|---|---|---|
| `ToggleMicrophoneAction`, `ToggleCameraAction`, `ToggleSpeakerphoneAction`, `ToggleHifiAudioAction` | `shape`, `enabledColor`, `disabledColor`, `enabledIconTint`, `disabledIconTint`, `onStyle: StreamFixedSizeButtonStyle?`, `offStyle: StreamFixedSizeButtonStyle?` | `onStyle: StreamButtonStyle`, `offStyle: StreamButtonStyle`, `size` |
| `ClosedCaptionsToggleAction`, `ScreenShareToggleAction`, `ToggleSettingsAction` | `shape`, `enabledColor`, `disabledColor` | `onStyle`, `offStyle`, `size` |
| `ToggleAction` | `iconOnOff: Pair<ImageVector, ImageVector>`, `shape`, the four color parameters, `onStyle`/`offStyle` (old type) | `iconOnOff: Pair<Painter, Painter>`, `contentDescription`, `onStyle`, `offStyle`, `size` |
| `GenericAction` | `icon: ImageVector`, `shape`, `color`, `iconTint`, `style: StreamFixedSizeButtonStyle?` | `icon: Painter`, `contentDescription`, `style: StreamButtonStyle`, `size` |
| `AcceptCallAction`, `DeclineCallAction`, `CancelCallAction` | `icon: ImageVector?`, `bgColor`, `iconTint`, `style: StreamFixedSizeButtonStyle?` | `icon: Painter?`, `style: StreamButtonStyle?`, `size` |
| `LeaveCallAction` | `style: StreamFixedSizeButtonStyle?` | `icon: Painter?`, `style: StreamButtonStyle?`, `size` |
| `FlipCameraAction` | `color`, `iconTint` | `style`, `size` |
| `ChatDialogAction` | `icon: ImageVector?`, `bgColor`, `iconTint`, `badgeColor` | `icon: Painter?`, `style`, `size`. The callback type changed, see §7.4 |

`ScreenShareToggleAction` callback type changed from `(ClosedCaptionsAction) -> Unit` to `(ToggleScreenShare) -> Unit`. See §7.4.

### §5.3 Avatars

| v1 | v2 |
|---|---|
| `UserAvatar(..., textStyle: TextStyle = VideoTheme.typography.titleM, textOffset: DpOffset, ...)` | `UserAvatar(..., textStyle: TextStyle? = null, ...)`. `textOffset` is removed |
| `UserAvatarBackground(..., textStyle: TextStyle = ..., textOffset: DpOffset)` | `UserAvatarBackground(..., textStyle: TextStyle? = null)`. `textOffset` is removed |

With `textStyle = null`, the initials style is picked from the avatar size (§7.5).

### §5.4 ParticipantAction

`ParticipantAction.icon` changed from `ImageVector` to a drawable resource id (`@DrawableRes Int`).

```kotlin
// v1
ParticipantAction(icon = Icons.Filled.PushPin, label = "Pin", ...)

// v2
ParticipantAction(icon = R.drawable.stream_design_ic_pin_fill, label = "Pin", ...)
```

### §5.5 Removed participants menu and invite flow

These were unreachable in v1 (no SDK screen opened them) and were deprecated in v1.34.0:

| Removed | Replacement |
|---|---|
| `CallParticipantsInfoMenu` | None. Build your own participants list |
| `CallParticipantInfoMode` | None |
| `ParticipantInfoAction`, `InviteUsers`, `ChangeMuteState` | None |

`ParticipantInformation` is NOT removed.

---

## §6 Resources

### §6.1 Icons

The Compose module now uses the design system icon set: `stream_design_ic_*` drawables in `io.getstream.video.android.compose.R`. The full list is in `stream-video-android-ui-compose/src/main/res/drawable/`.

To change an SDK icon, override the `stream_design_ic_*` drawable with the same name in your app. Overrides of the old names no longer change the Compose UI.

**ui-core drawables that the v1 Compose UI used, and what v2 draws instead:**

| v1 (`io.getstream.video.android.ui.common.R`) | v2 (`io.getstream.video.android.compose.R`) | v1 drawable still exists? |
|---|---|---|
| `stream_video_ic_live` | `stream_design_ic_livestream_fill` | No, removed |
| `stream_video_ic_options` | `stream_design_ic_more_vertical_fill` | No, removed |
| `stream_video_ic_play` | `stream_design_ic_play_fill` | No, removed |
| `stream_video_ic_screensharing` | `stream_design_ic_present_mobile_fill` | No, removed |
| `stream_video_ic_selected` | `stream_design_ic_checkmark` | No, removed |
| `stream_video_ic_videocam_off` | `stream_design_ic_video_off_fill` | Yes (used by the XML module) |
| `stream_video_ic_mic_off` | `stream_design_ic_voice_off_fill` | Yes (used by the XML module) |
| `stream_video_ic_arrow_back` | `stream_design_ic_arrow_left` | Yes (used by the XML module) |

**Removed ui-core drawables that no v1 SDK component used:** `stream_video_ic_close` (use `stream_design_ic_xmark`), `stream_video_ic_join_call`, `stream_video_ic_leave`, `stream_video_ic_message`, `stream_video_ic_reaction`. If the app references them, copy the drawable into the app or pick a `stream_design_ic_*` icon.

**Material icons used by v1 SDK components and their v2 replacements** (useful when you copied an SDK component):

| v1 | v2 |
|---|---|
| `Icons.Default.Mic` / `MicOff` | `stream_design_ic_voice_fill` / `stream_design_ic_voice_off_fill` |
| `Icons.Default.Videocam` / `VideocamOff` | `stream_design_ic_video_fill` / `stream_design_ic_video_off_fill` |
| `Icons.Default.Call` | `stream_design_ic_phone_fill` |
| `Icons.Default.CallEnd` | `stream_design_ic_phone_down_fill` |
| `Icons.Default.FlipCameraIos` | `stream_design_ic_camera_flip_fill` |
| `Icons.Default.ClosedCaption` / `ClosedCaptionOff` | `stream_design_ic_caption_fill` |
| `Icons.Default.QuestionAnswer` | `stream_design_ic_message_bubbles_fill` |
| `Icons.Default.ExitToApp` | `stream_design_ic_leave` |
| `Icons.Default.Close`, `Icons.Filled.Close` | `stream_design_ic_xmark` |
| `Icons.Filled.PushPin` / `Icons.Outlined.PushPin` | `stream_design_ic_pin_fill` / `stream_design_ic_pin` |
| `Icons.Default.MoreVert` | `stream_design_ic_more_vertical_fill` |
| `Icons.Outlined.MoreHoriz` | `stream_design_ic_more_horizontal` |
| `Icons.Default.SignalWifiBad` | `stream_design_ic_exclamation_triangle_fill` |
| `Icons.Default.GroupAdd` | `stream_design_ic_user_add_fill` |

> After replacing, check that each `stream_design_ic_*` name exists in the SDK drawable folder. Names are not 1:1 with v1.

### §6.2 Removed ui-core resources

v2 removes 10 drawables (§6.1), 88 colors, 87 dimens, 18 strings and 1 plural from `stream-video-android-ui-core`. An app that **overrides** one of these by name (same name in its own `res/values`) keeps compiling, but the override does nothing. An app that **references** one directly (`R.color.x`, `@dimen/x`) fails to compile. Delete the overrides and replace the references with your own resources.

**Strings:** `stream_video_call_controls_chat_dialog`, `stream_video_call_controls_reaction`, `stream_video_call_participants_info_add_participants`, `stream_video_call_participants_info_invite`, `stream_video_call_participants_info_options_invite`, `stream_video_call_participants_info_options_mute`, `stream_video_call_participants_info_options_unmute`, `stream_video_call_participants_menu_content_description`, `stream_video_call_rendering_failed`, `stream_video_change_orientation`, `stream_video_invite_users_accept`, `stream_video_invite_users_cancel`, `stream_video_invite_users_message`, `stream_video_invite_users_title`, `stream_video_permissions_cancel`, `stream_video_permissions_message`, `stream_video_permissions_settings`, `stream_video_toggle_fullscreen`.

**Plurals:** `stream_video_call_participants_info_number_of_participants`.

**Colors and dimens:** the full lists are in the `removedResources` block of §8.

**Resources that still exist but no longer theme the Compose UI:** v1 `StreamColors.defaultColors()` and `StreamDimens.defaultDimens()` read `stream_video_*` color and dimen resources, so apps could theme Compose by overriding them. v2 reads nothing from resources. The remaining `stream_video_*` colors and dimens in `ui-core` are used by the XML module only. Move Compose theming to `VideoTheme(colors = ...)` (§3.2).

---

## §7 Behavior Changes

No compile error, but a different result at runtime.

### §7.1 Light theme

v1 `VideoTheme` ignored `isInDarkMode` and always drew the dark palette. v2 uses it: with the default `isSystemInDarkTheme()`, the UI is light when the device is in light mode.

To keep the v1 look (always dark):

```kotlin
VideoTheme(isInDarkMode = true) { ... }
```

### §7.2 New default palette

The v2 palette comes from the Stream design system. In the light palette, `accentPrimary` is `#005FFF`, the same as v1 `brandPrimary`. The dark palette uses a lighter blue (`#4586FF`), so an app that forces `isInDarkMode = true` also sees a different accent. The other colors are new. Custom colors copied from v1 `StreamColors` will not match the new SDK surfaces. Map them with §4.1.

### §7.3 Text color

v1 `StreamTypography` styles carried a color (for example `bodyM` was `baseQuinary`). v2 `StreamDesign.Typography` styles have no color. `VideoTheme` provides `LocalContentColor = colors.textPrimary`, and SDK components set their own content color.

If your `Text(style = VideoTheme.typography.x)` relied on the style color, pass `color = VideoTheme.colors.textSecondary` (or the color you need) explicitly.

### §7.4 Call action events

| Composable | v1 event | v2 event |
|---|---|---|
| `ChatDialogAction` | `CancelCall` | `ChatDialog` |
| `ScreenShareToggleAction` | `ClosedCaptionsAction(isEnabled)` | `ToggleScreenShare(isEnabled)` (new in core) |

A custom `onCallAction` handler that reacted to `CancelCall` from the chat button, or to `ClosedCaptionsAction` from the screen share button, must switch to the new events. The default handler does not handle `ToggleScreenShare`: starting a screen share needs the app's `MediaProjection` consent intent, so the app must handle it.

### §7.5 Avatar initials size

v1 drew the initials with `titleM` and shrank them when they did not fit. v2 picks the style from the avatar size: `metadataEmphasis` under 24dp, `captionEmphasis` under 40dp, `bodyEmphasis` under 48dp, `headingMedium` under 56dp. From 56dp up, the initials are 40% of the avatar size, capped at 48dp. Pass `textStyle` to `UserAvatar` to keep a fixed style.

### §7.6 Additive changes (no action needed)

- `VideoPermissionsState` has two new members, `isCameraPermissionDenied` and `isMicrophonePermissionDenied`. Both have a default `false` implementation, so custom implementations keep compiling.
- `StreamTokens` is public and every member has an explicit type.
- `StreamButtonSize.ExtraLarge` (64dp) is used by the ringing accept and decline buttons.
- `VideoComponentFactory.UserAvatar(params: UserAvatarParams)` (§3.5).

---

## §8 Machine-Readable Rename Block

`null` means "removed, no direct replacement, see the section in the guide".

```json
{
  "scope": "compose-ui",
  "classRenames": {
    "io.getstream.video.android.compose.theme.StreamColors": "io.getstream.video.android.compose.theme.design.StreamDesign.Colors",
    "io.getstream.video.android.compose.theme.StreamTypography": "io.getstream.video.android.compose.theme.design.StreamDesign.Typography",
    "io.getstream.video.android.compose.theme.StreamDimens": "io.getstream.video.android.compose.theme.design.StreamTokens",
    "io.getstream.video.android.compose.theme.StreamShapes": null,
    "io.getstream.video.android.compose.theme.StreamRippleConfiguration": null,
    "io.getstream.video.android.compose.ui.components.base.styling.CompositeStyleProvider": null,
    "io.getstream.video.android.compose.ui.components.base.styling.StreamButtonStyle": "io.getstream.video.android.compose.ui.components.base.StreamButtonStyle",
    "io.getstream.video.android.compose.ui.components.base.styling.StreamFixedSizeButtonStyle": "io.getstream.video.android.compose.ui.components.base.StreamButtonStyle",
    "io.getstream.video.android.compose.ui.components.base.styling.ButtonStyles": "io.getstream.video.android.compose.ui.components.base.StreamButtonStyleDefaults",
    "io.getstream.video.android.compose.ui.components.base.styling.StyleSize": "io.getstream.video.android.compose.ui.components.base.StreamButtonSize",
    "io.getstream.video.android.compose.ui.components.participants.CallParticipantsInfoMenu": null,
    "io.getstream.video.android.compose.state.ui.internal.CallParticipantInfoMode": null,
    "io.getstream.video.android.compose.state.ui.participants.ParticipantInfoAction": null,
    "io.getstream.video.android.compose.state.ui.participants.InviteUsers": null,
    "io.getstream.video.android.compose.state.ui.participants.ChangeMuteState": null
  },
  "functionRenames": {
    "StreamDialogPositiveNegative": "StreamDialog",
    "StreamOutlinedTextField": "StreamTextField",
    "GenericStreamButton": "StreamButton",
    "StreamDrawableButton": "StreamIconButton",
    "StreamToggleButton": null,
    "StreamIconToggleButton": null,
    "StreamDrawableToggleButton": null,
    "GenericToggleButton": null
  },
  "videoThemeParamRenames": {
    "reactionMapper": "config.reactionMapper",
    "allowUIAutomationTest": "config.allowUIAutomationTest",
    "dimens": null,
    "shapes": null,
    "rippleConfiguration": null,
    "styles": null
  },
  "videoThemeAccessorRenames": {
    "VideoTheme.reactionMapper": "VideoTheme.config.reactionMapper",
    "VideoTheme.dimens": null,
    "VideoTheme.shapes": null,
    "VideoTheme.rippleConfiguration": null,
    "VideoTheme.styles": null
  },
  "colorRenames": {
    "brandPrimary": "accentPrimary",
    "brandGreen": "accentSuccess",
    "brandYellow": "accentWarning",
    "brandRed": "accentError",
    "baseSecondary": "textSecondary",
    "baseTertiary": "textTertiary",
    "baseQuaternary": "textTertiary",
    "baseSenary": "borderCoreDefault",
    "baseSheetPrimary": "backgroundCoreApp",
    "baseSheetSecondary": "backgroundCoreElevation1",
    "baseSheetTertiary": "backgroundCoreSurfaceDefault",
    "baseSheetQuarternary": "backgroundCoreOverlayDarkStrong",
    "buttonBrandDefault": "accentPrimary",
    "iconDefault": "textPrimary",
    "alertSuccess": "accentSuccess",
    "alertCaution": "accentWarning",
    "alertWarning": "accentError"
  },
  "colorContextDependent": {
    "basePrimary": { "default": "textPrimary", "overVideoOrDarkOverlay": "textOnAccent" },
    "baseQuinary": { "text": "textTertiary", "border": "borderCoreDefault" }
  },
  "colorClosestMatches": {
    "brandPrimaryLt": "brand.s400",
    "brandPrimaryDk": "brand.s700",
    "brandSecondary": "backgroundCoreSurfaceDefault",
    "brandSecondaryTransparent": "backgroundUtilityDisabled",
    "buttonPrimaryDefault": "backgroundCoreSurfaceDefault",
    "buttonPrimaryDisabled": "backgroundUtilityDisabled",
    "buttonBrandDisabled": "backgroundUtilityDisabled",
    "buttonAlertDefault": "accentError",
    "buttonAlertDisabled": "backgroundUtilityDisabled",
    "iconPressed": "textSecondary",
    "iconActive": "accentPrimary",
    "iconAlert": "accentError",
    "iconDisabled": "textDisabled"
  },
  "colorsWithoutReplacement": [
    "brandCyan", "brandRedLt", "brandRedDk", "brandMaroon", "brandViolet",
    "buttonPrimaryPressed", "buttonBrandPressed", "buttonAlertPressed"
  ],
  "typographyRenames": {
    "titleL": "headingLarge",
    "titleM": "headingLarge",
    "titleS": "headingLarge",
    "titleXs": "headingExtraSmall",
    "subtitleL": "headingLarge",
    "subtitleM": "headingSmall",
    "subtitleS": "bodyDefault",
    "bodyL": "bodyDefault",
    "bodyM": "bodyDefault",
    "bodyS": "captionDefault",
    "labelL": "headingLarge",
    "labelM": "bodyEmphasis",
    "labelS": "captionEmphasis",
    "labelXS": "metadataEmphasis"
  },
  "dimensToStreamTokens": {
    "genericMax": null,
    "generic3xl": "size80",
    "genericXxl": "size48",
    "genericXl": "size32",
    "genericL": "size24",
    "genericM": "size16",
    "genericS": "size8",
    "genericXs": "size4",
    "genericXXs": "size2",
    "roundnessXl": "radius4xl",
    "roundnessL": "radius3xl",
    "roundnessM": "radiusXl",
    "roundnessS": "radiusMd",
    "spacingXl": "spacing2xl",
    "spacingL": "spacingXl",
    "spacingM": "spacingMd",
    "spacingS": "spacingXs",
    "spacingXs": "spacing2xs",
    "spacingXXs": "spacing3xs",
    "componentHeightL": "size48",
    "componentHeightM": "size32",
    "componentHeightS": "size24",
    "componentPaddingTop": "spacingXs",
    "componentPaddingBottom": "spacingXs",
    "componentPaddingFixed": "spacingXs",
    "componentPaddingStart": "spacingMd",
    "componentPaddingEnd": "spacingMd",
    "textSizeXxl": null,
    "textSizeXl": null,
    "textSizeL": "fontSize2xl",
    "textSizeM": "fontSizeXl",
    "textSizeS": "fontSizeMd",
    "textSizeXs": "fontSizeXs",
    "lineHeightXxl": null,
    "lineHeightXl": null,
    "lineHeightL": "lineHeightRelaxed",
    "lineHeightM": "lineHeightNormal",
    "lineHeightS": "lineHeightTight",
    "lineHeightXs": null
  },
  "shapesToCompose": {
    "circle": "CircleShape",
    "square": "RectangleShape",
    "button": "RoundedCornerShape(StreamTokens.buttonRadiusFull)",
    "input": "RoundedCornerShape(StreamTokens.inputRadiusTextInput)",
    "sheet": "RoundedCornerShape(StreamTokens.radiusXl)",
    "dialog": "RoundedCornerShape(StreamTokens.radius3xl)",
    "container": "RoundedCornerShape(StreamTokens.radius4xl)",
    "indicator": "RoundedCornerShape(StreamTokens.radiusMd)"
  },
  "drawableRenames": {
    "stream_video_ic_close": "stream_design_ic_xmark",
    "stream_video_ic_live": "stream_design_ic_livestream_fill",
    "stream_video_ic_options": "stream_design_ic_more_vertical_fill",
    "stream_video_ic_play": "stream_design_ic_play_fill",
    "stream_video_ic_screensharing": "stream_design_ic_present_mobile_fill",
    "stream_video_ic_selected": "stream_design_ic_checkmark",
    "stream_video_ic_videocam_off": "stream_design_ic_video_off_fill",
    "stream_video_ic_mic_off": "stream_design_ic_voice_off_fill",
    "stream_video_ic_arrow_back": "stream_design_ic_arrow_left",
    "stream_video_ic_leave": null,
    "stream_video_ic_message": null,
    "stream_video_ic_reaction": null,
    "stream_video_ic_join_call": null
  },
  "callActionEventChanges": {
    "ChatDialogAction": { "from": "CancelCall", "to": "ChatDialog" },
    "ScreenShareToggleAction": { "from": "ClosedCaptionsAction", "to": "ToggleScreenShare" }
  },
  "removedResources": {
    "drawable": [
      "stream_video_ic_close", "stream_video_ic_join_call", "stream_video_ic_leave", "stream_video_ic_live",
      "stream_video_ic_message", "stream_video_ic_options", "stream_video_ic_play", "stream_video_ic_reaction",
      "stream_video_ic_screensharing", "stream_video_ic_selected"
    ],
    "string": [
      "stream_video_call_controls_chat_dialog", "stream_video_call_controls_reaction",
      "stream_video_call_participants_info_add_participants", "stream_video_call_participants_info_invite",
      "stream_video_call_participants_info_options_invite", "stream_video_call_participants_info_options_mute",
      "stream_video_call_participants_info_options_unmute", "stream_video_call_participants_menu_content_description",
      "stream_video_call_rendering_failed", "stream_video_change_orientation", "stream_video_invite_users_accept",
      "stream_video_invite_users_cancel", "stream_video_invite_users_message", "stream_video_invite_users_title",
      "stream_video_permissions_cancel", "stream_video_permissions_message", "stream_video_permissions_settings",
      "stream_video_toggle_fullscreen"
    ],
    "plurals": [
      "stream_video_call_participants_info_number_of_participants"
    ],
    "color": [
      "stream_video_action_icon_disabled", "stream_video_action_icon_disabled_background",
      "stream_video_action_icon_disabled_background_dark", "stream_video_action_icon_disabled_dark",
      "stream_video_action_icon_enabled", "stream_video_action_icon_enabled_background",
      "stream_video_action_icon_enabled_background_dark", "stream_video_action_icon_enabled_dark",
      "stream_video_alert_caution", "stream_video_alert_success", "stream_video_alert_warning",
      "stream_video_audio_leave_dark", "stream_video_audio_room_actions", "stream_video_audio_room_actions_dark",
      "stream_video_avatar_border_color", "stream_video_avatar_gradient_blue", "stream_video_avatar_gradient_brown",
      "stream_video_avatar_gradient_green", "stream_video_avatar_gradient_orange", "stream_video_avatar_gradient_red",
      "stream_video_avatar_gradient_yellow", "stream_video_base_primary", "stream_video_base_quaternary",
      "stream_video_base_quinary", "stream_video_base_secondary", "stream_video_base_senary",
      "stream_video_base_sheet_quaternary", "stream_video_base_sheet_quinary", "stream_video_base_sheet_secondary",
      "stream_video_base_sheet_tertiary", "stream_video_base_tertiary", "stream_video_borders",
      "stream_video_borders_dark", "stream_video_brand_maroon", "stream_video_brand_primary_dk",
      "stream_video_brand_red_dk", "stream_video_brand_secondary", "stream_video_brand_secondary_transparent",
      "stream_video_button_alert_default", "stream_video_button_alert_disabled", "stream_video_button_alert_pressed",
      "stream_video_button_brand_default", "stream_video_button_brand_disabled", "stream_video_button_brand_pressed",
      "stream_video_button_primary_default", "stream_video_button_primary_disabled",
      "stream_video_button_primary_pressed", "stream_video_call_description", "stream_video_call_description_dark",
      "stream_video_call_gradient_end", "stream_video_call_gradient_start", "stream_video_connection_indicator_good",
      "stream_video_connection_indicator_great", "stream_video_connection_indicator_poor",
      "stream_video_connection_quality_background", "stream_video_connection_quality_background_dark",
      "stream_video_connection_quality_bar_background", "stream_video_deactivated_volume_indicator",
      "stream_video_deactivated_volume_indicator_dark", "stream_video_focused_border_color", "stream_video_highlight",
      "stream_video_highlight_dark", "stream_video_icon_active", "stream_video_icon_alert", "stream_video_icon_default",
      "stream_video_icon_disabled", "stream_video_icon_pressed", "stream_video_input_background",
      "stream_video_input_background_dark", "stream_video_link_background", "stream_video_link_background_dark",
      "stream_video_live_indicator", "stream_video_live_indicator_dark", "stream_video_lobby_background",
      "stream_video_lobby_background_dark", "stream_video_overlay_dark", "stream_video_overlay_dark_dark",
      "stream_video_participant_container_background", "stream_video_participant_label_background",
      "stream_video_participant_label_background_dark", "stream_video_primary_accent",
      "stream_video_primary_accent_dark", "stream_video_screen_sharing_tooltip_background",
      "stream_video_screen_sharing_tooltip_content", "stream_video_text_low_emphasis",
      "stream_video_text_low_emphasis_dark", "stream_video_volume_indicator_background",
      "stream_video_volume_indicator_background_dark"
    ],
    "dimen": [
      "stream_video_IndicatorBackgroundSize", "stream_video_activeSpeakerScreenSharingBoarderWidth",
      "stream_video_audioAvatarBorderPadding", "stream_video_audioAvatarPadding", "stream_video_audioAvatarSize",
      "stream_video_audioContentTopPadding", "stream_video_audioLevelIndicatorBarMaxHeight",
      "stream_video_audioLevelIndicatorBarPadding", "stream_video_audioLevelIndicatorBarSeparatorWidth",
      "stream_video_audioLevelIndicatorBarWidth", "stream_video_audioLevelIndicatorInternalPadding",
      "stream_video_audioMicPadding", "stream_video_audioMicSize", "stream_video_audioRoomAvatarLandscapePadding",
      "stream_video_audioRoomAvatarPortraitPadding", "stream_video_avatarBorderWidth",
      "stream_video_callAppBarRecordingIndicatorSize", "stream_video_callParticipant_container_radius",
      "stream_video_captionLineHeight", "stream_video_captionTextSize", "stream_video_component_height_l",
      "stream_video_component_height_m", "stream_video_component_height_s", "stream_video_component_padding_bottom",
      "stream_video_component_padding_end", "stream_video_component_padding_fixed",
      "stream_video_component_padding_start", "stream_video_component_padding_top",
      "stream_video_connectionIndicatorBarMaxHeight", "stream_video_connectionIndicatorBarSeparatorWidth",
      "stream_video_connectionIndicatorBarWidth", "stream_video_controlActionsBottomPadding",
      "stream_video_controlActionsElevation", "stream_video_footnoteLineHeight", "stream_video_footnoteTextSize",
      "stream_video_generic_3xl", "stream_video_generic_l", "stream_video_generic_m", "stream_video_generic_max",
      "stream_video_generic_s", "stream_video_generic_xl", "stream_video_generic_xs", "stream_video_generic_xxl",
      "stream_video_generic_xxs", "stream_video_headerElevation", "stream_video_line_height_l",
      "stream_video_line_height_m", "stream_video_line_height_s", "stream_video_line_height_xl",
      "stream_video_line_height_xs", "stream_video_line_height_xxl", "stream_video_lobbyControlActionsItemSpaceBy",
      "stream_video_lobbyControlActionsPadding", "stream_video_lobbyVideoHeight",
      "stream_video_microphoneIndicatorPadding", "stream_video_microphoneIndicatorSize",
      "stream_video_participantInfoMenuAppBarHeight", "stream_video_participantInfoMenuOptionsHeight",
      "stream_video_participantsGridPadding", "stream_video_participantsInfoAvatarSize",
      "stream_video_participantsInfoMenuOptionsButtonHeight", "stream_video_reactionSize",
      "stream_video_roundness_l", "stream_video_roundness_m", "stream_video_roundness_s", "stream_video_roundness_xl",
      "stream_video_screenShareParticipantsMargin", "stream_video_screenSharePresenterTooltipHeight",
      "stream_video_screenSharePresenterTooltipPadding", "stream_video_screenShareTooltipIconPadding",
      "stream_video_smallButtonSize", "stream_video_spacing_l", "stream_video_spacing_m", "stream_video_spacing_s",
      "stream_video_spacing_xl", "stream_video_spacing_xs", "stream_video_spacing_xxs", "stream_video_tabBarTextSize",
      "stream_video_text_size_l", "stream_video_text_size_m", "stream_video_text_size_s", "stream_video_text_size_xl",
      "stream_video_text_size_xs", "stream_video_text_size_xxl", "stream_video_title1LineHeight",
      "stream_video_title1TextSize", "stream_video_title3LineHeight"
    ]
  }
}
```

---

## §9 Source Paths to Read

When this guide is not enough, read these files on the `develop-v2` branch.

### Public API surface (ground truth for what exists)

```
stream-video-android-ui-compose/api/stream-video-android-ui-compose.api
stream-video-android-core/api/stream-video-android-core.api
```

### Key v2 classes

```
stream-video-android-ui-compose/src/main/kotlin/io/getstream/video/android/compose/theme/VideoTheme.kt
stream-video-android-ui-compose/src/main/kotlin/io/getstream/video/android/compose/theme/VideoUiConfig.kt
stream-video-android-ui-compose/src/main/kotlin/io/getstream/video/android/compose/theme/design/StreamDesign.kt
stream-video-android-ui-compose/src/main/kotlin/io/getstream/video/android/compose/theme/design/StreamTokens.kt
stream-video-android-ui-compose/src/main/kotlin/io/getstream/video/android/compose/theme/VideoComponentFactory.kt
stream-video-android-ui-compose/src/main/kotlin/io/getstream/video/android/compose/ui/components/base/StreamButton.kt
stream-video-android-ui-compose/src/main/kotlin/io/getstream/video/android/compose/ui/components/base/StreamButtonStyle.kt
stream-video-android-ui-compose/src/main/kotlin/io/getstream/video/android/compose/ui/components/base/StreamDialog.kt
stream-video-android-ui-compose/src/main/kotlin/io/getstream/video/android/compose/ui/components/base/StreamTextField.kt
stream-video-android-ui-compose/src/main/kotlin/io/getstream/video/android/compose/ui/components/call/controls/actions/
stream-video-android-ui-compose/ICONS.md
stream-video-android-ui-compose/src/main/res/drawable/
```

### v2 sample app (reference implementation)

```
demo-app/src/main/kotlin/io/getstream/video/android/ui/call/CallScreen.kt
```

---

## §10 Verification

After applying all changes, run these checks. ALL must pass.

### Step 1: No v1 symbols remain

```bash
# Must return 0 matches each
rg "StreamColors\b|StreamDimens\b|StreamTypography\b|StreamShapes\b|StreamRippleConfiguration\b" --type kotlin
rg "VideoTheme\.(dimens|shapes|styles|reactionMapper|rippleConfiguration)\b" --type kotlin
rg "components\.base\.styling\." --type kotlin
rg "StreamFixedSizeButtonStyle|CompositeStyleProvider|ButtonStyles\b|StyleSize\b" --type kotlin
rg "StreamDialogPositiveNegative|StreamOutlinedTextField|GenericStreamButton|StreamDrawableButton|StreamToggleButton|StreamIconToggleButton|StreamDrawableToggleButton|GenericToggleButton" --type kotlin
rg "CallParticipantsInfoMenu|CallParticipantInfoMode|ParticipantInfoAction\b|InviteUsers\b|ChangeMuteState\b" --type kotlin
rg "stream_video_ic_(close|join_call|leave|live|message|options|play|reaction|screensharing|selected)\b" --type kotlin --type xml
rg "textOffset\s*=" --type kotlin
```

### Step 2: No v1 color names on VideoTheme.colors

```bash
# Must return 0 matches
rg "VideoTheme\.colors\.(brand\w+|base\w+|button\w+|icon\w+|alert\w+)\b" --type kotlin
```

### Step 3: Build

```bash
./gradlew assembleDebug
```

Must compile with 0 errors. Warnings are fine.

### Step 4: Smoke test (manual)

1. Launch the app on an emulator in **light mode** and in **dark mode** (§7.1)
2. Open the lobby: camera preview, microphone and camera toggles work
3. Join a call: the controls bar shows, and the toggles change state
4. Open a custom chat or screen share button if the app has one: the new events arrive (§7.4)
5. Leave the call
6. Receive a ringing call: accept and decline buttons work

If a step fails, read the §9 source files and fix it.
