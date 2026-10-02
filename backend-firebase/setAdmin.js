const { initializeApp, cert } = require("firebase-admin/app");
const { getAuth } = require("firebase-admin/auth");
const serviceAccount = require("./serviceAccountKey.json");

initializeApp({
  credential: cert(serviceAccount),
});

// The UID of Admin
const uid = "nGGrNlnTbaWSgX55bl5pVwAuZfI3";

getAuth()
  .setCustomUserClaims(uid, { admin: true })
  .then(() => {
    console.log(`Success: ${uid} is now an admin.`);
    process.exit(0);
  })
  .catch((error) => {
    console.error("Error setting custom claim:", error);
    process.exit(1);
  });