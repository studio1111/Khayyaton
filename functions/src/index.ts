import { initializeApp } from "firebase-admin/app";
import { getFirestore, FieldValue, Timestamp } from "firebase-admin/firestore";
import { onCall, HttpsError } from "firebase-functions/v2/https";
import { defineSecret } from "firebase-functions/params";
import { createHash } from "node:crypto";

initializeApp();
const db = getFirestore();

const BAZAAR_CLIENT_ID = defineSecret("BAZAAR_CLIENT_ID");
const BAZAAR_CLIENT_SECRET = defineSecret("BAZAAR_CLIENT_SECRET");
const BAZAAR_REFRESH_TOKEN = defineSecret("BAZAAR_REFRESH_TOKEN");

const OWNER_EMAIL = "www.chelsea1010@gmail.com";

const PACKAGE_NAME = "com.farsinnov.khayyaton";
const PRODUCTS = new Set([
  "khayyaton_3_month",
  "khayyaton_6_month",
  "khayyaton_1_year",
]);

let accessToken: string | null = null;
let accessTokenExpiresAt = 0;

async function getAccessToken(): Promise<string> {
  if (accessToken && Date.now() < accessTokenExpiresAt - 60_000) return accessToken;

  const body = new URLSearchParams({
    grant_type: "refresh_token",
    client_id: BAZAAR_CLIENT_ID.value(),
    client_secret: BAZAAR_CLIENT_SECRET.value(),
    refresh_token: BAZAAR_REFRESH_TOKEN.value(),
  });

  const response = await fetch("https://pardakht.cafebazaar.ir/devapi/v2/auth/token/", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body,
  });

  if (!response.ok) {
    throw new Error("Bazaar OAuth token request failed");
  }

  const json = await response.json() as { access_token?: string; expires_in?: number };
  if (!json.access_token) throw new Error("Bazaar OAuth response has no access_token");

  accessToken = json.access_token;
  accessTokenExpiresAt = Date.now() + Number(json.expires_in ?? 3600) * 1000;
  return accessToken;
}

async function validateBazaarSubscription(productId: string, purchaseToken: string) {
  const token = await getAccessToken();
  const buildUrl = (t: string) =>
    "https://pardakht.cafebazaar.ir/devapi/v2/api/applications/" +
    encodeURIComponent(PACKAGE_NAME) +
    "/subscriptions/" +
    encodeURIComponent(productId) +
    "/purchases/" +
    encodeURIComponent(purchaseToken) +
    "/?access_token=" +
    encodeURIComponent(t);

  let response = await fetch(buildUrl(token));
  if (response.status === 401) {
    accessToken = null;
    response = await fetch(buildUrl(await getAccessToken()));
  }

  if (!response.ok) throw new Error("Bazaar subscription validation failed");

  return await response.json() as {
    initiationTimestampMsec?: number | string;
    validUntilTimestampMsec?: number | string;
    autoRenewing?: boolean;
  };
}

export const ensureOwnerAccess = onCall(
  {
    region: "europe-west1",
    timeoutSeconds: 15,
    enforceAppCheck: true,
    consumeAppCheckToken: true,
  },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "ورود به حساب الزامی است.");
    }

    const email = String(request.auth.token.email ?? "").trim().toLowerCase();
    if (email !== OWNER_EMAIL.toLowerCase()) {
      return { granted: false };
    }

    const uid = request.auth.uid;
    const subRef = db.collection("users").doc(uid).collection("subscription").doc("info");

    await subRef.set({
      subscriptionStatus: "ADMIN_GRANTED",
      source: "owner_account",
      ownerEmail: OWNER_EMAIL,
      grantedAt: FieldValue.serverTimestamp(),
    }, { merge: true });

    return { granted: true, subscriptionStatus: "ADMIN_GRANTED" };
  }
);

export const verifyBazaarSubscription = onCall(
  {
    region: "europe-west1",
    timeoutSeconds: 30,
    enforceAppCheck: true,
    consumeAppCheckToken: true,
    secrets: [BAZAAR_CLIENT_ID, BAZAAR_CLIENT_SECRET, BAZAAR_REFRESH_TOKEN],
  },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "ورود به حساب الزامی است.");
    }

    const uid = request.auth.uid;
    const data = request.data as Record<string, unknown>;
    const packageName = String(data.packageName ?? "");
    const productId = String(data.productId ?? "");
    const purchaseToken = String(data.purchaseToken ?? "");
    const payload = String(data.payload ?? "");
    const orderId = String(data.orderId ?? "");

    if (
      packageName !== PACKAGE_NAME ||
      !PRODUCTS.has(productId) ||
      purchaseToken.length < 8 ||
      purchaseToken.length > 4096 ||
      payload !== `user_${uid}`
    ) {
      throw new HttpsError("invalid-argument", "اطلاعات خرید معتبر نیست.");
    }

    try {
      const purchase = await validateBazaarSubscription(productId, purchaseToken);
      const expiresAt = Number(purchase.validUntilTimestampMsec ?? 0);
      const startedAt = Number(purchase.initiationTimestampMsec ?? 0);

      if (!Number.isFinite(expiresAt) || expiresAt <= Date.now()) {
        throw new HttpsError("failed-precondition", "اشتراک کافه‌بازار فعال نیست یا منقضی شده است.");
      }

      const tokenHash = createHash("sha256").update(purchaseToken).digest("hex");
      const tokenRef = db.collection("subscriptionPurchases").doc(tokenHash);
      const subRef = db.collection("users").doc(uid).collection("subscription").doc("info");

      await db.runTransaction(async tx => {
        const existing = await tx.get(tokenRef);
        if (existing.exists && existing.data()?.uid !== uid) {
          throw new HttpsError("permission-denied", "این خرید قبلاً به حساب دیگری متصل شده است.");
        }

        const now = Timestamp.now();
        tx.set(tokenRef, {
          uid,
          productId,
          orderId,
          verifiedAt: now,
          expiresAt: Timestamp.fromMillis(expiresAt),
        }, { merge: true });

        tx.set(subRef, {
          subscriptionStatus: "SUBSCRIBED",
          activeProductId: productId,
          startedAt: Timestamp.fromMillis(startedAt > 0 ? startedAt : Date.now()),
          expiresAt: Timestamp.fromMillis(expiresAt),
          orderId,
          autoRenewing: Boolean(purchase.autoRenewing),
          source: "cafebazaar_server_verification",
          verifiedAt: FieldValue.serverTimestamp(),
        }, { merge: true });
      });

      return {
        subscriptionStatus: "SUBSCRIBED",
        activeProductId: productId,
        startedAt,
        expiresAt,
        orderId,
        autoRenewing: Boolean(purchase.autoRenewing),
      };
    } catch (error) {
      if (error instanceof HttpsError) throw error;
      throw new HttpsError("internal", "تأیید اشتراک کافه‌بازار روی سرور ناموفق بود.");
    }
  }
);
