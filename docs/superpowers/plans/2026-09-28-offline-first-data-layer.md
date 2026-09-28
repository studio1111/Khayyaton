# Offline-First Data Layer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Make Khayyaton's Room/Firebase data layer durable offline-first with UUID document identity, tombstones, reconnect sync, realtime Firestore listeners, and Storage queues without breaking legacy data.

**Architecture:** Keep the current Room entities and legacy numeric IDs for UI relations, but add durable sync metadata and queue tables. A singleton SyncManager owns connectivity-triggered upload/delete/reconcile work; Firestore listeners feed Room through documentId-based upserts. The existing FirebaseService remains the authentication boundary.

**Tech Stack:** Kotlin, Room 2.8.5, Coroutines/Flow, Firebase Firestore 34.19.0, Firebase Storage via Firebase BoM, ConnectivityManager, WorkManager 2.12.0, Robolectric/JUnit.

**Spec:** docs/superpowers/specs/2026-09-28-offline-first-data-layer-design.md

## Global Constraints

- Room is the only UI data source.
- New cloud document IDs are UUIDs generated client-side.
- Main lists use Room Flow; Firestore listeners are the cloud input.
- Server data enters Room only through documentId/syncId upsert.
- deleted_ids blocks resurrection until server deletion is confirmed.
- File operations use durable upload_queue and pending_deletes.
- Firestore offline persistence remains enabled with a large cache.
- All production comments added by this work are Persian.
- Existing legacy numeric IDs and existing successful restore behavior must remain compatible.

## Review Focus

- Legacy Firestore documents without syncId must remain visible and map to stable local records.
- Deleting the last workshop must not recreate a placeholder.
- A stale Firestore snapshot after a local delete must not resurrect the record.
- An offline update must not be overwritten by an older server snapshot.
- A failed Storage delete/upload must survive process death and retry after reconnect.

### Task 1: Durable Room Sync Schema

**Files:**
- Modify: app/src/main/java/com/example/model/Models.kt
- Modify: app/src/main/java/com/example/data/Daos.kt
- Modify: app/src/main/java/com/example/data/AppDatabase.kt
- Test: app/src/test/java/com/example/DataIntegrityTest.kt

**Interfaces:**
- Produces Room entities/DAOs for deleted_ids, upload_queue, pending_deletes and per-record sync metadata.
- Preserves current entity primary keys and syncId indexes.

- [ ] Write failing tests for tombstone persistence, queue persistence, and upsert-by-syncId.
- [ ] Run the focused test and verify it fails for the missing schema.
- [ ] Add entities, enums, DAOs, and Room migration.
- [ ] Run the focused test and full unit suite.
- [ ] Commit.

### Task 2: Firestore Configuration and Connectivity

**Files:**
- Modify: app/src/main/java/com/example/MainActivity.kt
- Create: app/src/main/java/com/example/data/sync/ConnectivityMonitor.kt
- Modify: app/src/main/java/com/example/data/firebase/FirebaseService.kt
- Modify: app/build.gradle.kts
- Modify: gradle/libs.versions.toml
- Test: app/src/test/java/com/example/ConnectivityMonitorTest.kt

**Interfaces:**
- ConnectivityMonitor exposes StateFlow<Boolean>.
- FirebaseService initializes Firestore persistent cache and exposes Firestore/Storage handles to SyncManager.

- [ ] Write failing tests for connectivity state transitions.
- [ ] Verify RED.
- [ ] Implement NetworkCallback monitor and large Firestore cache.
- [ ] Add Firebase Storage dependency.
- [ ] Verify GREEN and full suite.
- [ ] Commit.

### Task 3: SyncManager and Realtime Reconciliation

**Files:**
- Create: app/src/main/java/com/example/data/sync/SyncManager.kt
- Modify: app/src/main/java/com/example/data/firebase/FirebaseService.kt
- Modify: app/src/main/java/com/example/data/AppDatabase.kt
- Modify: app/src/main/java/com/example/MainActivity.kt
- Modify: app/src/main/java/com/example/data/sync/CloudSyncWorker.kt
- Test: app/src/test/java/com/example/SyncPolicyTest.kt

**Interfaces:**
- SyncManager.start()/stop(), syncNow(), pendingCount: StateFlow<Int>.
- Sync order: uploads -> pending deletes -> listener reconciliation.
- Listener input always filters deleted_ids and upserts by syncId.
- Local writes are represented as pending until acknowledged.

- [ ] Write failing tests for duplicate-free upsert, tombstone filtering, and last-write-wins policy.
- [ ] Verify RED.
- [ ] Implement SyncManager and listener lifecycle.
- [ ] Route existing repository cloud deletion records into durable tombstones.
- [ ] Keep legacy one-shot restore only as a compatibility bootstrap where necessary, not as the UI list source.
- [ ] Verify GREEN and full suite.
- [ ] Commit.

### Task 4: Storage Upload/Delete Queue

**Files:**
- Create: app/src/main/java/com/example/data/sync/StorageSyncManager.kt
- Modify: app/src/main/java/com/example/data/AppDatabase.kt
- Modify: app/src/main/java/com/example/model/Models.kt
- Test: app/src/test/java/com/example/StorageQueueTest.kt

**Interfaces:**
- enqueueUpload(localFilePath, storagePath, documentId)
- enqueueDelete(documentId, storagePath)
- processQueues() with exponential retry and FAILED state.
- Storage object names are UUID-based.

- [ ] Write failing tests for retry state transitions and duplicate upload protection.
- [ ] Verify RED.
- [ ] Implement queue processor using Firebase Storage UploadTask.
- [ ] Update Room/Firestore fileUrl after successful upload.
- [ ] Delete local files only after successful server-side deletion where required.
- [ ] Verify GREEN and full suite.
- [ ] Commit.

### Task 5: Repository/ViewModel Integration and Regression Coverage

**Files:**
- Modify: app/src/main/java/com/example/data/AppDatabase.kt
- Modify: app/src/main/java/com/example/ui/KhayyatonViewModel.kt
- Modify: app/src/main/java/com/example/MainActivity.kt
- Modify: app/src/main/java/com/example/data/sync/CloudSyncWorker.kt
- Modify: app/src/test/java/com/example/DataIntegrityTest.kt

**Interfaces:**
- Existing save/delete methods create UUID-backed records and durable tombstones.
- UI remains Room Flow driven.
- pending operation count is exposed to ViewModel.
- Existing placeholder-workshop fix remains intact.

- [ ] Write failing regression tests for delete/reconnect, logout/login restore, and no default workshop recreation.
- [ ] Verify RED.
- [ ] Integrate SyncManager lifecycle with app startup and WorkManager safety net.
- [ ] Remove SharedPreferences as the authoritative deletion queue while preserving migration of old pending deletions.
- [ ] Verify focused tests, full unit suite, and release build.
- [ ] Commit.

### Task 6: Final Verification and Release Readiness

**Files:**
- Modify only files required by verification findings.
- Inspect: GitHub Actions workflow, release artifacts, Firestore rules.

- [ ] Run the complete project test/build workflow.
- [ ] Inspect failures and fix root causes with RED->GREEN tests.
- [ ] Verify latest workflow conclusion is success.
- [ ] Verify release artifact availability.
- [ ] Review data security and Firestore rules for the new document fields.
- [ ] Record any remaining device-only checks explicitly; do not claim runtime behavior without device evidence.
