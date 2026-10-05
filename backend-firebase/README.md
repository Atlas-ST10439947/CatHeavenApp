Hosting Deployment and Stability Rationale

The Cat Heaven system is hosted entirely on Google Firebase, using Cloud Firestore for the database and Firebase Hosting or authentication for supporting infrastructure. This choice was made deliberately in Task 1 and remains justified for Task 2's implementation:

No servers to maintain. Cat Heaven is a nonprofit with no dedicated IT staff. A fully serverless architecture means there is no operating system to patch, no server to monitor for uptime, and no database infrastructure to manually administer. It means that Google manages the entire backend, which is critical for a client with no technical team to hand this over to.

High reliability by default. Firestore and Firebase Hosting run on Google's global cloud infrastructure, which allows for automatic scaling and high uptime without any manual configuration from our team. This satisfies the deployment reliability expected of a client ready system without requiring custom server management.

Cost stability for a nonprofit client. As shown in our Task 1 Running Costs analysis, Firebase's free tier comfortably covers Cat Heaven's expected usage (22 cats, a small number of daily bookings) at R0/month, with costs scaling gradually and predictably only if usage grows significantly beyond that. Budget alerts are configured to notify the client before any charges are incurred.

Security Rules enforce stability at the data layer. Because all read and write access is controlled through Firestore Security Rules (deployed and tested — see Security section), the system prevents invalid or unauthorized writes from corrupting data, which is itself a form of operational stability independent of server uptime.

Deviation from Task 1 plan: Task 1 originally planned a region in europe-west for lowest latency to South African users. Since Task 1 was written, Firestore introduced a South Africa region, which we adopted instead for genuinely lower latency — a deliberate, informed technical improvement over the original plan rather than a deviation driven by necessity.