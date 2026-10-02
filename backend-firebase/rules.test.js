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

  await testEnv.cleanup();
  console.log("All tests passed.");
}

runTests().catch((err) => {
  console.error("Test failed:", err);
  process.exit(1);
});