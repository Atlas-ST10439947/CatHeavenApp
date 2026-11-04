// One-off: copies the café and gift shop lists that were hard-coded in the app
// into the Firestore menuItems collection, so admins can manage them in the app.
// Run from backend-firebase with the (git-ignored) service account key present:
//   node seedMenu.js
// Safe to run more than once: each item has a fixed ID, so it is overwritten, not duplicated.
const { initializeApp, cert } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const serviceAccount = require("./serviceAccountKey.json");

initializeApp({ credential: cert(serviceAccount) });
const db = getFirestore();

const menu = {
  foodAndDrink: [
    "Coffee", "Tea", "Hot Chocolate", "Water", "Tonics", "Cool Drinks",
    "Toasted Sandwiches", "Cake Slices", "Tarts", "Ice Cream", "Popcorn",
  ],
  giftShop: [
    "Books", "Arts & Crafts", "Cat Toys", "Stationery", "Decor", "Jewelry",
    "Accessories", "Framed Photos", "Scarves", "Apparel",
  ],
};

const slug = (text) => text.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "");

async function seed() {
  const batch = db.batch();
  let count = 0;
  for (const [category, names] of Object.entries(menu)) {
    for (const name of names) {
      batch.set(db.collection("menuItems").doc(slug(name)), {
        name,
        category,
        price: null, // prices can be added by an admin in the app
        isAvailable: true,
        updatedAt: FieldValue.serverTimestamp(),
      });
      count++;
    }
  }
  await batch.commit();
  console.log(`Seeded ${count} menu items.`);
}

seed().then(() => process.exit(0)).catch((error) => {
  console.error("Seeding failed:", error);
  process.exit(1);
});
