const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_creator_123";
const BOB_UID = "bob_other_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validSurprisePayload(creatorUid, status = "published", now = new Date(Date.now() - 2000)) {
  const payload = {
    creatorUid,
    title: "Happy Birthday Maya!",
    categoryId: "birthday",
    recipientName: "Maya",
    senderName: "Alice",
    introMessage: "Open this when you have a quiet moment!",
    mainMessage: "Wishing you the brightest year ahead filled with laughter.",
    fontStyle: "playfair",
    textAlignment: "center",
    textColorHex: "#2D1623",
    boldBody: false,
    italicBody: false,
    photos: [],
    photoCaptions: [],
    voiceDurationSec: 0,
    musicTrackId: "music_box_lullaby",
    musicVolume: 0.75,
    themeId: "blush_rose",
    customBgColorHex: "#FFF1F5",
    cardStyle: "glass",
    animationEffect: "hearts",
    envelopeStyle: "wax_seal",
    giftBoxStyle: "classic_rose",
    selectedGames: ["catch_hearts", "memory_match"],
    gameDifficulty: "medium",
    gamesMandatory: true,
    allowSkip: true,
    secretCodeClue: "Our favorite coffee spot?",
    secretCodeAnswer: "luna",
    secretPasscode: "",
    unlockAtMillis: 0,
    eventDateMillis: 0,
    expiresAtMillis: 0,
    status,
    openCount: 0,
    replyCount: 0,
    createdAt: now,
    updatedAt: now,
  };
  if (status === "published") {
    payload.publishedAt = now;
  }
  return payload;
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Users collection: owner create/read/update succeeds; unauth, cross-user, and ghost fields fail", async () => {
  const now = new Date(Date.now() - 2000);
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      displayName: "Alice",
      email: "alice@example.com",
      notifyOnOpen: true,
      notifyOnReply: true,
      defaultAllowSkip: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      displayName: "Alice",
      email: "alice@example.com",
      notifyOnOpen: true,
      notifyOnReply: true,
      defaultAllowSkip: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Alice Wonder",
      notifyOnOpen: false,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Cross-user PII read/write denied
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "Hacked",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Shadow/ghost field rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Surprises: creator CRUD, filtered list query, and @DocumentId guard", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();

  const draftPayload = validSurprisePayload(ALICE_UID, "draft");
  await assertSucceeds(aliceDb.collection("surprises").doc("gfy_draft_1").set(draftPayload));

  // Unauthenticated & Bob cannot read Alice's draft or list surprises unfiltered
  await assertFails(unauthDb.collection("surprises").doc("gfy_draft_1").get());
  await assertFails(bobDb.collection("surprises").doc("gfy_draft_1").get());
  await assertFails(unauthDb.collection("surprises").get());
  await assertFails(aliceDb.collection("surprises").get());

  // Filtered creator query matches Kotlin repository
  await assertSucceeds(
    aliceDb.collection("surprises").where("creatorUid", "==", ALICE_UID).get()
  );

  // Creator publishes draft
  await assertSucceeds(
    aliceDb.collection("surprises").doc("gfy_draft_1").update({
      status: "published",
      publishedAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // @DocumentId `id` key in payload must fail
  const badPayload = validSurprisePayload(ALICE_UID, "published");
  badPayload.id = "gfy_bad_1";
  await assertFails(aliceDb.collection("surprises").doc("gfy_bad_1").set(badPayload));

  // Cross-user update & delete fail
  await assertFails(
    bobDb.collection("surprises").doc("gfy_draft_1").update({
      title: "Hijacked",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(bobDb.collection("surprises").doc("gfy_draft_1").delete());

  // Owner delete succeeds
  await assertSucceeds(aliceDb.collection("surprises").doc("gfy_draft_1").delete());
});

test("Recipient reveal flow: unauth get, bounded openCount increment, reply, notification, and event", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 3000);

  await assertSucceeds(
    aliceDb.collection("surprises").doc("gfy_pub_8899").set(validSurprisePayload(ALICE_UID, "published", now))
  );

  // Recipient opens published gift by exact ID without signing in
  await assertSucceeds(unauthDb.collection("surprises").doc("gfy_pub_8899").get());

  // Recipient increments openCount by +1 and sets status='opened'
  await assertSucceeds(
    unauthDb.collection("surprises").doc("gfy_pub_8899").update({
      openCount: 1,
      status: "opened",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Recipient CANNOT tamper with mainMessage or inflate openCount by +5
  await assertFails(
    unauthDb.collection("surprises").doc("gfy_pub_8899").update({
      title: "Tampered",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    unauthDb.collection("surprises").doc("gfy_pub_8899").update({
      openCount: 6,
      status: "opened",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Recipient submits valid reply & notification & event
  await assertSucceeds(
    unauthDb.collection("replies").doc("rep_101").set({
      surpriseId: "gfy_pub_8899",
      creatorUid: ALICE_UID,
      surpriseTitle: "Happy Birthday Maya!",
      recipientName: "Maya",
      reaction: "loved_it",
      message: "This made me cry happy tears!!",
      emojiTag: "❤️",
      readFlag: false,
      createdAt: new Date(Date.now() - 800),
    })
  );

  // Spoofed creatorUid on reply must fail
  await assertFails(
    unauthDb.collection("replies").doc("rep_spoof").set({
      surpriseId: "gfy_pub_8899",
      creatorUid: BOB_UID,
      surpriseTitle: "Happy Birthday Maya!",
      recipientName: "Maya",
      reaction: "loved_it",
      message: "Spoof",
      emojiTag: "❤️",
      readFlag: false,
      createdAt: new Date(Date.now() - 800),
    })
  );

  // Notification & event creation
  await assertSucceeds(
    unauthDb.collection("notifications").doc("notif_101").set({
      userId: ALICE_UID,
      surpriseId: "gfy_pub_8899",
      type: "replied",
      title: "New Reply from Maya!",
      message: "Maya loved your surprise and left a heartfelt reply.",
      readFlag: false,
      createdAt: new Date(Date.now() - 700),
    })
  );
  await assertSucceeds(
    unauthDb.collection("surpriseEvents").doc("evt_101").set({
      surpriseId: "gfy_pub_8899",
      creatorUid: ALICE_UID,
      eventType: "opened",
      anonymousSessionId: "sess_abc123",
      createdAt: new Date(Date.now() - 700),
    })
  );

  // Creator reads & marks reply and notification as read; Bob is denied
  await assertSucceeds(aliceDb.collection("replies").where("creatorUid", "==", ALICE_UID).get());
  await assertSucceeds(aliceDb.collection("notifications").where("userId", "==", ALICE_UID).get());
  await assertSucceeds(aliceDb.collection("replies").doc("rep_101").update({ readFlag: true }));
  await assertSucceeds(aliceDb.collection("notifications").doc("notif_101").update({ readFlag: true }));

  await assertFails(bobDb.collection("replies").where("creatorUid", "==", ALICE_UID).get());
  await assertFails(bobDb.collection("notifications").doc("notif_101").get());
  await assertFails(unauthDb.collection("replies").doc("rep_101").get());
});
