# Claude Instructions for Stream Video Android SDK

## Documentation Commands

When SDK changes require documentation updates:

| Command | Purpose |
|---------|---------|
| `/docs:sync-sdk-pr [PR#]` | Create docs PR for SDK changes |
| `/docs:revise-pr [PR#]` | Apply feedback to docs PR |

### Workflow

1. Dev creates SDK PR
2. Dev runs `/docs:sync-sdk-pr 1234`
3. Claude analyzes changes, creates draft docs PR
4. Dev reviews, provides feedback
5. Dev runs `/docs:revise-pr 567 "feedback"` as needed
6. Dev marks ready for review

### Standards

All documentation work must follow:
- `.claude/docs-standards.md` — Quality rules for docs

Key rules:
- Code must compile against develop branch
- No internal APIs (check `.api` files)
- Direct writing style, no filler phrases
- No AI attribution in commits/PRs

## Project Planning

GSD workflow files are in `.planning/`:
- `PROJECT.md` — Project context
- `STANDARDS.md` — Audit standards
- `ROADMAP.md` — Phase structure
- `STATE.md` — Current position

## Repository Structure

```
stream-video-android/          # SDK source
├── stream-video-android-core/
├── stream-video-android-ui-compose/
├── stream-video-android-ui-core/
├── stream-video-android-filters-video/
└── .claude/
    ├── docs-standards.md      # Doc quality rules
    └── commands/
        ├── docs-sync-sdk-pr.md
        └── docs-revise-pr.md
```

## Related Repos

| Repo | Purpose | Path |
|------|---------|------|
| docs-content | Documentation source | Clone fresh to /tmp for edits |
| docs | Parent repo (don't edit) | /Users/aapostol/projects/docs |
