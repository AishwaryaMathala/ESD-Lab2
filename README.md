# ESD Lab 2 - Spring Boot Auth, Built with Restricted AI

**Duration:** 90 minutes  
**Prerequisites:** Lab 1 (Spring Boot REST + DB) and the Auth lecture (hashing, JWT)

> **Quick start:** Clone -> set env vars -> `./mvnw spring-boot:run` -> complete S1 manually -> use AI for S2-S5.

---

## What you are building

A small **course catalogue service** that adds authentication on top of the existing course API.

By the end you will have:
- `POST /api/auth/signup` - register with email + password
- `POST /api/auth/login` - get a JWT
- `POST /api/auth/logout` - invalidate the token server-side *(stretch)*
- `GET /api/courses` - public list of courses
- `GET /api/users/me` - protected: requires a valid token
- `POST /api/courses` - admin-only: requires `ADMIN` role

**The point of the lab is the process, not just working code.**  
You must use a structured prompt template, review every AI output

---

## API Contract

All error responses use this shape (already implemented):
```json
{
  "timestamp": "...",
  "status": 409,
  "error": "Conflict",
  "message": "Email already registered",
  "path": "/api/auth/signup",
  "fieldErrors": null
}
```

| # | Method & Path | Access | Request Body | Success | Error Codes |
|---|---|---|---|---|---|
| A1 | `POST /api/auth/signup` | Public | `{ email, password, fullName }` | 201 `UserResponse` | 400, 409 |
| A2 | `POST /api/auth/login` | Public | `{ email, password }` | 200 `LoginResponse` | 400, 401 |
| A3 | `POST /api/auth/logout` | Logged-in | none | 204 | 401 |
| P1 | `GET /api/courses` | **Public** | none | 200 list |  |
| P2 | `GET /api/users/me` | **Any logged-in user** | none | 200 `UserResponse` | 401 |
| P3 | `POST /api/courses` | **ADMIN only** | `{ code, title, description }` | 201 course | 400, 401, 403, 409 |

**DTOs you will create:**
```
SignupRequest   { email: valid email, required
                  password: required, 8-64 chars
                  fullName: required, not blank, max 100 }

UserResponse    { id, email, fullName, role, createdAt }   // no password hash!

LoginRequest    { email: required, password: required }

LoginResponse   { accessToken, tokenType: "Bearer", expiresIn: <seconds> }
```

**Rules that are part of the contract:**
- Signup always creates a `STUDENT`. Ignore any `role` field in the request.
- Store email in lowercase and trimmed.
- Login failure returns the **same** 401 message whether the email is unknown or the password is wrong: `"Invalid email or password"`.
- JWT contains: `sub` = email, `role` claim, `jti` (random UUID), `iat`, `exp`. Signed with HS256.
- After logout, the same token gets 401 on every protected endpoint.

---

## What is already provided

```text
src/main/java/com/esd/lab2/
├── Lab2Application.java
├── config/
│   ├── SecurityConfig.java       TEMPORARY: permits all. Replace in S3.
│   └── AdminSeeder.java          Seeds admin@esd.local at startup.
├── controller/
│   └── CourseController.java     GET + POST /api/courses (unprotected for now)
├── service/
│   └── CourseService.java
├── dto/
│   ├── CourseRequest.java
│   └── CourseResponse.java
├── entity/
│   ├── User.java
│   ├── Role.java                 enum: STUDENT, ADMIN
│   └── Course.java
├── repository/
│   ├── UserRepository.java       findByEmail, existsByEmail
│   └── CourseRepository.java
├── exception/
│   ├── ApiError.java
│   ├── GlobalExceptionHandler.java
│   ├── DuplicateResourceException.java    409
│   ├── InvalidCredentialsException.java   401
│   └── ResourceNotFoundException.java     404
└── security/
    ├── JsonAuthenticationEntryPoint.java   writes ApiError for 401 from filter chain
    └── JsonAccessDeniedHandler.java        writes ApiError for 403 from filter chain
```

> The `security/` package has the two handlers but **no JWT service and no filter**.  
> Those are AI-generated in S2 and S3.

**No other dependency may be added during the lab.**  
The `pom.xml` already has everything you need.

---

## Setup

### 1. Environment Variables (.env)

The application requires these variables. A template is provided in [`.env.example`](.env.example).

| Variable | Description | Example |
|---|---|---|
| `JWT_SECRET` | Base64-encoded secret, at least 32 bytes | `c2VjcmV0LWtleS1mb3ItZXNkLWxhYi0yLXNlY3VyZS1rZXk=` |
| `ADMIN_PASSWORD` | Password for the seeded admin account | `Admin1234!` |
| `DB_USER` | Postgres username (default: `postgres`) | `postgres` |
| `DB_PASS` | Postgres password (default: `postgres`) | `postgres` |

**Option A — Using .env file:**
1. Copy the example file:
   ```bash
   cp .env.example .env          # macOS / Linux
   Copy-Item .env.example .env    # PowerShell
   ```
2. Generate your own JWT secret and paste it into `.env`:
   ```bash
   # macOS / Linux
   openssl rand -base64 32

   # PowerShell
   [Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Maximum 256 }))
   ```
3. Load the `.env` variables before running:
   ```bash
   # macOS / Linux (bash/zsh)
   export $(grep -v '^#' .env | xargs)

   # PowerShell
   Get-Content .env | ForEach-Object { if ($_ -match '^(.*?)=(.*)$') { [System.Environment]::SetEnvironmentVariable($matches[1], $matches[2]) } }
   ```

**Option B — In your IDE (IntelliJ IDEA / VS Code):**
- Open Run/Debug Configurations for `Lab2Application`
- Under **Environment variables**, paste:
  `JWT_SECRET=<your-secret>;ADMIN_PASSWORD=Admin1234!;DB_USER=postgres;DB_PASS=postgres`

### 2. Database Setup

Ensure PostgreSQL is running locally on port `5432`. Create the empty database named `esd_lab2`:

**Method 1 — Terminal (`psql` CLI):**
```bash
# Windows / macOS / Linux
psql -U postgres -c "CREATE DATABASE esd_lab2;"
```
*(Enter your PostgreSQL password when prompted)*

> **Note on Tables:** You do **not** need to write or run `CREATE TABLE` scripts manually. Spring Boot / Hibernate will automatically create and update the schema (`users`, `courses`, etc.) on first boot (`ddl-auto: update`).
>
> **Important:** Make sure the username and password in your `.env` file (`DB_USER` and `DB_PASS`) match your local PostgreSQL credentials!

### 3. Run the app

```bash
mvn spring-boot:run
```
*(Or run `Lab2Application.java` directly from your IDE)*

The app should start on `http://localhost:8080`.  
You should see `[AdminSeeder] Admin user created: admin@esd.local` in the console.

**Verify it works:**
```bash
curl http://localhost:8080/api/courses
# -> 200 []
```

---

## The Prompt Template

Every prompt to the AI **must** follow this template. Free-form prompts are not allowed.

```
CONTEXT
Spring Boot 3.x, Spring Security 6, Java 17+, jjwt 0.12.x. No other libraries.
Package layout: <paste the tree above>
Existing code you must use (do not rewrite it):
<paste the relevant existing classes>

TASK
Implement only: <one API or one component>
Contract: <paste the rows and DTOs from the contract>

ALLOWED TO CREATE OR CHANGE
<exact list of files/methods>

RULES
- Do not create or modify any file not listed above.
- Use the existing exceptions in com.esd.lab2.exception. Do not create new error classes.
- Use DTOs for request and response. Never return an entity from a controller.
- Use jakarta.validation annotations for input validation.
- Use only APIs that exist in Spring Security 6 and jjwt 0.12. No deprecated methods.
- If something is unclear, add a TODO comment instead of guessing.

OUTPUT
Each file in full, with its path. Then a list of anything you assumed.
```

**PROMPTS.md** has a pre-filled template for slice 1.

---

## The 5 Slices

Work through slices in order. **Do not start the next slice until the current one passes your review.**

| Slice | Delivers | APIs |
|---|---|---|
| **S1** Signup (Manual) | Registration with BCrypt | A1 |
| **S2** Login (AI) | Password check + JWT issuing | A2 |
| **S3** Protect (AI) | JWT filter, security rules, current user | P2 (P1 stays public) |
| **S4** Role (AI) | ADMIN-only course creation | P3  |
| **S5** Logout (AI) *(stretch)* | Token denylist | A3 |

---

### S1 - Signup (Manual - no AI)

**Skeleton files are already in the repo. You fill in the `// TODO` blocks:**
- [`dto/SignupRequest.java`](src/main/java/com/esd/lab2/dto/SignupRequest.java) - add validation annotations
- [`dto/UserResponse.java`](src/main/java/com/esd/lab2/dto/UserResponse.java) - fields already provided
- [`service/AuthService.java`](src/main/java/com/esd/lab2/service/AuthService.java) - implement the 5 steps in `signup()`
- [`controller/AuthController.java`](src/main/java/com/esd/lab2/controller/AuthController.java) - call the service, return 201

**Things to verify before moving to S2:**
- Password hashed via the **injected** `PasswordEncoder` bean - not `new BCryptPasswordEncoder()`.
- `UserResponse` does not contain `passwordHash`.
- The service always sets `Role.STUDENT` - it never reads role from the request.
- Controller returns **201**, not 200.
- You have **not** modified `SecurityConfig`.

**Fill in S1 section of `REVIEW.md` before moving on.**

**Test manually:**
```bash
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@test.com","password":"password1","fullName":"Alice"}'
# -> 201 { "id": ..., "email": "alice@test.com", "role": "STUDENT", ... }

# Duplicate -> 409
curl -X POST http://localhost:8080/api/auth/signup \
  -d '{"email":"alice@test.com","password":"password1","fullName":"Alice"}' \
  -H "Content-Type: application/json"
```

---

### S2 - Login & JWT (AI)

**What you ask AI to create:**
- `dto/LoginRequest.java`
- `dto/LoginResponse.java`
- `security/JwtService.java`  `generateToken` method only
- `service/AuthService.java`  add `login` method
- `controller/AuthController.java`  add `login` endpoint

**Key things to check in your review:**
- Same 401 message for unknown email AND wrong password: `"Invalid email or password"`.
- JWT secret comes from `@Value("${jwt.secret}")`  no string literal.
- Uses jjwt 0.12 API: `.subject()`, `.claim()`, `.id()`, `.issuedAt()`, `.expiration()`, `.signWith(key)`.
- **Reject** deprecated methods: `setSubject`, `setExpiration`, `signWith(SignatureAlgorithm.HS256, secret)`, `Jwts.parserBuilder()`.
- `jti` is a random UUID, not the user's database id.
- The token parse/validate method is **not** written yet (that's S3).

**Test manually:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@test.com","password":"password1"}'
#  200 { "accessToken": "eyJ...", "tokenType": "Bearer", "expiresIn": 3600 }
```

---

### S3 - JWT Filter & Protected API (AI)

> This is the hardest slice. Read the review checklist carefully before accepting any file.

**What you ask AI to create/modify:**
- `security/JwtService.java`  add parse/validate method
- `security/JwtAuthenticationFilter.java`  new class, `extends OncePerRequestFilter`
- `config/SecurityConfig.java`  replace the `filterChain` bean only (keep `PasswordEncoder`)
- `controller/UserController.java`  new, with `GET /api/users/me`

**Key things to check in your review:**
- **Spring Security 6 style only:** `SecurityFilterChain` bean, lambda DSL, `requestMatchers`, `authorizeHttpRequests`. Reject `WebSecurityConfigurerAdapter`, `antMatchers`, `authorizeRequests`, `.and()`.
- `permitAll` covers only: `POST /api/auth/signup`, `POST /api/auth/login`, `GET /api/courses`. Not `/api/**`.
- `GET /api/courses` is public but `POST /api/courses` is **NOT** (watch for matchers that open both).
- Filter uses `Jwts.parser().verifyWith(key).build().parseSignedClaims(token)`  not Base64-decode-and-trust.
- Filter does **not** write a response on error  it clears context and continues.
- Authorities use `ROLE_` prefix (e.g. `ROLE_ADMIN`)  required for `hasRole("ADMIN")` in S4.
- Entry point and access-denied handler wired via `exceptionHandling(...)`.
- `GET /api/users/me` returns `UserResponse`, not the `User` entity.

**Test manually:**
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@test.com","password":"password1"}' | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)

# Protected  200
curl http://localhost:8080/api/users/me -H "Authorization: Bearer $TOKEN"

# No token  401
curl http://localhost:8080/api/users/me

# Public still works
curl http://localhost:8080/api/courses
```

---

### S4 - Role Restriction (AI)

**What you ask AI to change:**

Choose **one** approach and state it in your review:
- **Option A:** Add a `requestMatchers` rule in `SecurityConfig` before `anyRequest().authenticated()`.
- **Option B:** Add `@PreAuthorize("hasRole('ADMIN')")` on `CourseController.create` and `@EnableMethodSecurity` on `SecurityConfig`.

**Key things to check in your review:**
- Role naming must be consistent: `hasRole("ADMIN")` looks for authority `ROLE_ADMIN`. If S3 set `ROLE_ADMIN`, this works. If there's a mismatch, every admin call fails with 403.
- If Option B: `@EnableMethodSecurity` is present on the config class.
- The diff should be **tiny**. If the AI rewrote the whole config, reject and re-prompt.

**Test manually:**
```bash
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@esd.local","password":"<your-admin-password>"}' \
  | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)

# ADMIN  201
curl -X POST http://localhost:8080/api/courses \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"code":"CS501","title":"Distributed Systems","description":"..."}'

# STUDENT  403
curl -X POST http://localhost:8080/api/courses \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"code":"CS502","title":"Test","description":""}'
```

---

### S5 - Logout *(stretch, AI)*

**What you ask AI to create/modify:**
- `security/TokenDenylist.java`  new `@Component` with `ConcurrentHashMap<String, Instant>`
- `security/JwtAuthenticationFilter.java`  add denylist check
- `controller/AuthController.java`  add `logout` endpoint (returns 204)

**Key things to check in your review:**
- Logout **changes server state**  rejects any code that only tells the client to discard the token.
- Denylist keyed by **jti**, not email.
- Map is `ConcurrentHashMap` (thread-safe).
- Logout endpoint is **not** under `permitAll`.
- Filter treats a denylisted jti the same as an expired token.

**Test manually:**
```bash
# Login  get token  access protected  logout  try protected again
curl -X POST http://localhost:8080/api/auth/logout -H "Authorization: Bearer $TOKEN"
#  204

curl http://localhost:8080/api/users/me -H "Authorization: Bearer $TOKEN"
#  401 (same token now rejected)
```

---

## No-AI Change (mandatory)

After finishing your slices  **Close AI tools.**

1. Change token expiry to 15 minutes and make `expiresIn` reflect it.
2. Add `GET /api/courses/{id}` as a public endpoint (404 via `ResourceNotFoundException`).
3. Add `PUT /api/users/me` that updates `fullName` only, for any logged-in user.
4. Make signup reject passwords without at least one digit, returning 400 with a field error.
5. Make `GET /api/courses` protected instead of public, and explain which tests would now fail.

Record your change in the **No-AI Change** section at the bottom of `REVIEW.md`.

---

## Common Problems

| Symptom | Likely cause |
|---|---|
| App won't start: `WeakKeyException` | `JWT_SECRET` not set, or decoded to fewer than 32 bytes |
| Every protected call gives **403** with empty body | Entry point not wired in `exceptionHandling` |
| Admin gets 403 on `POST /api/courses` | `ROLE_` prefix mismatch between filter and `hasRole` |
| `@PreAuthorize` has no effect | Missing `@EnableMethodSecurity` |
| Signup/login return 401 after S3 | Public paths missing or wrong HTTP method in matcher |
| SQL error on table `user` | `user` is reserved in Postgres  entity maps to `users`  |
| Filter runs twice | Filter is `@Component` and also added to the chain  remove one |
| `WebSecurityConfigurerAdapter` not found | AI generated Spring Boot 2 code  reject and re-prompt |
| Logout returns 204 but token still works | Denylist populated but filter never checks it |

---

## Submission

Submit your repository with:
- All slice code (`controller/`, `service/`, `security/`, `dto/` additions)
- `PROMPTS.md`  one prompt per slice, template used, follow-ups included
- `REVIEW.md`  all sections filled, understanding answers in your own words
