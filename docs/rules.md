# Project Rules

These rules define how code should be written in this repository.

---

## G1 — Control Time / Environment

Do not access system time directly.

- ❌ Avoid: `OffsetDateTime.now()`, `Instant.now()`
- ✅ Use: injected time source (e.g., `Clock`, `TimeProvider`)

---


## Usage

- Follow these rules when writing or modifying code
- Lint/CI may enforce some rules (e.g., G1)
- If a rule needs to change, update this file first, then update checks