# Icon System

This document describes the icon naming conventions, placement rules, and update workflow for the design system icons used by the Compose Video SDK.

## Source

The icons come from the shared Stream design system. The SVG source lives in the
[design-system-tokens](https://github.com/GetStream/design-system-tokens) repository under
`assets/icons/<namespace>/line/`. The namespaces tell shared icons from product icons:

| Namespace | Used by the Video SDK |
|---|---|
| `core` | Yes, the full set |
| `video` | Yes, the full set |
| `chat` | Only `message-bubbles-fill` (the in-call chat action) |

## Naming Convention

Icons use the `stream_design_ic_` prefix with the **Figma component name** converted from kebab-case to snake_case:

```
Figma: arrow-down-circle  ->  stream_design_ic_arrow_down_circle.xml
Figma: phone-down-fill    ->  stream_design_ic_phone_down_fill.xml
```

The prefix is the same one the Chat SDK uses. Both SDKs ship the same design system art under the same
names, so an app that includes both gets one copy after resource merging. The plan is to move the shared
set into a common UI module later.

### Prefixes by module

| Module | Prefix | Purpose |
|---|---|---|
| `stream-video-android-ui-compose` | `stream_design_ic_*` | Design system icons (Compose SDK) |
| `stream-video-android-ui-core` | `stream_video_ic_*` | Legacy icons still read by the XML SDK and by the core notifications |

## Icon Variants

The design system provides two categories:

- **Line**: stroke-based SVGs. Stroke width can be adjusted programmatically.
- **Flat**: flattened vector shapes with baked-in strokes, exported at 12, 16, 20 and 32. Used mainly by Flutter.

**Android uses the Line variant** for all icons.

## Size Convention

All `stream_design_ic_*` icons use **20dp** as the intrinsic size with a 20x20 viewport. When a different
size is needed at the call site, set it explicitly with `Modifier.size()`.

## Colors

Icons are monochrome and use the literal color `#FF000000`. The color is applied at the call site with
`Icon(tint = VideoTheme.colors...)`. Do not use `?attr` or `@color` references inside the drawables.

Directional icons (arrows, chevrons, leave, speaker, camera) carry `android:autoMirrored="true"` so they
flip in right-to-left layouts.

## How to Update Icons

1. Fetch the `assets/icons/core/line` and `assets/icons/video/line` folders from the design-system-tokens repository (plus `chat/line/message-bubbles-fill.svg`).
2. Replace the named colors in the SVGs: `"black"` with `"#000000"` and `"white"` with `"#FFFFFF"`. The converter mishandles named colors.
3. Convert the SVGs to Android vector drawables:
   ```bash
   npx svg2vectordrawable@2.9.1 -f <svg folder> -o <output folder>
   ```
4. Add the license header used by the other XML resources in this module.
5. Add `android:autoMirrored="true"` to the directional icons: `arrow_left`, `arrow_right`, `arrow_up_right`, `audio`, `chevron_left`, `chevron_right`, `leave`, `message_bubbles_fill`, `mute`, `sidebar`, `video_fill`, `video_off_fill`.
6. Name each file `stream_design_ic_{figma_name_snake_case}.xml` and place it in `stream-video-android-ui-compose/src/main/res/drawable/`.
7. Record the source commit of the design-system-tokens repository in the pull request description.
