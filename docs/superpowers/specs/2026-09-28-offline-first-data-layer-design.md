# Khayyaton Offline-First Data Layer Design

Goal: Room is the UI source of truth; Firestore and Storage synchronize through durable queues, tombstones, snapshot listeners, connectivity monitoring, and WorkManager.

Current gaps: deletion tombstones are in SharedPreferences, main cloud restore uses one-shot get(), and there is no durable file upload/delete queue.

Architecture: Compose -> Room Flow -> Repository -> SyncManager -> Firestore/Storage. Firestore persistence remains enabled, but UI never reads Firestore directly.

Core guarantees: UUID document IDs, Room upsert by documentId, durable deleted_ids, pending_deletes, upload_queue, server updatedAt conflict resolution, reconnect-triggered sync, and tests for duplicate prevention and ghost-record prevention.

Scope: preserve the existing app and legacy data mappings; change the data/sync layer only where required, then run the complete test/build workflow before release claims.