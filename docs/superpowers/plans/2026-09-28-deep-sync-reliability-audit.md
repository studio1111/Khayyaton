# Khayyaton Deep Sync Reliability Audit Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make account-scoped Firestore synchronization deterministic, durable, observable, and safe against races so locally saved records reliably reach Firebase.

**Architecture:** Room remains the UI source of truth. A new account first restores cloud state, marks the account cloud-ready, then uploads durable local changes. A cloud-ready account uploads first and reconciles afterward. WorkManager keeps one durable sync chain, and the worker retries when a new local change arrives during an active run.

**Tech Stack:** Kotlin, Room, Firebase Auth/Firestore, WorkManager 2.12.0, JUnit/Robolectric.

**Spec:** `docs/superpowers/specs/2026-09-28-offline-first-data-layer-design.md`

## Global Constraints

- Firebase remains the backend.
- Room remains the only UI data source.
- No automatic workshop creation.
- Deletions are durable tombstones.
- New account data must not be uploaded before initial cloud restore.
- User-visible errors remain Persian.
- Final readiness requires fresh test/build evidence.

## Review Focus

1. New-account local records must survive initial cloud restore and then upload.
2. A local write created while a sync is already running must not be dropped by unique-work scheduling.
3. A failed Firestore write must expose a useful Persian error.
4. Release/App Check behavior must not silently block Firestore writes for the target Bazaar distribution channel.
5. Legacy/malformed cloud records must not invent workshop ID 1 or hide valid cards.

### Task 1: Lock synchronization invariants with tests
- [x] Test restore-before-upload for a not-ready account and upload-before-reconcile for a ready account.
- [x] Test that account switching clears cloud-ready state.
- [ ] Run the focused tests and capture RED failures before production changes.

### Task 2: Implement cloud-ready account synchronization
- [x] Add account-scoped cloud-ready state.
- [x] Switch sync order based on that state.
- [x] Mark an account cloud-ready only after a successful initial restore.
- [x] Surface actual Persian sync failures from the Worker.

### Task 3: Prevent WorkManager from dropping writes
- [x] Keep one unique sync worker per account and retain network constraints.
- [x] Detect new pending writes before declaring a sync successful so the same worker retries and drains them.

### Task 4: Remove remaining workshop identity fallbacks
- [x] Remove runtime fallbacks that fabricate workshop ID 1 for malformed/legacy records.
- [x] Resolve by real sync identity where possible.
- [ ] Reject or quarantine records that cannot be mapped safely.

### Task 5: Deep repository/static audit
- [ ] Re-scan auth, Firestore rules, sync IDs, timestamps, account switching, tombstones, subscriptions, trial duration, theme contrast, and release configuration.
- [ ] Re-check active Firebase Storage and legacy queue references.
- [ ] Add regression tests for every newly verified defect.

### Task 6: Verification
- [ ] Run the full unit-test command.
- [ ] Build the debug APK.
- [ ] Inspect the GitHub Actions result.
- [ ] For every failure, return to root-cause analysis and repeat the affected cycle.
