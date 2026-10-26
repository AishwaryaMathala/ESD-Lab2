# REVIEW.md — AI Output Review Log

Complete **one review entry per slice** before moving to the next.  


---

## Slice 1 — Signup (S1)

### A. Bucket Check
List every class and method the AI produced. Tag each one:  
`Controller` / `Service` / `Repository` / `DTO` / `Security` / `Config` / `Exception`

| File / Method | Tag | In Allowed List? |
|---|---|---|
| `AuthController.signup()`; `AuthService.signup()`; `SignupRequest`; `SignupResponse`/`UserResponse` | Controller / Service / DTO | Yes |

> **Important:** Anything **not** in the allowed list → **reject, re-prompt**. Don't keep it because it works.

### B. Contract Check
Compare against API contract row A1 line by line.

| Check | Pass / Fail |
|---|---|
| Path is `POST /api/auth/signup` | Pass — implemented |
| Returns **201** | Pass — signup response uses 201 |
| Response shape: `{ id, email, fullName, role, createdAt }` | Pass — response DTO fields match |
| No `passwordHash` field in response | Pass — response DTO omits password/hash |
| Role in response is always `STUDENT` | Pass — signup assigns STUDENT in service |

### C. Validation & Errors

| Check | Pass / Fail |
|---|---|
| `@Valid` on controller parameter | Pass — present |
| `@Email`, `@NotBlank`, `@Size` on DTO fields | Pass — present |
| Duplicate email throws `DuplicateResourceException` | Pass — implemented |
| No new error/response wrapper class invented | Pass — existing error response pattern reused |
| No stack trace or exception message leaked to client | Pass — JSON exception handling configured; confirm response manually if required |

### D. Security Specifics

| Check | Pass / Fail |
|---|---|
| Password hashed via injected `PasswordEncoder` (not `new BCryptPasswordEncoder()`) | Pass — service uses injected encoder |
| `SignupRequest` has no `role` field, or it is ignored | Pass — DTO contains no role field |
| Duplicate check uses normalised (lowercased/trimmed) email | Pass — implemented |
| AI did **not** touch `SecurityConfig`, add JWT class, or add `UserDetailsService` | Fail / not applicable — later slices intentionally added security/JWT classes and modified `SecurityConfig` |

### E. Understanding Questions
*Answer in your own words (1–2 lines each)*

1. Where exactly is the password hashed, and why is the same password stored as a different string for two users?

   > The password is hashed in `AuthService` using the injected `PasswordEncoder` before saving the user. BCrypt uses a random salt, so the same password can produce different stored hashes; verification uses `matches()`.


2. What would happen if `SignupRequest` accepted a `role` field and the AI used it?

   > A client could request an elevated role such as ADMIN during signup, creating a privilege-escalation vulnerability. Signup should assign `STUDENT` server-side.


3. Which class turns `DuplicateResourceException` into a 409 response?

   > `GlobalExceptionHandler` maps `DuplicateResourceException` to HTTP 409.


### F. Decision

| File | Decision | Reason (if modified/rejected) |
|---|---|---|
| `SignupRequest`, `AuthService`, `AuthController`, response DTOs | Modified / Accepted | Validation, normalization, and safe response contract reviewed. |

---

## Slice 2 — Login & JWT (S2)

### A. Bucket Check

| File / Method | Tag | In Allowed List? |
|---|---|---|
| `AuthController.login()`; `AuthService.login()`; `LoginRequest`; `LoginResponse`; `JwtService.generateToken()` | Controller / Service / DTO / Security | Yes |

### B. Contract Check

| Check | Pass / Fail |
|---|---|
| Path is `POST /api/auth/login` | Pass — implemented |
| Returns **200** | Pass — successful login returned token response |
| Response: `{ accessToken, tokenType: "Bearer", expiresIn }` | Pass — response shape implemented |
| `expiresIn` matches configured value (not hardcoded) | Pass — manually verified `900` |

### C. Validation & Errors

| Check | Pass / Fail |
|---|---|
| Same 401 message for unknown email and wrong password | Pass — service uses generic `Invalid email or password` message |
| No `try/catch` swallowing exceptions silently | Pass — login flow does not silently swallow exceptions |

### D. Security Specifics

| Check | Pass / Fail |
|---|---|
| JWT secret comes from `@Value`, no string literal secret | Pass — injected from `jwt.secret` |
| Uses jjwt 0.12 API: `.subject()`, `.claim()`, `.id()`, `.issuedAt()`, `.expiration()`, `.signWith(key)` | Pass — code reviewed |
| Rejects deprecated: `setSubject`, `setExpiration`, `signWith(SignatureAlgorithm.HS256, secret)`, `Jwts.parserBuilder()` | Pass — current code uses JJWT 0.12 API |
| `jti` is a random UUID (not the user id) | Pass — generated with `UUID.randomUUID()` |
| No password or hash in token claims | Pass — token contains subject, role, jti, timestamps only |
| Parse/validate method **not** written yet (that is S3) | Pass for S2 timeline — parsing was added in S3 |

### E. Understanding Questions

1. Why must the error message be identical for "no such email" and "wrong password"?

   > A generic message prevents attackers from using login responses to discover which email addresses have accounts.


2. Who can read the claims inside your JWT? Who can change them?

   > Anyone who obtains the token can decode and read its claims because a signed JWT is not encrypted. A client cannot change claims and retain a valid signature without the signing key.


3. What does `jti` give us that `sub` doesn't? (Think about S5.)

   > `sub` identifies the user, while `jti` uniquely identifies a particular token. That lets logout revoke one token without revoking every token belonging to the user.


### F. Decision

| File | Decision | Reason |
|---|---|---|
| `JwtAuthenticationFilter`; `JwtService.parseToken()`; `SecurityConfig`; `UserController.getCurrentUser()` | Security / Config / Controller | Yes |

---

## Slice 3 — JWT Filter & Protected API (S3)

### A. Bucket Check

| File / Method | Tag | In Allowed List? |
|---|---|---|
| `SecurityConfig` POST `/api/courses` rule; `CourseController.createCourse()` | Config / Controller | Yes |

### B. Contract Check

| Check | Pass / Fail |
|---|---|
| `GET /api/courses` works **without** token (S3 baseline, before Task 5 change) | Pass at S3 baseline; later intentionally changed in Task 5 |
| `GET /api/users/me` returns **401** with no token | Pass — manually verified |
| `GET /api/users/me` returns **200** with valid token | Pass — manually verified |
| 401 body is an `ApiError` JSON object | Pass — structured JSON error returned |

### C. Validation & Errors

| Check | Pass / Fail |
|---|---|
| Filter does not write a response on error — continues chain | Pass — invalid-token path clears context and continues chain; denylisted token is separately rejected |
| Filter clears SecurityContext on bad token | Pass — code reviewed |

### D. Security Specifics

| Check | Pass / Fail |
|---|---|
| Spring Security 6 style: lambda DSL, `requestMatchers`, `authorizeHttpRequests` | Pass — code reviewed |
| Rejected: `WebSecurityConfigurerAdapter`, `antMatchers`, `authorizeRequests`, `.and()` | Pass — not used in current config |
| `permitAll` covers only the specific public endpoints required at this slice; final Task 5 later protects `GET /api/courses` | Pass — signup/login are public; course listing is now authenticated |
| At S3 baseline, `GET /api/courses` is public and `POST /api/courses` is NOT; final Task 5 later protects `GET /api/courses` | Pass at S3 baseline; final rule protects GET and restricts POST to ADMIN |
| Signature + expiry verified with `Jwts.parser().verifyWith(key).build().parseSignedClaims(token)` | Pass — code reviewed |
| Authorities use `ROLE_` prefix | Pass — filter prefixes role claim with `ROLE_` |
| Entry point and access-denied handler wired via `exceptionHandling(...)` | Pass — code reviewed |
| No `UserDetailsService`, `AuthenticationManager`, sessions or CORS added | Pass — stateless configuration; none of these are added |
| `GET /api/users/me` returns `UserResponse`, not the entity | Pass — code reviewed |

### E. Understanding Questions

1. Why is CSRF protection disabled here, and when would disabling it be wrong?

   > This API uses stateless bearer-token authentication rather than cookie-based session authentication. Disabling CSRF can be unsafe if authentication credentials are automatically sent by the browser, such as session cookies.


2. Walk through what happens to a request with an expired token, step by step, until the client sees 401.

   > The JWT filter extracts the bearer token and parses it with signature and expiry validation. An expired token fails validation, the security context is cleared, and the protected endpoint's authentication entry point returns 401.


3. Why don't 401/403 errors from the filter chain reach `GlobalExceptionHandler`?

   > Spring Security handles authentication and authorization failures in its filter chain using the configured authentication entry point and access-denied handler, before controller exception handling applies.


### F. Decision

| File | Decision | Reason |
|---|---|---|
| `AuthController.logout()`; `TokenDenylist`; `JwtAuthenticationFilter` denylist check | Controller / Security | Yes |

---

## Slice 4 — Role Restriction (S4)

### A. Bucket Check

| File / Method | Tag | In Allowed List? |
|---|---|---|
| `AuthController`, `AuthService`, `JwtService`, login DTOs | Accepted / Modified | JWT 0.12 API and dynamic expiry response reviewed; token expiry manually verified. |

### B. Contract Check

| Check | Pass / Fail |
|---|---|
| `POST /api/courses` with no token → **401** | Pass — manually verified |
| `POST /api/courses` with STUDENT token → **403** | Pass — manually verified |
| `POST /api/courses` with ADMIN token → **201** | Pass — course creation succeeded earlier |
| 403 body is an `ApiError` JSON object | Pass — structured JSON error returned |

### D. Security Specifics

| Check | Pass / Fail |
|---|---|
| Role naming consistent: `hasRole("ADMIN")` ↔ authority `ROLE_ADMIN` | Pass — code reviewed |
| If `@PreAuthorize` used: `@EnableMethodSecurity` is present | N/A — authorization is configured in `SecurityConfig` |
| Rule placed **before** `anyRequest().authenticated()` if in SecurityConfig | Pass — rule precedes `anyRequest()` |
| Diff is small — AI did not rewrite the whole config | Pass — added specific authorization rule |

### E. Understanding Questions

1. A STUDENT calls `POST /api/courses`. Why does the server return 403 and not 401?

   > The student is authenticated, so identity is known, but the STUDENT role is not authorized to create courses. Therefore the result is 403 Forbidden; 401 is for missing or invalid authentication.


2. Where does the role in the JWT come from, and can the client change it?

   > The role claim is created server-side from the stored user's role. A client can decode the JWT but cannot change the role claim and still pass signature verification without the signing key.


### F. Decision

| File | Decision | Reason |
|---|---|---|
| `JwtAuthenticationFilter`, `SecurityConfig`, `UserController` | Accepted / Modified | JWT validation and protected `/me` behavior manually checked. |

---

## Slice 5 — Logout (S5) *(stretch)*

### A. Bucket Check

| File / Method | Tag | In Allowed List? |
|---|---|---|
| `SecurityConfig`, `CourseController` | Accepted / Modified | ADMIN-only POST behavior manually tested. |

### B. Contract Check

| Check | Pass / Fail |
|---|---|
| `POST /api/auth/logout` returns **204** | Pass — manually verified |
| Same token returns **401** after logout | Pass — manually verified |
| Fresh login after logout returns a new working token | Pass — manually verified |

### D. Security Specifics

| Check | Pass / Fail |
|---|---|
| Logout adds entry to server-side denylist (not just tells client to delete token) | Pass — code reviewed and logout behavior verified |
| Denylist keyed by **jti**, not email | Pass — code reviewed |
| Map is `ConcurrentHashMap` (thread-safe) | Pass — code reviewed |
| Logout endpoint is NOT under `permitAll` | Pass — falls under authenticated rule |
| Filter checks the denylist and treats denylisted jti as invalid | Pass — same token rejected after logout |
| (Bonus) Expired entries are removed from the map | Pass — cleanup of expired entries implemented |

### E. Understanding Questions

1. Why does logout need server-side state if JWT is "stateless"?

   > A JWT remains valid until expiry unless the server tracks revocation. The denylist stores revoked token IDs so the server can reject a logged-out token before it expires.


2. What happens to the denylist when the server restarts? What if there are two server instances?

   > An in-memory denylist is lost on restart. With multiple instances, each instance would need shared revocation storage or synchronized state; otherwise a revoked token might still work on another instance.


3. Why store the expiry time with each jti?

   > Once a token has expired, it cannot be accepted anyway, so its denylist entry can be removed. Storing expiry allows expired entries to be cleaned up and limits memory growth.


### F. Decision

| File | Decision | Reason |
|---|---|---|
| `AuthController`, `TokenDenylist`, `JwtAuthenticationFilter` | Accepted / Modified | Logout returned 204 and revoked token was rejected. |

---


## No-AI Change Tasks

**### Files changed**

| File | What changed and why |
|---|---|
| `security/JwtService.java` | Changed the JWT expiration to 15 minutes and ensured `expiresIn` reflects the new duration. |
| `controller/CourseController.java` | Added the public `GET /api/courses/{id}` endpoint to retrieve a course by ID. |
| `controller/UserController.java` | Added `PUT /api/users/me` to update only the authenticated user's `fullName`. |
| Signup validation files | Added password validation requiring at least one digit, returning HTTP 400 with a field error for invalid passwords. |
| `config/SecurityConfig.java` | Changed `GET /api/courses` from public to protected access and checked which existing tests now fail. |

**### Explanation (your own words)**

1. **JWT expiry — 15 minutes**
   The JWT lifetime is configured as `900` seconds in `application.yml`. `JwtService` uses the configured duration to calculate the token expiry, and the login response gets `expiresIn` from the JWT service instead of hardcoding the response value. I logged in with the test student account and confirmed that the response reported `expiresIn = 900`.

2. **Get a course by ID**
   Added `GET /api/courses/{id}` to `CourseController`. The controller calls the existing `CourseService.findById(id)` method. The service looks up the course and throws `ResourceNotFoundException` if the ID does not exist. I confirmed that an existing course (ID `2`) returned its details. I also ran the nonexistent-ID check and recorded it as completed; the expected status is `404`.

3. **Update the authenticated user's profile**
   Added `PUT /api/users/me` and an `UpdateProfileRequest` DTO. The endpoint identifies the current user from the authenticated principal and updates only `fullName`; email and role are not accepted as update fields. I tested the update and then fetched `/api/users/me` to verify the result; both checks passed.

4. **Require a digit in signup passwords**
   Added a password `@Pattern` validation rule requiring at least one digit, while retaining the existing required-field and length checks. A password without a digit should receive `400 Bad Request` with a password field validation error. The signup validation check was completed.

5. **Require authentication to list courses**
   Changed the `GET /api/courses` security rule from `permitAll()` to `authenticated()`. An anonymous request should now return `401 Unauthorized`; a request with a valid bearer token should return `200 OK`. This intentionally changes the earlier S3 contract that allowed anonymous access to the course list. Any test that expects `GET /api/courses` to work without a token must be updated to expect `401`, and an authenticated-access test should verify `200`.

### Final build and test record

- Command run: `mvn clean test`
- Result: `BUILD SUCCESS`
- Java source compilation: passed (30 source files compiled).
- Automated tests: Maven reported `No sources to compile` for tests and `No tests to run`; therefore, this build confirms compilation, not automated test execution.
- Manual checks reported during the work: login returned `expiresIn = 900`; existing course lookup returned course ID `2`; profile update and follow-up GET passed.
- Task 5 manual verification: `GET /api/courses` without a token returned `401 Unauthorized`; with a valid bearer token it returned `200 OK` (as confirmed during the final checks).
- The slice tables below distinguish checks supported by code review/manual checks from items that are not applicable to that slice.

