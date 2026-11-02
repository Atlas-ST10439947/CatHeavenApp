const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const fs = require("fs");

let testEnv;

async function setup() {
testEnv = await initializeTestEnvironment({
  projectId: "catheaven-dev",
  firestore: {
    rules: fs.readFileSync("firestore.rules", "utf8"),
    host: "127.0.0.1",
    port: 8080,
  },
});

  // Seed a sample cat document, bypassing rules (as if an admin created it earlier)
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("cats").doc("cat-milo").set({
      name: "Milo",
      adoptionStatus: "resident",
    });
  });

  // Seed a sample user profile, bypassing rules
await testEnv.withSecurityRulesDisabled(async (context) => {
  await context.firestore().collection("users").doc("some-other-user").set({
    fullName: "Jane Supporter",
    email: "jane@example.com",
    role: "supporter",
  });
});
}

async function runTests() {
  await setup();

  // Test 1: anyone (no auth) can read a cat profile
  const anon = testEnv.unauthenticatedContext();
  await assertSucceeds(
    anon.firestore().collection("cats").doc("cat-milo").get()
  );
  console.log("PASS: anonymous user can read cat profile");

  // Test 2: a signed-in, non-admin user CANNOT edit a cat profile
  const regularUser = testEnv.authenticatedContext("fakeuser123", {});
  await assertFails(
    regularUser.firestore().collection("cats").doc("cat-milo").update({
      name: "Hacked Name",
    })
  );
  console.log("PASS: non-admin user is blocked from editing a cat profile");

  // Test 3: a signed-in user WITH the admin claim CAN edit a cat profile
  const adminUser = testEnv.authenticatedContext("admin-uid", { admin: true });
  await assertSucceeds(
    adminUser.firestore().collection("cats").doc("cat-milo").update({
      name: "Milo Updated",
    })
  );
  console.log("PASS: admin user can edit a cat profile");

  // Test 4: a regular (non-admin) user CANNOT read someone else's profile
await assertFails(
  regularUser.firestore().collection("users").doc("some-other-user").get()
);
console.log("PASS: non-admin user is blocked from reading another user's profile");

// Test 5: an admin CAN read someone else's profile
await assertSucceeds(
  adminUser.firestore().collection("users").doc("some-other-user").get()
);
console.log("PASS: admin user can read another user's profile");

const newUser = testEnv.authenticatedContext("new-user-1", {});
await assertFails(
  newUser.firestore().collection("users").doc("new-user-1").set({ fullName: "Sneaky", email: "s@example.com", role: "admin" })
);
console.log("PASS: user cannot self-assign admin role");
await assertSucceeds(
  newUser.firestore().collection("users").doc("new-user-1").set({ fullName: "Normal", email: "n@example.com", role: "supporter" })
);
console.log("PASS: user can register as a supporter");

  await profileTests();
  await contentTests();
  await ledgerTests();
  await supporterChangeTests();

  await testEnv.cleanup();
  console.log("All tests passed.");
}

// ---- Profile editing (Settings > Profile / Notifications) ----
async function profileTests() {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc("supporter-1").set({
      fullName: "Sam Supporter",
      email: "sam@example.com",
      phone: "0821234567",
      role: "supporter",
    });
  });
  const sam = testEnv.authenticatedContext("supporter-1", {}).firestore();
  const samDoc = sam.collection("users").doc("supporter-1");

  await assertSucceeds(samDoc.update({ fullName: "Sam S", phone: "+27 82 123 4567", location: "Somerset West" }));
  console.log("PASS: supporter can update their name, phone and location");

  await assertSucceeds(samDoc.update({ notificationPrefs: { email: false, inApp: true } }));
  console.log("PASS: supporter can save notification preferences");

  await assertFails(samDoc.update({ role: "admin" }));
  console.log("PASS: supporter cannot promote themselves through a profile update");

  await assertFails(samDoc.update({ email: "new@example.com" }));
  console.log("PASS: supporter cannot change their sign-in email in Firestore");

  await assertFails(samDoc.update({ phone: "call me maybe" }));
  console.log("PASS: invalid phone number is rejected");

  await assertFails(samDoc.update({ fullName: "" }));
  console.log("PASS: empty name is rejected");

  await assertFails(samDoc.update({ notificationPrefs: { email: "yes" } }));
  await assertFails(samDoc.update({ notificationPrefs: { email: true, sms: true } }));
  console.log("PASS: malformed notification preferences are rejected");

  const otherUser = testEnv.authenticatedContext("someone-else", {}).firestore();
  await assertFails(otherUser.collection("users").doc("supporter-1").update({ fullName: "Hijacked" }));
  console.log("PASS: a user cannot edit someone else's profile");
}

// ---- Admin content (cats and menu items) ----
async function contentTests() {
  // context.firestore() is the compat (v8-style) API, so use compat field values too
  const firebase = require("firebase/compat/app");
  require("firebase/compat/firestore");
  const { Timestamp } = firebase.firestore;
  const serverTimestamp = () => firebase.firestore.FieldValue.serverTimestamp();
  const admin = testEnv.authenticatedContext("admin-uid", { admin: true }).firestore();
  const supporter = testEnv.authenticatedContext("supporter-1", {}).firestore();

  const goodCat = {
    name: "Amore",
    breed: "Tabby",
    gender: "female",
    personality: "cuddly",
    biography: "Loves sunny windowsills.",
    adoptionStatus: "resident",
    photoUrl: "amore",
    dateOfBirth: Timestamp.fromDate(new Date("2022-03-01")),
    dateArrived: Timestamp.fromDate(new Date("2023-01-15")),
    updatedAt: serverTimestamp(),
  };

  await assertSucceeds(admin.collection("cats").doc("cat-amore").set(goodCat));
  console.log("PASS: admin can add a valid cat");

  await assertFails(supporter.collection("cats").doc("cat-sneaky").set(goodCat));
  console.log("PASS: supporter cannot add a cat");

  await assertFails(admin.collection("cats").doc("cat-bad").set({ ...goodCat, name: "" }));
  console.log("PASS: cat without a name is rejected");

  await assertFails(admin.collection("cats").doc("cat-bad").set({ ...goodCat, adoptionStatus: "sold" }));
  console.log("PASS: unknown adoption status is rejected");

  await assertFails(admin.collection("cats").doc("cat-bad").set({ ...goodCat, photoUrl: "https://evil.example/x.png" }));
  console.log("PASS: photo must be one of the bundled photo names");

  await assertFails(admin.collection("cats").doc("cat-bad").set({
    ...goodCat, dateOfBirth: Timestamp.fromDate(new Date(Date.now() + 7 * 24 * 3600 * 1000)),
  }));
  console.log("PASS: future date of birth is rejected");

  await assertFails(admin.collection("cats").doc("cat-bad").set({
    ...goodCat, dateArrived: Timestamp.fromDate(new Date("2021-01-01")),
  }));
  console.log("PASS: arrival before birth is rejected");

  await assertSucceeds(admin.collection("cats").doc("cat-amore").delete());
  console.log("PASS: admin can delete a cat");

  const goodItem = { name: "Cappuccino", category: "foodAndDrink", price: 35, isAvailable: true, updatedAt: serverTimestamp() };

  await assertSucceeds(admin.collection("menuItems").doc("cappuccino").set(goodItem));
  await assertSucceeds(admin.collection("menuItems").doc("books").set({ ...goodItem, name: "Books", category: "giftShop", price: null }));
  console.log("PASS: admin can add menu items, with or without a price");

  await assertSucceeds(testEnv.unauthenticatedContext().firestore().collection("menuItems").doc("cappuccino").get());
  console.log("PASS: anyone can read the menu");

  await assertFails(supporter.collection("menuItems").doc("free-coffee").set({ ...goodItem, price: 0 }));
  console.log("PASS: supporter cannot change the menu");

  await assertFails(admin.collection("menuItems").doc("bad").set({ ...goodItem, price: -5 }));
  await assertFails(admin.collection("menuItems").doc("bad").set({ ...goodItem, category: "weapons" }));
  await assertFails(admin.collection("menuItems").doc("bad").set({ ...goodItem, secretNote: "x" }));
  console.log("PASS: invalid menu items are rejected");
}

// ---- Admin Booking Ledger ----
async function ledgerTests() {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("timeslots").doc("slot-ledger").set({
      startTime: "10:00", capacity: 10, bookedCount: 10, status: "full", lastBookingId: "booking-ledger",
    });
    await db.collection("bookings").doc("booking-ledger").set({
      supporterId: "supporter-1", timeslotId: "slot-ledger", numberOfGuests: 2, status: "confirmed",
    });
  });
  const admin = testEnv.authenticatedContext("admin-uid", { admin: true }).firestore();
  const sam = testEnv.authenticatedContext("supporter-1", {}).firestore();

  await assertSucceeds(admin.collection("bookings").where("timeslotId", "==", "slot-ledger").get());
  console.log("PASS: admin can list every supporter's bookings for the ledger");

  await assertFails(sam.collection("bookings").where("timeslotId", "==", "slot-ledger").get());
  console.log("PASS: supporter cannot list bookings without filtering to their own");

  await assertFails(sam.collection("timeslots").doc("slot-ledger").update({ bookedCount: 8, status: "open" }));
  console.log("PASS: supporter cannot release seats on a timeslot");

  const batch = admin.batch();
  batch.update(admin.collection("bookings").doc("booking-ledger"), { status: "cancelled", cancelledBy: "admin" });
  batch.update(admin.collection("timeslots").doc("slot-ledger"), { bookedCount: 8, status: "open" });
  await assertSucceeds(batch.commit());
  console.log("PASS: admin can cancel a booking and release its seats together");
}

// ---- Supporter cancel / reschedule ----
async function supporterChangeTests() {
  const firebase = require("firebase/compat/app");
  require("firebase/compat/firestore");
  const { Timestamp, FieldValue } = firebase.firestore;
  const dayMs = 24 * 3600 * 1000;
  const inDays = (n) => Timestamp.fromDate(new Date(Date.now() + n * dayMs));
  const nextWeek = inDays(7);
  const nextFortnight = inDays(14);
  const earlierToday = inDays(-0.1);

  async function seed() {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const db = context.firestore();
      await db.collection("timeslots").doc("slot-a").set({
        date: nextWeek, startTime: "10:00", capacity: 4, bookedCount: 4, status: "full", lastBookingId: "b-sam",
      });
      await db.collection("timeslots").doc("slot-b").set({
        date: nextFortnight, startTime: "14:00", capacity: 4, bookedCount: 1, status: "open", lastBookingId: "x",
      });
      await db.collection("timeslots").doc("slot-past").set({
        date: earlierToday, startTime: "16:00", capacity: 4, bookedCount: 0, status: "open", lastBookingId: "x",
      });
      await db.collection("bookings").doc("b-sam").set({
        supporterId: "supporter-1", timeslotId: "slot-a", slotDate: nextWeek, slotStart: "10:00",
        numberOfGuests: 2, status: "confirmed",
      });
    });
  }
  const sam = testEnv.authenticatedContext("supporter-1", {}).firestore();
  const eve = testEnv.authenticatedContext("someone-else", {}).firestore();
  const booking = (db) => db.collection("bookings").doc("b-sam");
  const slot = (db, id) => db.collection("timeslots").doc(id);

  function reschedule(db, toSlot, toDate, toStart, newCount, newStatus) {
    const batch = db.batch();
    batch.update(booking(db), {
      timeslotId: toSlot, slotDate: toDate, slotStart: toStart, rescheduledAt: FieldValue.serverTimestamp(),
    });
    batch.update(slot(db, "slot-a"), { bookedCount: 2, status: "open", lastBookingId: "b-sam" });
    batch.update(slot(db, toSlot), { bookedCount: newCount, status: newStatus, lastBookingId: "b-sam" });
    return batch.commit();
  }

  function cancel(db, released) {
    const batch = db.batch();
    batch.update(booking(db), { status: "cancelled", cancelledBy: "supporter", cancelledAt: FieldValue.serverTimestamp() });
    batch.update(slot(db, "slot-a"), { bookedCount: released, status: "open", lastBookingId: "b-sam" });
    return batch.commit();
  }

  await seed();
  await assertSucceeds(reschedule(sam, "slot-b", nextFortnight, "14:00", 3, "open"));
  console.log("PASS: supporter can move their booking to another open slot");

  await seed();
  await assertFails(reschedule(eve, "slot-b", nextFortnight, "14:00", 3, "open"));
  console.log("PASS: nobody else can move a supporter's booking");

  await seed();
  await assertFails(reschedule(sam, "slot-b", nextFortnight, "14:00", 1, "open"));
  console.log("PASS: reschedule must take the seats on the new slot");

  await seed();
  await assertFails(reschedule(sam, "slot-b", nextWeek, "14:00", 3, "open"));
  console.log("PASS: booking date must match the new slot");

  await seed();
  await assertFails(reschedule(sam, "slot-past", earlierToday, "16:00", 2, "open"));
  console.log("PASS: cannot move a booking onto a slot in the past");

  await seed();
  await assertSucceeds(cancel(sam, 2));
  console.log("PASS: supporter can cancel their booking and release its seats");

  await seed();
  await assertFails(cancel(sam, 0));
  console.log("PASS: cancelling cannot release more seats than the booking held");

  await seed();
  await assertFails(booking(sam).update({
    status: "cancelled", cancelledBy: "supporter", cancelledAt: FieldValue.serverTimestamp(),
  }));
  console.log("PASS: cancelling must release the seats in the same write");

  await seed();
  await assertFails(booking(sam).update({ numberOfGuests: 8 }));
  console.log("PASS: supporter cannot change guests or price on an existing booking");

  await seed();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("bookings").doc("b-sam").update({ slotDate: earlierToday });
  });
  await assertFails(cancel(sam, 2));
  console.log("PASS: bookings can't be changed on the day of the visit");
}

runTests().catch((err) => {
  console.error("Test failed:", err);
  process.exit(1);
});