# Sync SDK PR to Documentation

Automatically update docs-content when SDK changes require documentation updates.

## Usage

```
/docs:sync-sdk-pr [PR_NUMBER]
```

## Standards Reference

**IMPORTANT:** Read and follow all rules in the standards document:
@.claude/docs-standards.md

Key sections to reference during this workflow:
- §2 Code Blocks - for code formatting rules
- §3 Writing Style - for terminology and tone
- §9 SDK to Docs Mapping - for finding affected files
- §11 PR Standards - for commit/PR format

---

## Configuration

| Setting | Value |
|---------|-------|
| Docs repo | `https://github.com/GetStream/docs-content.git` |
| Docs path | `video/android/` |
| Sidebar | `_sidebars/[video][android].json` |
| Clone to | `/tmp/docs-content-sync/` |
| Base branch | `main` |

---

## Process

### Step 1: Validate Input

```bash
PR_NUMBER=$1

if [ -z "$PR_NUMBER" ]; then
  echo "Usage: /docs:sync-sdk-pr [PR_NUMBER]"
  exit 1
fi

# Verify PR exists and get details
gh pr view $PR_NUMBER --json number,title,body,state,labels,files
```

Check PR state - warn if not merged (docs should typically sync after merge).

### Step 2: Analyze SDK Changes

```bash
# Get changed files
gh pr diff $PR_NUMBER --name-only

# Get full diff for .api files
gh pr diff $PR_NUMBER -- "*.api" 2>/dev/null || gh pr diff $PR_NUMBER | grep -A50 "\.api"

# Check for deprecation annotations
gh pr diff $PR_NUMBER | grep -i "@Deprecated\|@deprecated"
```

**Classify each change (per §9 of standards):**

| Change Type | Detection | Doc Action |
|-------------|-----------|------------|
| New public class | Lines added to `.api` file | New section or page |
| New parameter | Function signature changed in `.api` | Update examples + add to parameters table |
| Changed signature | Modified line in `.api` file | Update all code examples using it |
| Deprecated API | `@Deprecated` in diff | Add deprecation notice (§8 format) |
| Removed API | Lines removed from `.api` file | Remove from docs entirely |
| Behavior change | PR description mentions it | Update descriptions |
| New feature | PR labeled `pr:new-feature` | Likely needs new section/page |

### Step 3: Clone docs-content

```bash
rm -rf /tmp/docs-content-sync
mkdir -p /tmp/docs-content-sync
cd /tmp/docs-content-sync
git clone --depth 1 https://github.com/GetStream/docs-content.git .
```

### Step 4: Find Affected Doc Files

**Use the mapping table from §9 of standards:**

| SDK Area | Doc Location |
|----------|--------------|
| `StreamVideoBuilder` | `03-guides/01-client-auth.md` |
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
| Telecom | `06-advanced/12-telecom.md` |

**Also search directly:**

```bash
cd /tmp/docs-content-sync

# Search for class/function references
grep -r "ClassName" video/android/ --include="*.md" -l
grep -r "functionName" video/android/ --include="*.md" -l
```

### Step 5: Update Documentation

For each affected file, apply these rules:

#### Code Examples (§2)

- **Explicit types required:** `val client: StreamVideo = ...` not `val client = ...`
- **Full samples** for first example on page, **fragments** for subsequent
- **Verify compilation** - code must work against SDK develop branch
- **No internal APIs** - check `.api` files if unsure

```bash
# Check if API is public
cat stream-video-android-*/api/*.api | grep "ClassName"
```

#### Writing Style (§3)

- **Direct language:** "The SDK provides..." not "In terms of the SDK..."
- **No filler phrases:** Remove "Basically", "Simply", "Just", "Obviously"
- **Correct terminology:** "customize" not "custom", "participant" not "user"

#### Parameters Tables

When adding new parameters, use this format:

```markdown
| Parameter | Description | Default |
|-----------|-------------|---------|
| `paramName` | What it does | `defaultValue` |
```

#### Deprecations (§8)

Use admonition format:

```markdown
<admonition type="caution">

**Deprecated:** `oldMethod()` is deprecated and will be removed in version X.Y.Z. Use `newMethod()` instead.

</admonition>
```

#### Android-Specific (§5)

- Document API level: "Available on Android 10 (API level 29) and above."
- Document permissions if required
- Include version-specific code with `Build.VERSION.SDK_INT` checks

#### New Pages (§7)

If creating a new page:
1. Use correct number prefix (e.g., `14-new-feature.md`)
2. Follow page structure from §1
3. Update sidebar: `_sidebars/[video][android].json`

### Step 6: Create Branch and Commit

```bash
cd /tmp/docs-content-sync

BRANCH_NAME="docs/android-sdk-pr-${PR_NUMBER}"
git checkout -b $BRANCH_NAME

git add video/android/
git add _sidebars/  # if sidebar changed

# Commit - NO AI attribution (§11)
git commit -m "docs(android): update for SDK PR #${PR_NUMBER}

[Brief description of what changed]

Related to GetStream/stream-video-android#${PR_NUMBER}"
```

### Step 7: Push and Create Draft PR

```bash
cd /tmp/docs-content-sync

git push -u origin $BRANCH_NAME

# Create draft PR with template from §11
gh pr create --draft \
  --title "docs(android): update for SDK PR #${PR_NUMBER}" \
  --body "## Summary

[Brief description of doc changes]

## SDK Changes

Related to GetStream/stream-video-android#${PR_NUMBER}

- [Change 1 from SDK PR]
- [Change 2 from SDK PR]

## Doc Updates

- [File 1]: [what changed]
- [File 2]: [what changed]

---

**Status:** Draft - ready for dev review"
```

### Step 8: Report Results

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 DOCS SYNC COMPLETE
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

SDK PR: GetStream/stream-video-android#[PR_NUMBER]
Docs PR: GetStream/docs-content#[NEW_PR_NUMBER]

Changes detected:
- [Type]: [Description]

Files updated:
- video/android/[file1].md - [what changed]
- video/android/[file2].md - [what changed]

Quality checks:
✓ Code examples use explicit types
✓ No internal APIs exposed
✓ Writing style is direct
✓ No AI attribution

Status: Draft PR created

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Next steps:
1. Review the draft PR: [PR_URL]
2. Provide feedback: /docs:revise-pr [DOCS_PR_NUMBER] "feedback"
3. When satisfied, mark PR ready for review
```

---

## Edge Cases

| Situation | Action |
|-----------|--------|
| No doc changes needed | Report "No documentation impact detected" with reasoning |
| New feature, no existing docs | Create new page, update sidebar, link from related pages |
| Major restructure needed | Create GitHub issue instead, flag for manual planning |
| Can't determine impact | List potentially affected files, ask dev to confirm scope |
| Deprecated without replacement | Document deprecation, note removal timeline |
| Breaking change | Add migration guide (§8), update all affected examples |

---

## Verification Before Finishing

Run through checklist from §13:

### Content
- [ ] All code blocks compile against SDK develop
- [ ] Explicit types used (`val x: Type = ...`)
- [ ] No internal APIs exposed
- [ ] Correct language tags (kotlin, groovy, xml, bash)

### Writing
- [ ] Direct writing style (no "Basically", "Simply", etc.)
- [ ] Consistent terminology (see §3 dictionary)
- [ ] API levels documented for version-specific features
- [ ] Required permissions documented

### Structure
- [ ] Page follows standard structure (§1)
- [ ] Proper heading hierarchy (##, ###)
- [ ] Links use correct format (`/video/docs/android/...`)

### PR
- [ ] Commit message: `docs(android): [description]`
- [ ] PR description follows template
- [ ] SDK PR referenced
- [ ] NO "Generated with Claude" or "Co-Authored-By: Claude"

---

## Handling Feedback

After dev reviews the draft PR, use:

```
/docs:revise-pr [DOCS_PR_NUMBER] "feedback here"
```
