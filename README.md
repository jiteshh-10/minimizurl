# MinimizUrl Backend: Technical Specifications and Architectural Design

This document delineates the technical implementation of the MinimizUrl backend, a production-grade URL shortening service. The system is engineered from first principles, prioritizing data isolation, stateless identity management, and referential integrity within a distributed NoSQL environment.

## 1. System Architecture and Identity Theory

The architectural cornerstone of this system is the immutability of identity. While attributes such as usernames and emails remain mutable, the **UserID** (MongoDB ObjectId) serves as the definitive primary key for all ownership logic. This design choice mitigates the risk of "dangling references"—a common failure mode where a user modifies their username but consequently loses access to previously associated resources.

Security is enforced via a stateless JWT "Passport" mechanism. Upon successful authentication (leveraging either Bcrypt-hashed local credentials or Google OAuth2), the system issues a JWT where the `sub` (subject) claim is the 24-character Hex ID of the user. This ensures that throughout the request lifecycle, the `SecurityContext` provides a unique, persistent identifier used for high-performance database lookups.



## 2. Data Modeling and Schema Design

The persistence layer utilizes MongoDB, structured around two primary collections: `users` and `url_mappings`.

### User Schema
* **Identity**: `id` (ObjectId) as the immutable primary key.
* **Credentials**: `password` (Bcrypt hash) for local users; `googleId` for OAuth2 principals.
* **Metadata**: `email`, `username`, `roles`, and `provider` (LOCAL/GOOGLE).
* **Recovery Lifecycle**: `passwordResetToken` and `passwordResetTokenExpiry` for secure, time-bound credential recovery.

### URL Mapping Schema
* **Sequential Key**: `id` (Long) generated via a thread-safe `DatabaseSequence` collection to ensure global uniqueness.
* **Encoding**: The numerical `id` is transformed into a Base62 string (`[0-9][a-z][A-Z]`) to generate the shortened URI.
* **Ownership**: `userId` (String) is indexed for optimized retrieval of user-specific link sets.
* **TTL (Time-To-Live)**: `expirationDate` field is indexed with `expireAfterSeconds: 0`, enabling automatic database-level document eviction.

## 3. URL Resolution and Asynchronous Analytics

The URL shortening pipeline follows a deterministic execution path:
1. An atomic increment is performed on the sequence generator.
2. The integer is processed by the `ShorteningService` for Base62 encoding.
3. If a `customCode` is provided, a uniqueness collision check is executed prior to persistence.

Redirection logic is optimized for minimal latency. The `UrlController` resolves the `shortCode` by querying the custom code index first, falling back to ID decoding if no match is found. Analytics—including click counts, referrers, and device telemetry—are processed via the `@Async` executor. This ensures that write-heavy analytical operations do not block the primary redirect execution thread.

## 4. Authentication Lifecycle and Security Rigor

### Dual-Mode Authentication
The system implements a unified authentication provider. The `CustomUserDetailsService` is capable of loading user principals by their unique Hex ID (for JWT validation) or their username/email (for initial login). The `OAuth2SuccessHandler` manages the "Sync or Register" logic, ensuring Google-authenticated users are assigned an internal `userId` consistent with local users.

### Secure Password Recovery
Local accounts utilize a secure, multi-step recovery flow:
* **Tokenization**: Generation of a cryptographically secure UUID.
* **Temporal Constraints**: Strict 15-minute expiration window.
* **Provider Isolation**: The system explicitly blocks reset requests for `GOOGLE` provider users to prevent state fragmentation between local and third-party identity providers.



### Data Integrity and Cascading Deletion
Given that MongoDB lacks native declarative referential integrity (cascading deletes), the system enforces this at the application layer. When a user account is deleted:
1. The system identifies all `url_mappings` where `userId == currentUserId`.
2. It deletes all associated `click_events`.
3. It removes the corresponding `url_mappings`.
4. Finally, it evicts the `user` document.
This specific sequence ensures that identity records are never removed before their dependent data, avoiding orphaned analytical artifacts.

## 5. Profile API and Information Hiding

The User Profile API is designed to provide a secure projection of the user's state. To maintain industry-grade data isolation, we utilize a `UserProfileResponse` DTO. This prevents sensitive internal state (such as password hashes or reset tokens) from being leaked over the network.

The API exposes:
* Identity metadata (Username, Email, Provider type).
* Aggregated link statistics via `countByUserId`, minimizing memory overhead compared to full list retrieval.

## 6. Technical Specifications

* **Runtime**: Java 17
* **Framework**: Spring Boot 3.4.1
* **Security**: Spring Security 6 (Stateless JWT + OAuth2)
* **Database**: MongoDB (utilizing TTL and Unique indices)
* **Cryptography**: Bcrypt (cost factor: 10)
* **Communication**: JavaMailSender (SMTP via TLS 587)
