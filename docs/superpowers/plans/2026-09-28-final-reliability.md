# Khayyaton Final Reliability and Offline-First Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** Apply the approved final Khayyaton architecture, remove the unused file-storage subsystem, eliminate known account/workshop/sync-state bugs, and produce verified debug/release artifacts from the final commit.

**Architecture:** Room remains the only UI data source. Firestore is reconciled through FirebaseService, SyncManager, and CloudSyncWorker using UUID sync IDs, tombstones, and deterministic timestamp conflict rules. WorkManager is the execution authority for queued sync, while GitHub Actions is the build/release authority.

**Tech Stack:** Kotlin, Jetpack Compose, Room, Kotlin Coroutines/Flow, Firebase Auth/Firestore, WorkManager, Gradle, GitHub Actions.

**Spec:** `docs/superpowers/specs/2026-09-28-offline-first-data-layer-design.md`

## Global Constraints

- Khayyaton stores only textual/numeric structured data; no active file-storage feature is allowed.
- Firebase remains the backend and the implementation must remain compatible with the free Firebase plan.
- Room is the UI source of truth.
- No runtime path may auto-create a placeholder/default workshop.
- The four mandatory unit rules are always present and not user-deletable: 3->2, 2->1.5, 1->1, 0.5->0.5.
- Trial duration is exactly 7 days.
- New records must inherit the active workshop at the ViewModel/repository boundary.
- User-facing error/warning text remains Persian.
- A green workflow must represent a genuinely completed build/release path, not a best-effort publish.

## Review Focus

1. A new preset in workshop N must never be stored under workshop 1.
   Test: create a new preset while active workshop ID is not 1 and assert its stored workshopId/syncId match that workshop.

2. Switching Firebase users must never expose the previous user's Room rows to the new account.
   Test: populate user A local data, switch to user B, assert A's domain rows are cleared before B data is surfaced.

3. A deleted workshop/order/payment/preset must not reappear from a stale cloud snapshot.
   Test: write tombstone, feed stale cloud record, assert Room does not resurrect it.

4. Pending-sync state must not be non-zero solely because legacy file queues contain rows.
   Test: populate only legacy upload/pending-delete rows and assert active pending count remains zero.

5. Production release publication failure must make the workflow fail.
   Test: validate workflow definition has no `continue-on-error: true` on release publication and that the publish step is not an always-success wrapper.

---

### Task 1: Lock regression tests for workshop assignment and no-workshop state

**Files:**
- Modify: `app/src/test/java/com/example/data/OfflineFirstDataLayerTest.kt` or the closest existing data-integrity test file discovered during implementation.
- Modify: `app/src/test/java/com/example/ui/*` only if an existing ViewModel test target exists.
- Create: a focused ViewModel/data test if no suitable existing file exists.

**Interfaces:**
- Consumes: current `KhayyatonViewModel.savePreset`, workshop flows, repository save methods.
- Produces: regression coverage that fails on the hard-coded workshop-id bug and verifies zero means “no workshop”.

- [ ] **Step 1: Write failing test `new_preset_uses_active_workshop_id`** with an active workshop ID different from 1 and assert the saved preset belongs to that workshop.
- [ ] **Step 2: Run the focused test and verify it fails for the current implementation because `ModelPresetsDialog`/preset save path can retain workshop ID 1.
- [ ] **Step 3: Write failing test `no_workshop_keeps_active_workshop_zero` and assert an empty workshop list never selects ID 1.
- [ ] **Step 4: Run the focused tests and verify the failure is caused by current fallback behavior.
- [ ] **Step 5: Commit the regression tests only.

---

### Task 2: Fix workshop-safe preset creation and cleanup fallback identities

**Files:**
- Modify: `app/src/main/java/com/example/ui/dialogs/ModelPresetsDialog.kt`
- Modify: `app/src/main/java/com/example/ui/KhayyatonViewModel.kt`
- Modify: `app/src/main/java/com/example/data/WorkshopRepository.kt` if repository-level validation is required.
- Modify: `app/src/main/java/com/example/ui/components/SidebarDrawer.kt` and other exact UI files found during root-cause scan where a missing workshop is rendered as a real “کارگاه اصلی”.
- Modify: `app/src/test/...` focused tests from Task 1.

**Interfaces:**
- Consumes: active workshop ID Flow/value and workshop-scoped preset DAO operations.
- Produces: `savePreset` and new preset creation always use the actual active workshop; duplicate-name checks are scoped by workshop.

- [ ] **Step 1: Change new preset construction to omit any hard-coded real workshop ID.
- [ ] **Step 2: Implement the minimal `savePreset` behavior that forces `workshopId = activeWorkshopId` for `id == 0L` and rejects the save when no active workshop exists.
- [ ] **Step 3: Scope duplicate preset-name detection to the current workshop.
- [ ] **Step 4: Replace presentation-only “کارگاه اصلی” fallbacks used when no workshop exists with a neutral no-workshop label/state.
- [ ] **Step 5: Run the focused regression tests and then the relevant existing unit tests.
- [ ] **Step 6: Commit.

---

### Task 3: Make account switching single-authority and race-safe

**Files:**
- Modify: `app/src/main/java/com/example/ui/KhayyatonViewModel.kt`
- Modify: `app/src/main/java/com/example/data/sync/SyncManager.kt`
- Modify: `app/src/main/java/com/example/data/WorkshopRepository.kt`
- Modify: `app/src/main/java/com/example/data/sync/CloudSyncWorker.kt`
- Modify: `app/src/test/java/com/example/data/OfflineFirstDataLayerTest.kt` and/or focused account-isolation test.

**Interfaces:**
- Consumes: `repository.getLocalAccountUid()`, `clearAccountLocalState()`, `saveLocalAccountUid()`, `isCloudSyncReady()`, and FirebaseAuth state.
- Produces: one idempotent account-isolation transition and no stale-account UI exposure.

- [ ] **Step 1: Write failing regression test `account_switch_clears_previous_local_state_before_new_state`.
- [ ] **Step 2: Run the focused test and verify it exposes the current asynchronous ordering/race.
- [ ] **Step 3: Move account isolation into one explicit repository/ViewModel transition and make SyncManager treat the same transition as idempotent rather than independently repopulating stale state.
- [ ] **Step 4: Ensure currentUser/user-visible state is not committed as the new active context until stale local state is cleared.
- [ ] **Step 5: Keep UID-specific WorkManager cancellation and cloud-ready reset tied to the same transition.
- [ ] **Step 6: Run the focused test plus full unit suite.
- [ ] **Step 7: Commit.

---

### Task 4: Remove active Firebase Storage and file synchronization

**Files:**
- Delete: `app/src/main/java/com/example/data/sync/StorageSyncManager.kt`
- Modify: `app/src/main/java/com/example/data/firebase/FirebaseService.kt`
- Modify: `app/src/main/java/com/example/data/sync/SyncManager.kt`
- Modify: `app/src/main/java/com/example/data/sync/SyncEntities.kt`
- Modify: `app/src/main/java/com/example/data/sync/SyncDaos.kt`
- Modify: `app/src/main/java/com/example/data/WorkshopRepository.kt`
- Modify: `app/src/main/java/com/example/data/AppDatabase.kt`
- Modify: `app/build.gradle.kts`
- Modify: `gradle/libs.versions.toml`
- Modify: any models/DAOs/importers/exporters that actively write/read `fileUrl` or `storagePath`.
- Add/modify migration test only if the selected compatibility strategy needs one.

**Interfaces:**
- Consumes: existing structured-data sync APIs.
- Produces: Firestore-only structured-data synchronization with no active Storage API, bucket setup, upload/download queue, or file URLs.

- [ ] **Step 1: Add a failing build/static test or repository grep check that asserts there is no active `FirebaseStorage`, `putFile`, storage bucket configuration, or dependency after the task.
- [ ] **Step 2: Remove the Storage dependency/version entry and all active FirebaseService Storage configuration/imports.
- [ ] **Step 3: Remove active file upload/download mapping from FirebaseService and SyncManager.
- [ ] **Step 4: Delete StorageSyncManager and active callers.
- [ ] **Step 5: Keep legacy Room schema only when required for safe backward compatibility; if retained, make it inert, clear queues where safe, and ensure it never contributes to pending-sync state.
- [ ] **Step 6: Remove active model/UI paths that present file URLs or storage operations.
- [ ] **Step 7: Run focused compilation and synchronization tests.
- [ ] **Step 8: Run a repository-wide static search for Storage/file-sync symbols and verify no active references remain.
- [ ] **Step 9: Commit.

---

### Task 5: Correct sync pending state and WorkManager status reporting

**Files:**
- Modify: `app/src/main/java/com/example/data/sync/SyncManager.kt`
- Modify: `app/src/main/java/com/example/data/WorkshopRepository.kt`
- Modify: `app/src/main/java/com/example/ui/KhayyatonViewModel.kt`
- Modify: `app/src/main/java/com/example/data/sync/SyncWorkScheduler.kt` only if status observation requires it.
- Add/modify sync tests.

**Interfaces:**
- Consumes: active structured-data queues, deleted IDs, WorkManager unique work state.
- Produces: pending count that ignores obsolete file queues and auto-sync UI that accurately reports queued/running/completed state.

- [ ] **Step 1: Write failing test `legacy_file_queues_do_not_change_active_pending_count`.
- [ ] **Step 2: Run focused test and verify the current count includes the legacy queues.
- [ ] **Step 3: Remove legacy file queue contributions from active pending computation and `hasPendingSyncWork()`.
- [ ] **Step 4: Replace immediate `isAutoSyncing=false` after enqueue with state derived from observable work state or an explicit queued state.
- [ ] **Step 5: Preserve Persian user-visible status strings.
- [ ] **Step 6: Run focused tests and full unit suite.
- [ ] **Step 7: Commit.

---

### Task 6: Audit subscription, theme, dialogs, and mandatory unit-rule invariants

**Files:**
- Modify: `app/src/main/java/com/example/data/subscription/SubscriptionManager.kt` only where comments/state contradict server-controlled entitlement or exact 7-day semantics.
- Modify: `app/src/main/java/com/example/ui/dialogs/SubscriptionDialog.kt` only where needed to keep one authoritative 7-day banner.
- Modify: `app/src/main/java/com/example/ui/dialogs/WorkshopsDialog.kt` to remove dead/fallback logic without changing intended behavior.
- Modify: `app/src/main/java/com/example/data/AppDatabase.kt` only as necessary for mandatory unit rules and legacy migrations.
- Modify: light/dark theme or affected UI files only when a real contrast defect is verified.
- Add focused tests for unit-rule invariants.

**Interfaces:**
- Consumes: current subscription manager, theme colors, unit-rule DAO methods.
- Produces: exactly one 7-day trial status presentation, mandatory base rules protected, and no-workshop UI that reflects reality.

- [ ] **Step 1: Write failing test `mandatory_unit_rules_cannot_be_deleted` for all four exact rules.
- [ ] **Step 2: Run it and verify the existing regression baseline.
- [ ] **Step 3: Confirm and preserve mandatory-rule repair on login/account initialization.
- [ ] **Step 4: Remove misleading “subscription written to Firebase” comments or status claims if they do not match the server-controlled rules.
- [ ] **Step 5: Verify the single trial banner remains “نسخه آزمایشی ۷ روزه خیاطان” and no duplicate trial card exists.
- [ ] **Step 6: Run relevant UI/data tests and full unit suite.
- [ ] **Step 7: Commit.

---

### Task 7: Harden GitHub Actions for final artifact production

**Files:**
- Modify: `.github/workflows/android-build.yml`
- Modify: `.github/workflows/build-release.yml`

**Interfaces:**
- Consumes: final source commit and existing signing secrets.
- Produces: debug APK artifact on build workflow; signed AAB/APK, source ZIP, and checksum manifest on release workflow; publication failures are real failures.

- [ ] **Step 1: Write/validate workflow assertions for artifact paths, source commit identity, and absence of best-effort release publication.
- [ ] **Step 2: Ensure build workflow runs on the final development branch and main/PR paths needed for verification and uploads a debug APK.
- [ ] **Step 3: Ensure production workflow is manual and main-push driven, runs tests before signing/building, and emits signed AAB/APK.
- [ ] **Step 4: Add SHA-256 checksum generation for the release AAB/APK/source ZIP and upload it as an artifact and release asset.
- [ ] **Step 5: Remove `continue-on-error: true` from the release publication step.
- [ ] **Step 6: Keep keystore cleanup in an always-run step and verify the source ZIP comes from `GITHUB_SHA` only.
- [ ] **Step 7: Keep artifact names deterministic and paths exact, and use the current compatible upload-artifact major version already adopted by the repository unless verification shows a newer major is required.
- [ ] **Step 8: Commit workflow changes.

---

### Task 8: Full verification, build, artifact inspection, and release readiness

**Files:**
- No source changes expected unless verification exposes a concrete defect.
- Modify only the failing source/test/workflow file that root-cause analysis identifies.

**Interfaces:**
- Consumes: all previous tasks.
- Produces: verified green unit/build workflow and downloadable production artifacts tied to the final commit.

- [ ] **Step 1: Run the complete unit-test command used by CI: `gradle testDebugUnitTest --stacktrace --no-daemon`.
- [ ] **Step 2: Run debug APK assembly: `gradle assembleDebug --stacktrace --no-daemon`.
- [ ] **Step 3: Trigger/observe GitHub Actions for the final branch and inspect failed jobs/logs when necessary.
- [ ] **Step 4: Merge to `main` only after branch verification is green, if the repository state and existing PR are consistent with the approved finalization.
- [ ] **Step 5: Trigger/observe production release workflow and verify signed AAB, signed APK, source ZIP, checksum artifact, and GitHub Release publication.
- [ ] **Step 6: Verify release identity against the exact `GITHUB_SHA` source archive and inspect artifact metadata.
- [ ] **Step 7: If any step fails, apply systematic root-cause debugging and repeat verification until the actual failing condition is resolved.
- [ ] **Step 8: Final repository-wide scan for prohibited active Storage symbols and stale hard-coded workshop-1 assignments.
- [ ] **Step 9: Commit any verification-only corrections and rerun affected and complete verification paths.

## Commit Sequence

1. Regression tests.
2. Workshop/account correctness fixes.
3. Storage removal.
4. Sync-state fixes.
5. UI/subscription/unit-rule cleanup.
6. CI/release hardening.
7. Final verification corrections, if any.

Each commit must be independently buildable where practical. No success claim is made from source inspection alone; final readiness requires fresh test/build/Workflow evidence.
