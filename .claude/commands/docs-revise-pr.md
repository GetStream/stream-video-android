# Revise Documentation PR

Apply feedback to an existing docs-content PR.

## Usage

```
/docs:revise-pr [DOCS_PR_NUMBER]
```

Then provide feedback when prompted, or include inline:

```
/docs:revise-pr 959 "Fix the code example in call-content.md - missing import"
```

## Standards Reference

**IMPORTANT:** All revisions must follow the standards document:
@.claude/docs-standards.md

Key sections for revisions:
- §2 Code Blocks - verify code compiles, explicit types
- §3 Writing Style - terminology, no filler phrases
- §12 Examples - good vs bad patterns

---

## Process

### Step 1: Fetch PR and Branch

```bash
PR_NUMBER=$1

# Get PR details including branch name
gh pr view $PR_NUMBER --repo GetStream/docs-content --json headRefName,title,body,state,files

# Clone and checkout the PR branch
rm -rf /tmp/docs-content-sync
mkdir -p /tmp/docs-content-sync
cd /tmp/docs-content-sync
git clone https://github.com/GetStream/docs-content.git .
git checkout [branch-name-from-pr]
```

### Step 2: Get Feedback

If feedback not provided as argument, ask:
gs
```
What changes are needed?

Common feedback types:
- Code fixes: "The code example doesn't compile - StreamVideo should be StreamVideoBuilder"
- Missing content: "Add a note about Android 14 compatibility"
- Style issues: "Remove the filler phrases in the intro paragraph"
- Revert: "Revert the changes to permissions.md"
- Structure: "Move the parameters table before the example"
```

### Step 3: Read Affected Files

Before making changes:
1. Read the files mentioned in feedback
2. Understand current state
3. Identify exact locations to change

### Step 4: Apply Changes

Based on feedback type:

#### Code Fixes

When fixing code examples, verify against standards §2:

```markdown
**Checklist:**
- [ ] Explicit types: `val client: StreamVideo = ...`
- [ ] Compiles against SDK develop branch
- [ ] No internal APIs (check `.api` files)
- [ ] Correct language tag (kotlin, groovy, xml)
- [ ] Imports included for full samples
```

#### Writing Fixes

When fixing prose, verify against standards §3:

```markdown
**Checklist:**
- [ ] No filler phrases (Basically, Simply, Just, Obviously)
- [ ] Direct language ("The SDK provides" not "In terms of the SDK")
- [ ] Correct terminology (customize not custom, participant not user)
- [ ] Active voice for instructions
```

#### Structure Fixes

When restructuring, verify against standards §1 and §7:

```markdown
**Checklist:**
- [ ] Page follows intro → sections → examples pattern
- [ ] Heading hierarchy (## for main, ### for sub)
- [ ] Links use correct format (/video/docs/android/...)
```

#### Adding Content

When adding new content:

**For notes/warnings (§4):**
```markdown
> **Note:** Brief inline note.

<admonition type="caution">
**Important:** Longer warning with details.
</admonition>
```

**For parameters (§2):**
```markdown
| Parameter | Description | Default |
|-----------|-------------|---------|
| `name` | What it does | `value` |
```

**For API levels (§5):**
```markdown
This feature is available on Android 10 (API level 29) and above.
```

### Step 5: Commit and Push

```bash
cd /tmp/docs-content-sync

git add .

# Commit - NO AI attribution (§11)
git commit -m "docs(android): address review feedback

[Summary of changes made]"

git push
```

### Step 6: Comment on PR

```bash
gh pr comment $PR_NUMBER --repo GetStream/docs-content --body "Applied feedback:

- [Change 1]
- [Change 2]

Ready for another look."
```

### Step 7: Report

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 REVISIONS APPLIED
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

PR: GetStream/docs-content#[PR_NUMBER]
Branch: [branch-name]

Feedback received:
"[original feedback]"

Changes made:
- [File 1]: [what changed]
- [File 2]: [what changed]

Quality verified:
✓ Code compiles
✓ Writing style correct
✓ No AI attribution

Status: Pushed, commented on PR

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

PR URL: [link]
```

---

## Multiple Revision Rounds

Run this command as many times as needed. Each run:
1. Fetches latest from the PR branch (gets any manual changes)
2. Applies new feedback
3. Pushes and comments
4. Reports what changed

When satisfied, dev marks PR ready for review manually in GitHub.

---

## Common Feedback Patterns

| Feedback | How to handle |
|----------|---------------|
| "Code doesn't compile" | Read SDK source, fix syntax, verify types |
| "Missing import" | Add import for full samples, or note it's a fragment |
| "Too wordy" | Apply §3 rules, remove filler, use direct language |
| "Add API level" | Use format: "Android X (API level Y)" |
| "Wrong terminology" | Check §3 dictionary, use correct terms |
| "Needs example" | Add code block with explicit types |
| "Revert file X" | `git checkout origin/main -- video/android/path/to/file.md` |
| "Link broken" | Fix to `/video/docs/android/section/page/` format |

---

## Verification Checklist

Before pushing revisions, verify (§13):

- [ ] Changes address the specific feedback
- [ ] No new issues introduced
- [ ] Code still compiles (if code was changed)
- [ ] Writing style consistent with rest of doc
- [ ] Commit message follows format
- [ ] No AI attribution anywhere
