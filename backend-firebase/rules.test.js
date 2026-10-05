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

  await testEnv.cleanup();
  console.log("All tests passed.");
}

runTests().catch((err) => {
  console.error("Test failed:", err);
  process.exit(1);
});