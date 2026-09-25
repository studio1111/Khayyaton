#!/usr/bin/env node
/**
 * Set/remove Khayyaton owner access using Firebase Authentication Custom Claims.
 *
 * Requirements:
 *   npm install firebase-admin
 *   export GOOGLE_APPLICATION_CREDENTIALS="/path/to/service-account.json"
 *
 * Usage:
 *   node scripts/set-owner-claim.mjs <firebase-uid>
 *   node scripts/set-owner-claim.mjs <firebase-uid> remove
 *
 * Never commit the service-account JSON or print it.
 */
import admin from "firebase-admin";

const uid = process.argv[2];
const action = process.argv[3] ?? "grant";

if (!uid || !/^[A-Za-z0-9:_-]{6,256}$/.test(uid)) {
  console.error("Usage: node scripts/set-owner-claim.mjs <firebase-uid> [remove]");
  process.exit(2);
}

if (!admin.apps.length) admin.initializeApp();

const user = await admin.auth().getUser(uid);
const current = user.customClaims ?? {};
const next = { ...current };

if (action === "remove") {
  delete next.role;
  delete next.admin;
} else if (action === "grant") {
  next.role = "owner";
  next.admin = true;
} else {
  console.error("Second argument must be omitted or 'remove'.");
  process.exit(2);
}

await admin.auth().setCustomUserClaims(uid, next);
console.log(action === "remove" ? "Owner access removed." : "Owner access granted.");
console.log("The user must sign out/in or refresh the ID token before the new claim is visible.");
