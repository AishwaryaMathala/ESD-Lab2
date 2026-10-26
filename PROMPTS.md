# PROMPTS.md — AI Prompt Log

Record every prompt you send to the AI **starting from Slice 2**.  
Slice 1 is done manually — no AI. See `README.md` for the prompt template structure.

> **Rules:**
> - One prompt (+ follow-ups) per slice
> - Use the structured template from the README — free-form prompts are not allowed
> - Do not back-fill this file after the lab

---

## Example — How a good S1 prompt looks (for reference only, S1 is done manually)

```
CONTEXT
Spring Boot 3.x, Spring Security 6, Java 17+, jjwt 0.12.x. No other libraries.
Package layout:
  com.esd.lab2
  ├── config/       SecurityConfig.java, AdminSeeder.java
  ├── controller/   CourseController.java
  ├── service/      CourseService.java
  ├── dto/          CourseRequest.java, CourseResponse.java
  ├── entity/       User.java, Role.java (enum), Course.java
  ├── repository/   UserRepository.java, CourseRepository.java
  ├── exception/    ApiError.java, GlobalExceptionHandler.java,
  │                 DuplicateResourceException.java, InvalidCredentialsException.java,
  │                 ResourceNotFoundException.java
  └── security/     JsonAuthenticationEntryPoint.java, JsonAccessDeniedHandler.java


TASK
Implement only POST /api/auth/signup.
Request: SignupRequest { email (valid email, required), password (required, 8–64 chars),
         fullName (required, not blank, max 100) }.
Response: 201 with UserResponse { id, email, fullName, role, createdAt }.
The new user's role is always STUDENT, even if the request contains a role field.
Store email trimmed and lowercased. Hash the password with the existing PasswordEncoder bean.
If the email already exists, throw DuplicateResourceException.

ALLOWED TO CREATE OR CHANGE
dto/SignupRequest.java, dto/UserResponse.java,
service/AuthService.java (signup method only),
controller/AuthController.java (signup endpoint only)

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

---

## Slice 2 — Login & JWT (S2)

**Prompt 1:**

```
CONTEXT

Spring Boot 3.x, Spring Security 6, Java 17+, jjwt 0.12.x. Use only dependencies already present in the project.

Package layout:
- `com.esd.lab2.controller`: `AuthController.java`, `CourseController.java`
- `com.esd.lab2.service`: `AuthService.java`, `CourseService.java`
- `com.esd.lab2.dto`: `SignupRequest.java`, `SignupResponse.java` if present, `UserResponse.java`, and other existing DTOs
- `com.esd.lab2.entity`: `User.java`, `Role.java`, `Course.java`
- `com.esd.lab2.repository`: `UserRepository.java`, `CourseRepository.java`
- `com.esd.lab2.exception`: `ApiError.java`, `GlobalExceptionHandler.java`, `DuplicateResourceException.java`, `InvalidCredentialsException.java`, `ResourceNotFoundException.java`
- `com.esd.lab2.security`: existing security handlers and any existing security classes

TASK

Implement only Slice 2: user login and JWT issuing.

1. Create `dto/LoginRequest.java` as a record with:
   - `email`: `@NotBlank` and `@Email`
   - `password`: `@NotBlank`

2. Create `dto/LoginResponse.java` as a record containing:
   - `accessToken` (`String`)
   - `tokenType` (`String`), always `"Bearer"`
   - `expiresIn` (`long`, seconds)

3. Implement `security/JwtService.java` with `generateToken(User user)` using the JJWT 0.12 API.
   - Inject `${jwt.secret}` using `@Value`; do not hardcode the secret.
   - Build a signing key suitable for HMAC-SHA256 from the Base64-encoded secret.
   - Include `sub` = user's email, `role` = user's role, `jti` = a random UUID, `iat` = issue time, and `exp` = expiration time.
   - Use the JJWT 0.12 builder methods: `.subject()`, `.claim()`, `.id()`, `.issuedAt()`, `.expiration()`, and `.signWith(key)`.
   - Use the existing token lifetime configuration if one is defined; otherwise use the project's documented default of 3600 seconds.
   - Return the token and make the token lifetime available to `AuthService`.

4. Implement `AuthService.login(LoginRequest request)`.
   - Trim and lowercase the email before looking up the user.
   - Verify the password with the injected `PasswordEncoder.matches()`.
   - For both an unknown email and a wrong password, throw the existing `InvalidCredentialsException` with exactly `"Invalid email or password"`.
   - Generate the JWT and return a `LoginResponse` with `tokenType` `"Bearer"` and `expiresIn` in seconds.

5. Add `POST /api/auth/login` to `AuthController.java`.
   - Accept `@Valid @RequestBody LoginRequest`.
   - Call `authService.login()`.
   - Return HTTP 200 OK with `LoginResponse`.

ALLOWED TO CREATE OR CHANGE

- `dto/LoginRequest.java`
- `dto/LoginResponse.java`
- `security/JwtService.java`
- `service/AuthService.java` — login method and required JWT service injection only
- `controller/AuthController.java` — login endpoint only

RULES

- Do not create, modify, or reformat files outside the allowed list.
- Preserve the existing Slice 1 signup implementation.
- Do not modify `SecurityConfig`, `UserRepository`, `User`, `Role`, exception classes, `application.properties`, or `pom.xml`.
- Reuse the existing `InvalidCredentialsException`; do not invent new exception or response wrapper classes.
- Use DTOs; never return the `User` entity from a controller.
- Use only JJWT 0.12 APIs. Do not use deprecated methods such as `setSubject`, `setExpiration`, `Jwts.parserBuilder()`, or `signWith(SignatureAlgorithm.HS256, secret)`.
- The JWT `jti` must be a random UUID, not the user's database ID.
- Do not implement token parsing, validation filters, logout, or protected endpoints; those belong to later slices.
- If a requirement conflicts with the existing project, explain the conflict and add a TODO rather than modifying an out-of-scope file.
- If a required existing method or field is unclear, inspect the project and state the assumption explicitly.

OUTPUT

1. List any assumptions or existing project details that need confirmation.
2. Provide each allowed file in full, with its path.
3. Briefly explain the login flow and how the token expiry is calculated.
4. Do not include changes to files outside the allowed list.
```

**Follow-up (if any):**

```
(paste here)
```

---

## Slice 3 — JWT Filter & Protected API (S3)

**Prompt 1:**

```
CONTEXT

Spring Boot 3.x, Spring Security 6, Java 17+, jjwt 0.12.x.
Use only dependencies already present in the project.

Package layout:
- com.esd.lab2.controller: AuthController.java, CourseController.java
- com.esd.lab2.service: AuthService.java, CourseService.java
- com.esd.lab2.dto: LoginRequest.java, LoginResponse.java, UserResponse.java
- com.esd.lab2.entity: User.java, Role.java, Course.java
- com.esd.lab2.repository: UserRepository.java, CourseRepository.java
- com.esd.lab2.exception: ApiError.java, GlobalExceptionHandler.java,
  DuplicateResourceException.java, InvalidCredentialsException.java,
  ResourceNotFoundException.java
- com.esd.lab2.security: JwtService.java, existing security handlers
- com.esd.lab2.config: SecurityConfig.java, AdminSeeder.java

Slice 1 signup and Slice 2 login/JWT generation are implemented.
Preserve their existing behavior.

TASK

Implement only Slice 3: JWT authentication and the protected current-user API.

1. Update security/JwtService.java.
   - Add parseToken(String token) returning io.jsonwebtoken.Claims.
   - Verify tokens using:
     Jwts.parser().verifyWith(key).build()
         .parseSignedClaims(token).getPayload()
   - Reuse the existing signing key created from the configured
     Base64-encoded JWT secret.
   - Allow JJWT to reject invalid signatures and expired tokens.
   - Do not change the existing generateToken() behavior.

2. Create security/JwtAuthenticationFilter.java.
   - Extend OncePerRequestFilter.
   - Read the Authorization header and extract a Bearer token.
   - Validate the token using JwtService.parseToken().
   - Extract the subject (email) and role from verified claims.
   - Create UsernamePasswordAuthenticationToken with the email
     as principal and authority "ROLE_" + role.
   - Store the authentication in SecurityContextHolder.
   - On token-processing errors, clear the SecurityContext and
     continue the filter chain without writing an HTTP response.
   - Do not trust unverified JWT claims.

3. Update config/SecurityConfig.java.
   - Use Spring Security 6 SecurityFilterChain, lambda DSL,
     authorizeHttpRequests(), and requestMatchers().
   - Disable CSRF.
   - Set session creation policy to STATELESS.
   - Permit only these public endpoints:
       POST /api/auth/signup
       POST /api/auth/login
       GET  /api/courses
   - Require authentication for any other request.
   - Ensure POST /api/courses is not public.
   - Wire the existing JsonAuthenticationEntryPoint and
     JsonAccessDeniedHandler using exceptionHandling().
   - Register JwtAuthenticationFilter before
     UsernamePasswordAuthenticationFilter.
   - Preserve existing required beans and constructor injection.

4. Create controller/UserController.java.
   - Implement GET /api/users/me.
   - Require authentication.
   - Obtain the authenticated email from Authentication.getName().
   - Retrieve the user from UserRepository by email.
   - Return the existing UserResponse DTO with HTTP 200 OK.
   - Never return the User entity directly.

ALLOWED TO CREATE OR CHANGE

- security/JwtService.java — add token parsing only
- security/JwtAuthenticationFilter.java — create
- config/SecurityConfig.java — configure the filter chain
- controller/UserController.java — create current-user endpoint

RULES

- Do not modify files outside the allowed list.
- Preserve existing signup, login, and JWT generation behavior.
- Do not modify pom.xml, application.yml, entities, repositories,
  DTOs, exception classes, or existing security handlers.
- Reuse existing classes and beans wherever possible.
- Do not implement role restrictions from S4 or logout from S5.
- Do not use deprecated APIs such as WebSecurityConfigurerAdapter,
  antMatchers(), authorizeRequests(), or .and().
- Do not make /api/** or both GET and POST /api/courses public.
- The JWT filter must not write an error response when token
  parsing fails; clear the context and continue the chain.
- If an existing class, bean, constructor, or method differs from
  these assumptions, inspect the project and explain the conflict.
  Do not modify out-of-scope files to work around it.

OUTPUT

1. List assumptions and existing project details requiring confirmation.
2. Provide each changed or created allowed file in full, with its path.
3. Explain the JWT verification and authentication flow briefly.
4. Provide PowerShell commands to test:
   - Valid token: GET /api/users/me returns 200.
   - No token: GET /api/users/me returns 401.
   - Public GET /api/courses still works.
   - POST /api/courses requires authentication.
   - Invalid or expired token cannot authenticate.
5. List any files changed outside the allowed list (there should be none).
```

**Follow-up (if any):**

```
(paste here)
```

---

## Slice 4 — Role Restriction (S4)

**Prompt 1:**

```
Slice 4 — Role-Based Authorization (ADMIN-only Course Creation)

Initial Prompt

CONTEXT

Spring Boot 3.x, Spring Security 6, Java 17+, and JJWT 0.12.x. Use only dependencies already present in the project.

Package layout:

com.esd.lab2.config: SecurityConfig.java

com.esd.lab2.controller: AuthController.java, CourseController.java

com.esd.lab2.service: AuthService.java, CourseService.java

com.esd.lab2.security: JwtAuthenticationFilter.java, JsonAccessDeniedHandler.java

com.esd.lab2.dto: existing request and response DTOs

com.esd.lab2.exception: existing exception handlers and exceptions

Use the existing code and preserve all working Slice 1–3 functionality.

TASK

Implement only Slice 4: restrict course creation to ADMIN users.

Contract

POST /api/courses requires the ADMIN role.

An ADMIN request with a valid course body returns HTTP 201 Created.

A STUDENT request returns HTTP 403 Forbidden using the standard JSON error response.

A request without a valid token returns HTTP 401 Unauthorized.

GET /api/courses remains public and returns HTTP 200 OK.

ALLOWED TO CREATE OR CHANGE

src/main/java/com/esd/lab2/config/SecurityConfig.java

Change only the authorization rules necessary to restrict POST /api/courses.

RULES

Use Spring Security 6 APIs.

Use hasRole("ADMIN"), consistent with the JWT filter's ROLE_ADMIN authority.

Place the POST matcher before .anyRequest().authenticated().

Preserve the existing public signup, login, and course-list endpoints.

Do not modify controllers, services, DTOs, exception handlers, or other files.

Do not add dependencies.

OUTPUT

Provide the complete updated SecurityConfig.java, followed by assumptions and a short explanation of the authorization rule.

Follow-up Prompt

Review the change against the Slice 4 contract. Confirm that only SecurityConfig.java needs to change, that POST /api/courses uses hasRole("ADMIN"), and that public signup, login, and GET /api/courses remain accessible. Identify any mismatch without rewriting unrelated code.

Manual Test Results

ADMIN creates a course: successful; returned course ID 2, code ZZ999, title Distributed Systems Test.

STUDENT attempts course creation: HTTP 403 Forbidden, with the expected JSON permission error.

No-token POST test: returned HTTP 401 Unauthorized.

Public GET /api/courses test: returned HTTP 200 OK.

```

**Follow-up (if any):**

```
(paste here)
```

---

## Slice 5 — Logout *(stretch, S5)*

**Prompt 1:**

```
Slice 5 — Logout (stretch, S5)

Prompt 1:

CONTEXT

Spring Boot 3.x, Spring Security 6, Java 17+, jjwt 0.12.x.
Use only dependencies already present in the project.
Preserve the working S1–S4 implementation.

TASK

Implement Slice 5 — Server-Side Token Invalidation (Logout).

1. Create security/TokenDenylist.java.
   - Use @Component and ConcurrentHashMap<String, Instant>.
   - Implement revoke(jti, expiry), isRevoked(jti), and cleanExpired().

2. Update controller/AuthController.java.
   - Add POST /api/auth/logout.
   - Extract the Bearer token from the Authorization header.
   - Parse verified JWT claims to retrieve jti and expiration.
   - Add the token ID and expiry to the denylist.
   - Return HTTP 204 No Content.

3. Update security/JwtAuthenticationFilter.java.
   - Inject TokenDenylist.
   - Check the parsed token's jti against the denylist.
   - Reject revoked tokens with HTTP 401 and stop the filter chain.

4. Preserve existing signup, login, JWT generation, role restrictions,
   and protected endpoints.

RULES

- Use existing project dependencies and JJWT 0.12 APIs.
- Use jti, not email, as the denylist key.
- Do not add /api/auth/logout to permitAll().
- Do not rewrite unrelated files.
- Ensure a revoked token cannot access protected endpoints.

OUTPUT

Provide the required file changes and PowerShell commands to test:
- A valid token accesses /api/users/me.
- Logout returns 204.
- The same token returns 401 after logout.
- A fresh token obtained by logging in again works.
```

**Follow-up (if any):**

```
Review the S5 implementation against the contract. Preserve existing
S1–S4 behavior and identify any issues without rewriting unrelated code.
The first logout attempt returned HTTP 500; after restarting the
application, POST /api/auth/logout returned HTTP 204. The same token
then returned HTTP 401 from GET /api/users/me, and a fresh login
successfully accessed the endpoint with HTTP 200.
```
