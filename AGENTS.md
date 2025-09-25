# Repository Guidelines

Guidance for AI coding agents (Copilot, Cursor, Aider, Claude, etc.) collaborating on this Android SDK. Humans are welcome, but the tone is optimized for tools.

### Repository purpose
This repository hosts Stream’s Kotlin-based Video SDK for Android. It delivers call state management, WebRTC signaling, and both Compose and XML UI layers so customers can ship 1:1 calls, rooms, and livestreams. Optimize for media reliability, developer ergonomics, and backwards-compatible APIs.

### Tech & toolchain
- Language: Kotlin (JVM toolchain 17). Java is legacy-only.
- UI: Jetpack Compose-first (`stream-video-android-ui-compose`); XML views supported via `stream-video-android-ui-xml`.
- Media: Google WebRTC stack + Stream SFU.
- Build: Gradle Kotlin DSL with custom plugins in `build-logic/`.
- Min/target SDK: follow `gradle.properties` & module `build.gradle.kts`; do not change API levels.
- Distribution: Maven Central (see `stream-video-android-bom`); demo app ships via Play/internal testing.
- CI: GitHub Actions (artifact uploads, lint/tests, publishing).

## Project Structure & Module Organization
- Entry points: `settings.gradle.kts`, root `build.gradle.kts` apply shared conventions.
- Core engine: `stream-video-android-core/src/main/kotlin` (signaling, state, RTC).
- UI layers: `stream-video-android-ui-core` (shared resources), `stream-video-android-ui-compose`, `stream-video-android-ui-xml`.
- UX helpers: `stream-video-android-previewdata` (fixtures/previews), `stream-video-android-filters-video` (effects pipeline).
- Distribution: `stream-video-android-bom` for dependency alignment.
- Samples/tools: `demo-app/`, `tutorials/`, `benchmark/`, `fastlane/`, `scripts/`, `metrics/`.
- Tests mirror production structure under `src/test` (unit/JVM) and `src/androidTest` (instrumented).

## Build, Test, and Development Commands
- Full build: `./gradlew assemble`.
- Quality gate: `./gradlew check` (hooks unit tests, lint, Spotless).
- JVM tests only: `./gradlew test`.
- Instrumented tests: `./gradlew connectedCheck` (ensure an emulator/device).
- Formatting: `./gradlew spotlessApply` (CI enforces `spotlessCheck`).
- Demo app: `./gradlew :demo-app:installDebug` (device/emulator attached).
- OpenAPI regeneration: `./generate-openapi.sh` (requires env setup from `env.properties.sample`).

### Public API & release management
- Modules shipped to Maven must retain binary compatibility; prefer additive changes.
- Use semantic versioning; coordinate with release owners before modifying artifacts.

### Performance & quality
• Keep critical RTC paths off the main thread; prefer coroutines with structured scopes.
• Avoid excessive recomposition in Compose; hoist state and mark stable types where possible.
• Monitor logging verbosity; rely on `StreamVideoImpl.developmentMode` for guardrails.
• Reuse preview/test data objects instead of ad-hoc builders to keep allocations predictable.

## Coding Style & Naming Conventions
- Kotlin style with 4-space indentation; avoid trailing whitespace and wildcard imports.
- Types/Composables: PascalCase (`StreamCallActivity`, `ParticipantGrid`). Functions/vals: camelCase. Constants: UPPER_SNAKE only when truly constant.
- Prefer explicit visibility modifiers; limit `internal` leakage across modules.
- Compose: follow unidirectional data flow; pass modifiers last; keep preview functions in `previewdata` or `debug` source sets.
- Run Spotless before pushing; custom license headers live in `spotless/`.
- Limit new third-party dependencies; add through `gradle/libs.versions.toml` with reviewer buy-in.

## Testing Guidelines
- Frameworks: JUnit4/5 mix, Truth, MockK, Robolectric for JVM UI tests; Espresso/Compose UI tests under `androidTest`.
- Base classes: `TestBase` for fast unit tests; `IntegrationTestBase` for end-to-end call flows; use provided helpers for coroutines and event assertions.
- Use descriptive backtick test names (``fun `joining a call publishes participant tracks`()``).
- Keep fixtures in `stream-video-android-previewdata`; avoid duplicating builder logic.
- Always run `./gradlew test` for affected modules; add `connectedCheck` when touching Android-specific code paths or media integration.
- For Compose UI, add `@get:Rule val composeRule = createComposeRule()` and snapshot assertions where feasible.

## Comments & documentation
- Use KDoc (`/** ... */`) for public APIs and complex subsystems; link to Stream docs when relevant.
- Group large files with `// region` judiciously; keep commentary purposeful (no "set value" noise).
- Update `development.md` or tutorials whenever workflow changes impact contributors.

## Commit & Pull Request Guidelines
- Commits: concise, imperative, optionally scoped (`feat(core): add call recording API`). Squash noisy WIP locally.
- Before PR: `./gradlew check`, relevant instrumentation tests, `spotlessApply`. Attach emulator logs or recordings for UI changes.
- PR description: summary, validation checklist, linked issues/specs, screenshots for UI updates, release notes when needed.
- Ensure GitHub Actions stays green; address flaky tests rather than retrying blindly.

## Security & configuration tips
- Never commit credentials; copy `.env.properties.sample` to `.env.properties` locally and keep it gitignored.
- Sanitize logs; avoid dumping JWTs, ICE tokens, or call IDs in verbose logs.
- Respect Play Store privacy requirements (camera/mic usage strings, foreground service justifications).

### Media & permissions checklist
- Handle runtime camera/microphone permissions gracefully; surface rationale via Compose components when denied.
- Pause/resume capture on lifecycle changes; ensure background audio routing is intentional.
- Validate orientation, aspect ratio, and dynascale handling for both portrait/landscape phones and tablets.
- Confirm device compatibility across API 24+; test low-end hardware scenarios when touching rendering or encoding paths.

### Quick checklist for agents
- [ ] Understand which module you’re editing and run its tests.
- [ ] Maintain explicit API boundaries; prefer additive changes.
- [ ] Ensure cleanup/teardown paths handle cancellation and failure (important for sockets, queues, retries).
- [ ] Keep concurrency deterministic—use structured coroutines and avoid global scope.
- [ ] Run Spotless (formatting) before finishing.
- [ ] Add tests for new APIs or behaviour changes.
- [ ] Coordinate with the release owner before modifying versioning metadata.
- [ ] Document new APIs and significant changes in `README.md` or module-specific docs.
- [ ] Sanitise logs to avoid leaking sensitive information.
