# Cat Heaven Booking App

An Android app and Firebase backend for Cat Heaven, a South African Rescue Cat Cafe in Somerset West. Supporters can browse the resident cats and the cafe menu and book visit or snack time slots online, and staff manage bookings, timeslots, content and users from an admin area. It replaces the current manual WhatsApp booking process.

Built by Echo Solutions for INSY7315 (Task 2: Code and Implementation).

### App Presentation & Demo Video Link
https://youtu.be/KVWhCGPmrSw 

> All cat, shop and menu data is placeholder data pending final confirmation from Cat Heaven. An administrator can edit it at any time without any structural change.

## Contents
- [Team and responsibilities](#team-and-responsibilities)
- [Team attendance and communication](#team-attendance-and-communication)
- [Features and screens](#features-and-screens)
- [Tech stack](#tech-stack)
- [Repository structure](#repository-structure)
- [Getting started](#getting-started)
- [Test accounts](#test-accounts)
- [Backend: database](#backend-database)
- [Backend: security](#backend-security)
- [Backend: authentication](#backend-authentication)
- [Hosting: deployment and stability rationale](#hosting-deployment-and-stability-rationale)
- [DevOps: branching and CI](#devops-branching-and-ci)
- [Deviations from the Task 1 plan](#deviations-from-the-task-1-plan)
- [Evidence](#evidence)
- [Hosting Deployment and Stability Rationale](#Hosting Deployment and Stability Rationale)

## Team and responsibilities
| Member | Main responsibilities |  
| Amelia Swart (Atlas)  - ST10439947 | Front end: visual design, UX and feedback, responsiveness and accessibility; presentation and demo |  
| Courtney Martin - ST10439401 | Database, security rules, authentication, Firebase hosting and deployment rationale |  
| Wiehan Smit - ST10446545 | Booking logic and data flow, APIs, programming structure, CI/CD pipeline |  

## Team attendance and communication

We worked as a team of three (Amelia, Courtney and Wiehan) and attended meetings to communicate throughout Task 2. Task 1 attendance is recorded in Appendix B of the Task 1 document.

### How we split the work
At the start of Task 2 we divided the rubric between us in a shared work-split sheet (`Cat Heaven Task 2 Work Split`) and confirmed it in our team WhatsApp group chat on 22/09/2026
- Amelia: front end visual design, UX and feedback, responsiveness and accessibility and the presentation and demo
- Courtney: database, security rules, authentication, and Firebase hosting and deployment rationale
- Wiehan: booking logic and data flow, APIs and the CI/CD pipeline

Each of us also contributes to the branching marks by working on feature branches and merging through pull requests.

### How we communicated
We used the WhatsApp group chat for coordination, for example:
- Amelia published the UI and navigation and left `TODO` comments in the code marking where each member's backend, authentication and logout code should go.
- We agreed to focus on the Android app only, instead of also building a website booking page.
- Pull requests are reviewed by a teammate before they are merged into `main`.

### Attendance record
| Date | Session type | Medium | Amelia | Courtney | Wiehan |   
| `22/09/20206` | Part 2 starting meeting | In person | Present | Present | Present |  
| `<28/09/2026>` | Internal group meeting | online | Present | Present | Present |   
| `<1/10/2026>` | Internal group meeting | online | Present | Present | Present |   


## Features and screens
Supporters
- Register, log in and log out
- Browse the resident cats and view a cat's profile
- Browse the cafe menu
- Book a visit: choose a date, choose a timeslot, enter details, confirm
- View upcoming and past bookings
- Settings: profile and notification preferences

Admins (a separate admin area, reached automatically after an admin logs in)
- View and manage all bookings
- Manage the timeslot schedule
- Manage content (cats and menu)
- View users

## Tech stack
- App: Kotlin, Android (minSdk 24, target and compile SDK 37), View Binding, Navigation component (separate graphs for launch, main app and admin), Material components, RecyclerView
- Domain layer: plain Kotlin models (`Booking`, `Cat`, `Timeslot`, `PricingTier`, `User`) and a `BookingValidator`, covered by unit tests
- Backend: Firebase Authentication, Cloud Firestore, Firestore Security Rules, Firebase Hosting
- Build and CI: Gradle (Android Gradle Plugin 9.3.3), GitHub Actions

## Repository structure
| Path | Purpose |  
| `app/` | The Android application |  
| `app/google-services.json` | Firebase config for the app |  
| `backend-firebase/` | Firestore rules, indexes, rules tests, admin script and hosting page |  
| `backend-firebase/docs/evidence/` | Screenshots used in this README |  
| `.github/workflows/android-ci.yml` | CI pipeline |  

## Getting started

Install the app 
1. On GitHub, open Actions, then the latest green Cat Heaven CI run on `main`.
2. Under Artifacts, download `catheaven-apk` and unzip it.
3. Copy the `.apk` to an Android phone (Android 7.0 or newer) and open it. If asked, allow installing from this source. Alternatively, with a phone connected over USB: `adb install app-debug.apk`.
4. Log in with one of the test accounts.

Build from source (developers)
1. Open the project in a current Android Studio (it must support Android Gradle Plugin 9.3.3) and let Gradle sync.
2. Run on an emulator or a physical device.
From the command line
bash
./gradlew testDebugUnitTest     # unit tests
./gradlew assembleDebug         # build the debug APK


Backend (run from `backend-firebase`)
bash
npm install
firebase login
firebase use dev

# Security Rules tests (needs Java for the emulator)
firebase emulators:start --only firestore     # terminal 1
node rules.test.js                            # terminal 2

# Deploy
firebase deploy --only firestore:rules
firebase deploy --only firestore:indexes
firebase deploy --only hosting


## Distribution and go-live
- Now (Task 2): the backend is hosted on Firebase, and the app is delivered as a debug APK built by CI and installable on any Android 7.0+ device, which is how it is demonstrated to the client.
- After client approval: the app will be published on Google Play under Cat Heaven's own developer account, so the non-profit owns the store listing. The plan from Task 1 applies: internal testing, then closed testing with Cat Heaven staff, then a staged production rollout.

## Test accounts
Dev-only accounts for the `catheaven-dev` Firebase project.

| Role | Email | Password | Lands on |  
| Admin | `admin@test.com` | `Test1234!` | Admin screens |  
| Supporter | `supporter@test.com` | `P123456/` | Main app |  

## Backend: database
Cloud Firestore (Standard edition) in the South Africa region, which became available after Task 1 was written and gives lower latency for local users than the originally planned europe-west.

| Collection | Purpose | Notes |  
| `cats` | Resident cat profiles | `updates` subcollection holds each cat's news posts |  
| `timeslots` | Bookable visit and snack-time slots | Holds `capacity` and `bookedCount` |  
| `pricingTiers` | Visit durations and price per person | Quoted price only, no payments are stored |  
| `categories` / `products` | Gift shop display | Display only, no online purchasing |  
| `menuItems` | Cafe menu | Display only |  
| `users` | One profile per Firebase Auth account | Document ID equals the Auth UID |  
| `bookings` | Visit bookings | Created by the app at runtime |  
| `pawParentEnrolments` | Supporter-to-cat sponsorship links | Created by the app at runtime |  

Design decisions:
- Denormalisation: fields such as `categoryName` on a product and `supporterName` on a booking are copied so list screens need one query instead of several joins.
- Top-level collections for bookings and enrolments so administrators can query across all of them. Cat updates are a subcollection because they are always read in the context of one cat.
- Composite indexes (in `firestore.indexes.json`): bookings by `slotDate` + `status`, bookings by `supporterId` + `createdAt`, and enrolments by `catId` + `status`.
- No financial data is stored. Donations go through Cat Heaven's existing external channels, and `totalDue` on a booking is a quoted amount payable at the venue.

## Backend: security
All access is decided server-side by `backend-firebase/firestore.rules`, and client code is treated as untrusted.

| Collection | Anonymous | Signed-in supporter | Admin |  
| `cats`, `timeslots`, `pricingTiers`, `categories`, `products`, `menuItems` | Read | Read | Read and write |  
| `users` | None | Read and update own profile only | Read any profile |  
| `bookings` | None | Create own (1 to 8 guests), read own | Read, update, delete all |  
| `pawParentEnrolments` | None | Create own, read own, cancel own | Full access |  

Key points:
- Admin rights come from a custom claim (`admin: true`) set with the Firebase Admin SDK (`setAdmin.js`), not from a field in the database. A client cannot grant itself admin.
- Users cannot self-assign a role: a profile can only be created with `role == 'supporter'`, and `role` cannot be changed by the owner afterwards.
- Group size cap: a booking must have between 1 and 8 guests, enforced in the rules, not only in the UI.
- Automated tests in `rules.test.js` cover: anonymous users can read cats, non-admins cannot edit cats, admins can edit cats, non-admins cannot read other users' profiles, admins can, a new user cannot register as admin, and a new user can register as a supporter.

## Backend: authentication
Firebase Authentication with email and password.
- Register creates the Auth account and a matching `users/{uid}` document with `role: "supporter"`.
- Login signs in, then reads the admin claim from the ID token: admins are sent to the admin screens and everyone else to the main app.
- Logout (Settings and the admin screen) signs the user out and returns to the launch screen.

## Hosting: deployment and stability rationale
The system runs on Google Firebase (Firestore and Authentication on Google Cloud), with no servers for Cat Heaven to maintain. The Android app talks to these managed services directly.

No servers to maintain. Cat Heaven is a non-profit with no IT staff. There is no operating system to patch, no server to monitor and no database to administer. Google runs the whole backend.

Handling peak cafe traffic. Demand is expected to spike on weekends and public holidays. The Spark (free) plan allows 50,000 document reads and 20,000 writes per day. As an illustrative estimate, if 200 supporters used the app on one busy day at roughly 100 reads and 5 writes each, that is about 20,000 reads and 1,000 writes, well under the daily limits. Firestore scales automatically, so there is nothing to resize for a spike. A booking updates one timeslot document, and Firestore comfortably sustains the roughly one write per second per document that a cafe slot filling up requires. Concurrent bookings of the same slot are handled by transaction retries rather than overbooking.

Cost stability. The project runs on the free Spark plan with no charges. Budget alerts will be configured if the client ever upgrades to the Blaze plan.

Reliability and data safety. Firestore and Hosting run on Google's global infrastructure with automatic scaling. The Security Rules block invalid or unauthorised writes, which protects data integrity independently of server uptime.

Deployment. Rules, indexes and Hosting are deployed with the Firebase CLI (`firebase deploy`). Because the client is Android-only, the Hosting page is a minimal placeholder that verifies the pipeline.

## DevOps: branching and CI
- Branching: all work happens on feature branches and reaches `main` through pull requests. There are no direct commits to `main`.
- CI (GitHub Actions, on every pull request and push to `main`):
  - Android unit tests, debug APK build and lint
  - Firestore Security Rules tests, run against the local Firestore emulator
  - Test reports, lint report and the debug APK are uploaded as build artifacts

## Deviations from the Task 1 plan
- Android-only client. The booking web app on a `book.catheaven.co.za` subdomain was dropped, so Hosting is a placeholder page.
- No Cloud Functions (free plan). Booking confirmation emails and push notifications to Paw Parents are not implemented, because Cloud Functions require the Blaze plan and a billing card was not available.
- Images are bundled in the app. Cloud Storage also needs the Blaze plan, so cat images ship inside the Android app instead of being stored in Firebase.
- Single Firebase project. Only `catheaven-dev` exists. A separate `catheaven-prod` project would be created before a real launch.
- South Africa region is used instead of europe-west, since it became available after Task 1.

## Hosting Deployment and Stability Rationale

The Cat Heaven system is hosted entirely on Google Firebase, using Cloud Firestore for the database and Firebase Hosting or authentication for supporting infrastructure. This choice was made deliberately in Task 1 and remains justified for Task 2's implementation:

No servers to maintain. Cat Heaven is a nonprofit with no dedicated IT staff. A fully serverless architecture means there is no operating system to patch, no server to monitor for uptime, and no database infrastructure to manually administer. It means that Google manages the entire backend, which is critical for a client with no technical team to hand this over to.

High reliability by default. Firestore and Firebase Hosting run on Google's global cloud infrastructure, which allows for automatic scaling and high uptime without any manual configuration from our team. This satisfies the deployment reliability expected of a client ready system without requiring custom server management.

Cost stability for a nonprofit client. As shown in our Task 1 Running Costs analysis, Firebase's free tier comfortably covers Cat Heaven's expected usage (22 cats, a small number of daily bookings) at R0/month, with costs scaling gradually and predictably only if usage grows significantly beyond that. The project currently runs on the free Spark plan with no charges so the budget alerts will be configured if the client upgrades to the Blaze plan.

Security Rules enforce stability at the data layer. Because all read and write access is controlled through Firestore Security Rules (deployed and tested — see Security section), the system prevents invalid or unauthorized writes from corrupting data, which is itself a form of operational stability independent of server uptime.

Deviation from Task 1 plan: Task 1 originally planned a region in europe-west for lowest latency to South African users. Since Task 1 was written, Firestore introduced a South Africa region, which we adopted instead for genuinely lower latency — a deliberate, informed technical improvement over the original plan rather than a deviation driven by necessity.

