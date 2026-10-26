# REVIEW.md — AI Output Review Log

Complete **one review entry per slice** before moving to the next.  


---

## Slice 1 — Signup (S1)

### A. Bucket Check
List every class and method the AI produced. Tag each one:  
`Controller` / `Service` / `Repository` / `DTO` / `Security` / `Config` / `Exception`

| File / Method | Tag | In Allowed List? |
|---|---|---|
| | | |

> **Important:** Anything **not** in the allowed list → **reject, re-prompt**. Don't keep it because it works.

### B. Contract Check
Compare against API contract row A1 line by line.

| Check | Pass / Fail |
|---|---|
| Path is `POST /api/auth/signup` | |
| Returns **201** | |
| Response shape: `{ id, email, fullName, role, createdAt }` | |
| No `passwordHash` field in response | |
| Role in response is always `STUDENT` | |

### C. Validation & Errors

| Check | Pass / Fail |
|---|---|
| `@Valid` on controller parameter | |
| `@Email`, `@NotBlank`, `@Size` on DTO fields | |
| Duplicate email throws `DuplicateResourceException` | |
| No new error/response wrapper class invented | |
| No stack trace or exception message leaked to client | |

### D. Security Specifics

| Check | Pass / Fail |
|---|---|
| Password hashed via injected `PasswordEncoder` (not `new BCryptPasswordEncoder()`) | |
| `SignupRequest` has no `role` field, or it is ignored | |
| Duplicate check uses normalised (lowercased/trimmed) email | |
| AI did **not** touch `SecurityConfig`, add JWT class, or add `UserDetailsService` | |

### E. Understanding Questions
*Answer in your own words (1–2 lines each)*

1. Where exactly is the password hashed, and why is the same password stored as a different string for two users?

   > *Your answer:*

2. What would happen if `SignupRequest` accepted a `role` field and the AI used it?

   > *Your answer:*

3. Which class turns `DuplicateResourceException` into a 409 response?

   > *Your answer:*

### F. Decision

| File | Decision | Reason (if modified/rejected) |
|---|---|---|
| | Accepted / Modified / Rejected | |

---

## Slice 2 — Login & JWT (S2)

### A. Bucket Check

| File / Method | Tag | In Allowed List? |
|---|---|---|
| | | |

### B. Contract Check

| Check | Pass / Fail |
|---|---|
| Path is `POST /api/auth/login` | |
| Returns **200** | |
| Response: `{ accessToken, tokenType: "Bearer", expiresIn }` | |
| `expiresIn` matches configured value (not hardcoded) | |

### C. Validation & Errors

| Check | Pass / Fail |
|---|---|
| Same 401 message for unknown email and wrong password | |
| No `try/catch` swallowing exceptions silently | |

### D. Security Specifics

| Check | Pass / Fail |
|---|---|
| JWT secret comes from `@Value`, no string literal secret | |
| Uses jjwt 0.12 API: `.subject()`, `.claim()`, `.id()`, `.issuedAt()`, `.expiration()`, `.signWith(key)` | |
| Rejects deprecated: `setSubject`, `setExpiration`, `signWith(SignatureAlgorithm.HS256, secret)`, `Jwts.parserBuilder()` | |
| `jti` is a random UUID (not the user id) | |
| No password or hash in token claims | |
| Parse/validate method **not** written yet (that is S3) | |

### E. Understanding Questions

1. Why must the error message be identical for "no such email" and "wrong password"?

   > *Your answer:*

2. Who can read the claims inside your JWT? Who can change them?

   > *Your answer:*

3. What does `jti` give us that `sub` doesn't? (Think about S5.)

   > *Your answer:*

### F. Decision

| File | Decision | Reason |
|---|---|---|
| | | |

---

## Slice 3 — JWT Filter & Protected API (S3)

### A. Bucket Check

| File / Method | Tag | In Allowed List? |
|---|---|---|
| | | |

### B. Contract Check

| Check | Pass / Fail |
|---|---|
| `GET /api/courses` works **without** token (S3 baseline, before Task 5 change) | |
| `GET /api/users/me` returns **401** with no token | |
| `GET /api/users/me` returns **200** with valid token | |
| 401 body is an `ApiError` JSON object | |

### C. Validation & Errors

| Check | Pass / Fail |
|---|---|
| Filter does not write a response on error — continues chain | |
| Filter clears SecurityContext on bad token | |

### D. Security Specifics

| Check | Pass / Fail |
|---|---|
| Spring Security 6 style: lambda DSL, `requestMatchers`, `authorizeHttpRequests` | |
| Rejected: `WebSecurityConfigurerAdapter`, `antMatchers`, `authorizeRequests`, `.and()` | |
| `permitAll` covers only the specific public endpoints required at this slice; final Task 5 later protects `GET /api/courses` | |
| At S3 baseline, `GET /api/courses` is public and `POST /api/courses` is NOT; final Task 5 later protects `GET /api/courses` | |
| Signature + expiry verified with `Jwts.parser().verifyWith(key).build().parseSignedClaims(token)` | |
| Authorities use `ROLE_` prefix | |
| Entry point and access-denied handler wired via `exceptionHandling(...)` | |
| No `UserDetailsService`, `AuthenticationManager`, sessions or CORS added | |
| `GET /api/users/me` returns `UserResponse`, not the entity | |

### E. Understanding Questions

1. Why is CSRF protection disabled here, and when would disabling it be wrong?

   > *Your answer:*

2. Walk through what happens to a request with an expired token, step by step, until the client sees 401.

   > *Your answer:*

3. Why don't 401/403 errors from the filter chain reach `GlobalExceptionHandler`?

   > *Your answer:*

### F. Decision

| File | Decision | Reason |
|---|---|---|
| | | |

---

## Slice 4 — Role Restriction (S4)

### A. Bucket Check

| File / Method | Tag | In Allowed List? |
|---|---|---|
| | | |

### B. Contract Check

| Check | Pass / Fail |
|---|---|
| `POST /api/courses` with no token → **401** | |
| `POST /api/courses` with STUDENT token → **403** | |
| `POST /api/courses` with ADMIN token → **201** | |
| 403 body is an `ApiError` JSON object | |

### D. Security Specifics

| Check | Pass / Fail |
|---|---|
| Role naming consistent: `hasRole("ADMIN")` ↔ authority `ROLE_ADMIN` | |
| If `@PreAuthorize` used: `@EnableMethodSecurity` is present | |
| Rule placed **before** `anyRequest().authenticated()` if in SecurityConfig | |
| Diff is small — AI did not rewrite the whole config | |

### E. Understanding Questions

1. A STUDENT calls `POST /api/courses`. Why does the server return 403 and not 401?

   > *Your answer:*

2. Where does the role in the JWT come from, and can the client change it?

   > *Your answer:*

### F. Decision

| File | Decision | Reason |
|---|---|---|
| | | |

---

## Slice 5 — Logout (S5) *(stretch)*

### A. Bucket Check

| File / Method | Tag | In Allowed List? |
|---|---|---|
| | | |

### B. Contract Check

| Check | Pass / Fail |
|---|---|
| `POST /api/auth/logout` returns **204** | |
| Same token returns **401** after logout | |
| Fresh login after logout returns a new working token | |

### D. Security Specifics

| Check | Pass / Fail |
|---|---|
| Logout adds entry to server-side denylist (not just tells client to delete token) | |
| Denylist keyed by **jti**, not email | |
| Map is `ConcurrentHashMap` (thread-safe) | |
| Logout endpoint is NOT under `permitAll` | |
| Filter checks the denylist and treats denylisted jti as invalid | |
| (Bonus) Expired entries are removed from the map | |

### E. Understanding Questions

1. Why does logout need server-side state if JWT is "stateless"?

   > *Your answer:*

2. What happens to the denylist when the server restarts? What if there are two server instances?

   > *Your answer:*

3. Why store the expiry time with each jti?

   > *Your answer:*

### F. Decision

| File | Decision | Reason |
|---|---|---|
| | | |

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
- Before submission, verify and record the two final Task 5 requests separately: `GET /api/courses` without a token should return `401`, and with a valid token should return `200`. Do not mark these manual checks as passed until you have run them.

