# Khayyaton Final Reliability and Offline-First Architecture Design

**Date:** 2026-09-28  
**Branch:** `fix/offline-first-sync-v2`

## Goal

Deliver a production-ready Khayyaton data, account, subscription, UI-state, and CI/release path in which Room is the local source of truth, Firestore is the cloud copy, deleted records never resurrect, account data never leaks across users, no file-storage subsystem remains, and GitHub Actions produces verified build artifacts from the final source commit.

## Product constraints

- Khayyaton stores only structured textual/numeric application data. It does not store user files.
- Firebase remains the backend.
- The implementation must not require Firebase Blaze/paid Secret Manager features.
- UI-visible data comes from Room flows, not direct Firestore reads.
- No workshop is created automatically at runtime.
- The mandatory unit rules must always exist and must not be user-deletable:
  - 3 -> 2
  - 2 -> 1.5
  - 1 -> 1
  - 0.5 -> 0.5
- Trial entitlement is exactly 7 days everywhere it is represented.
- User-facing warning/error messages remain Persian.
- Existing legacy local/cloud data must remain readable where practical, while the active architecture must not create new storage/file dependencies.

## Architecture

`Compose UI -> ViewModel -> WorkshopRepository -> Room` is the single UI data path.

`SyncManager/CloudSyncWorker -> FirebaseService -> Firestore` is the cloud synchronization path.

Room remains the authoritative local state for screens. Firestore is reconciled through deterministic, idempotent sync operations using client-generated UUID `syncId`, deletion tombstones, and last-updated timestamps.

Firestore Android offline persistence may remain enabled as a secondary cache. It is not used as the app's UI source of truth.

## Final synchronization invariants

1. Every cloud-synchronized domain record has a stable client-generated `syncId`.
2. Firestore document IDs equal `syncId` for new records.
3. Local writes happen before cloud writes.
4. Cloud restore happens before the first upload for a new account/device state that has not been marked cloud-ready.
5. Remote data is merged into Room with upsert semantics and deterministic timestamp conflict handling.
6. Confirmed deletions are represented by persistent tombstones and are checked before applying remote child records.
7. Deleting a workshop also tombstones its directly owned child records.
8. A deleted record cannot be recreated by a later stale snapshot.
9. Account switching clears the previous account's Room state before the new account becomes visible as the active data context.
10. Only one authority performs account-isolation cleanup; other sync components must be idempotent and must not race to repopulate stale data.
11. Pending-sync indicators reflect active structured-data work only and do not count legacy file queues.
12. Auto-sync UI state reflects actual WorkManager execution or an explicit queued state, never an immediate false completion.
13. A new preset/order/payment is always assigned to the currently active workshop. A new preset must never silently fall back to workshop ID 1.
14. Duplicate preset-name detection is scoped to the active workshop.
15. No runtime path creates a default/placeholder workshop merely because the list is empty.
16. Legacy migrations may preserve historical data compatibility, but current runtime code must not invoke historical default-workshop creation.

## File-storage removal

Because Khayyaton does not store user files, the active application architecture must contain no Firebase Storage feature.

Remove:
- Firebase Storage dependency and version-catalog entry.
- Storage initialization/property/bucket configuration.
- `StorageSyncManager.kt` and any active callers.
- Upload/download queue behavior for files.
- Firestore sync of `fileUrl`/`storagePath` as active fields.

Backward compatibility:
- Legacy Room columns/entities may be preserved temporarily when removing them would require an unsafe table rebuild.
- Any retained legacy columns/tables must be inert, cleared where safe during migration, excluded from pending-sync counts, and never written by new code.
- The user-facing architecture must not depend on Firebase Storage.

## Account isolation

There must be one account-switch transition:

1. Detect a UID change.
2. Cancel/collapse pending work owned by the previous UID.
3. Clear prior account local domain/sync state.
4. Reset active workshop and filters.
5. Save the new local account UID.
6. Restore the new account from Firestore before uploading new local state.
7. Mark the new account cloud-ready.
8. Expose the new account's data to UI.

The transition must be idempotent and safe if Firebase auth listeners and ViewModel initialization observe the same UID change.

## Workshop and model integrity

- `activeWorkshopId` starts at `0L`, not a fabricated workshop ID.
- When workshops are present and the saved active ID is invalid, select a real persisted workshop.
- When no workshop exists, active ID is 0 and UI must not imply that a real workshop exists.
- New records receive the current active workshop ID at the ViewModel/repository boundary.
- New model presets must never inherit a hard-coded workshop ID.
- Preset duplicate checks must be workshop-scoped.

## Unit rules

The four required base conversion rules are always present for an account. Deletion methods refuse deletion of these rules. Login/account initialization repairs only missing mandatory base rules and never uses deletion history to suppress them.

## Subscription

- Trial is exactly 7 days from Firebase Auth account creation.
- The banner is the single visible trial status presentation; duplicate trial cards are not created.
- Subscription purchase/restore logic remains compatible with Cafe Bazaar.
- Comments and UI state must not claim that client code writes subscription entitlement to Firestore when Firestore rules make that server-controlled.
- Remaining entitlement presentation must remain internally consistent.

## UI integrity

- No white text on white/light surfaces.
- Preserve intentional white text on dark or colored controls.
- When no real workshop exists, use a neutral no-workshop state rather than a fake “main workshop” identity.
- Closing or changing cloud-account/auth dialogs must not leave stale modal state visible after auth transitions.

## Workflow and release requirements

The final CI must:
- run unit tests;
- assemble a debug APK;
- for production releases, build signed AAB and APK;
- verify signatures;
- create a source ZIP from the exact build commit;
- create SHA-256 checksums for release artifacts;
- upload APK/AAB/source/checksum artifacts;
- fail when release publication fails instead of hiding the failure with `continue-on-error`;
- remove the decoded keystore in an always-run cleanup step;
- avoid packaging secrets or build outputs inside the source ZIP;
- use artifact actions compatible with the repository's current GitHub Actions environment.

The release workflow must produce fresh outputs from the final commit on `main` and remain manually dispatchable.

## Verification targets

Before declaring the project ready:
1. Static scan contains no active Firebase Storage dependency/import/use.
2. Unit tests pass.
3. Debug APK build passes.
4. Release AAB/APK build passes with configured signing secrets.
5. Artifact list contains debug APK, release AAB, release APK, source ZIP, and checksum manifest where the corresponding workflow runs support them.
6. Source ZIP commit identity matches the release commit.
7. GitHub Actions release publication is a required step, not best-effort.
8. Regression tests cover multi-workshop preset creation, account isolation, no-workshop state, mandatory unit rules, deletion tombstones, and sync pending counts.

## Non-goals

- No migration away from Firebase.
- No Firebase Storage replacement with another file service.
- No redesign of the app's visual language beyond fixes required for readability and state correctness.
- No speculative refactor of unrelated features.

## Expected result

The final branch is buildable, testable, free of active file-storage code, safe against the known data-resurrection/account-switch/workshop-assignment failures, and configured so future production runs generate verified deliverables from the exact source that was built.
