# Deployment lifecycle

Cat Heaven has two deployable parts: the **Firestore backend** (security rules and
indexes) and the **Android app** (an APK). Both go through the same GitHub Actions
pipeline in `.github/workflows/android-ci.yml`, and nothing is deployed unless every
test passes first.

```
feature/* ──PR──► develop ──PR──► main ──tag vX.Y.Z──► release
   │                │               │                     │
   └─ tests ────────┴─ tests ───────┤                     │
                                    ├─ deploy rules ──► Firebase (catheaven-dev, africa-south1)
                                    └─────────────────────┴─ signed APK ──► GitHub Releases
```

## Pipeline stages

| Stage | Runs on | What it does |
|---|---|---|
| `build-and-test` | every push and PR | Kotlin unit tests (booking, pricing, profile and content rules), debug APK, Android lint; uploads reports and the APK as artifacts |
| `firestore-rules-tests` | every push and PR | Starts the Firestore emulator and runs `rules.test.js` (bookings, profiles, admin content, ledger) |
| `deploy-firestore` | push to `main` only, after both test jobs | Deploys `firestore.rules` and `firestore.indexes.json` with the Firebase CLI, authenticated by a service account |
| `release-apk` | version tags `v*` only, after both test jobs | Builds a release APK signed with the Cat Heaven key, versioned from the tag and run number, and publishes it as a GitHub Release |

Both deploy jobs use the `production` GitHub environment, so every deployment is
listed under the repository's **Environments / Deployments** view.

## Releasing a new version

1. Merge the finished work into `main` through a pull request. The backend deploys automatically.
2. Tag the merge commit and push the tag:
   ```bash
   git checkout main && git pull
   git tag v1.1.0
   git push origin v1.1.0
   ```
3. When the run finishes, the APK is on the **Releases** page. Each release has a higher
   `versionCode` (the run number), so it installs over the previous one.

## One-time setup (repository secrets)

Add these under **Settings > Secrets and variables > Actions**.

**Backend deploy**

- `FIREBASE_SERVICE_ACCOUNT`: the full JSON key of a service account in the
  `catheaven-dev` project. Create it in Google Cloud Console > IAM > Service accounts,
  give it the **Firebase Rules Admin** and **Cloud Datastore Index Admin** roles, then
  create a JSON key. Never commit this file (`serviceAccountKey*.json` is git-ignored).

**Release signing**

Create the signing key once and keep a backup somewhere safe. If it is lost, future
releases can't update installed copies of the app.

```bash
keytool -genkeypair -v -keystore catheaven-release.jks -alias catheaven \
  -keyalg RSA -keysize 2048 -validity 10000
base64 -w 0 catheaven-release.jks > keystore.b64   # on Windows: certutil -encode, then remove the header/footer lines
```

- `CATHEAVEN_KEYSTORE_BASE64`: contents of `keystore.b64`
- `CATHEAVEN_KEYSTORE_PASSWORD`: the keystore password
- `CATHEAVEN_KEY_ALIAS`: `catheaven`
- `CATHEAVEN_KEY_PASSWORD`: the key password

If a secret is missing, the job fails with a message naming it, rather than publishing
something unsigned.

## Rolling back

- **Rules or indexes:** revert the commit on `main` (through a PR). The pipeline tests
  and redeploys the previous version. In an emergency, an earlier ruleset can also be
  republished from Firebase Console > Firestore > Rules > history.
- **App:** delete or mark the faulty GitHub Release as a pre-release, and point supporters
  to the previous release. Then fix forward and tag a new version, because Android won't
  install an APK with a lower `versionCode` over a newer one.

## Data that is not deployed by the pipeline

- Admin accounts: `node setAdmin.js` sets the `admin` custom claim (run manually, needs the service account key).
- Initial menu: `node seedMenu.js` copies the default café and gift shop items into `menuItems` once.
- Cats, menu items, timeslots and prices are business data, managed by admins in the app.

## Monitoring after release

- **Crashlytics** (free on the Spark plan) reports every crash from supporters' phones,
  grouped by cause, with the app version and device. Open Firebase Console > Crashlytics.
  The first report appears after the app is opened once with Crashlytics enabled.
- Handled errors that supporters see on screen (no connection, permission denied, a failed
  save) are also sent as **non-fatal** issues through `util/Diagnostics.kt`, so a problem
  that never crashes the app still shows up.
- Each report carries the `versionName` from the release tag, so a spike can be traced to
  the release that introduced it and rolled back as described above.
