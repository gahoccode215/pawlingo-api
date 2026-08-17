# Coding Standards — pawlingo-api

Applies to all code in this Spring Boot repo (Java 21 / Spring Boot 4.1.0).

---

## 1. Package structure

Organize by **feature (package-by-feature)**, not by layer, so it scales cleanly as modules (auth, pet, vocab, progress...) are added:

```
com.pawlingo.api
├── auth
│   ├── AuthController.java
│   ├── AuthService.java
│   ├── dto/            (RegisterRequest, LoginRequest, AuthResponse...)
│   └── entity/          (if the entity belongs only to this module)
├── user
├── pet
├── vocab
├── progress
├── common
│   ├── response/        (ApiResponse<T>, ErrorResponse)
│   ├── exception/        (GlobalExceptionHandler, custom exceptions)
│   ├── config/            (SecurityConfig, OpenApiConfig...)
│   └── util/
└── PawlingoApiApplication.java
```

Each feature package typically has: `*Controller`, `*Service` (+ `*ServiceImpl` if an interface is needed), `*Repository`, `entity/`, `dto/`.

---

## 2. Naming conventions

- Classes: `PascalCase`. Entities are singular (`User`, `Pet`, not `Users`).
- Methods/fields/variables: `camelCase`.
- Constants: `UPPER_SNAKE_CASE`.
- Request DTOs: `XxxRequest` (e.g. `RegisterRequest`). Response DTOs: `XxxResponse` (e.g. `PetResponse`).
- REST endpoints: plural nouns, lowercase/kebab-case, versioned: `/api/v1/vocab-topics`.
- DB tables: `snake_case`, plural (`users`, `vocab_words`).

---

## 3. Layered architecture — dependency rules

`Controller → Service → Repository`

- **Controller**: only accepts requests, validates input (`@Valid`), calls the service, returns `ApiResponse<T>`. No business logic here.
- **Service**: holds all business logic. Accepts/returns DTOs — never leaks Entities to the Controller.
- **Repository**: interface extending `JpaRepository`, no business logic.
- Never call back from Repository → Service, and Entities never call Services.

---

## 4. DTO vs Entity — strict separation

- **Never** return an Entity (`@Entity`) directly from a Controller. Always map to a response DTO.
- Mapping: use MapStruct (recommended as the project grows) or manual constructors/static factory methods early on. Avoid ModelMapper (reflection-based, hard to debug).
- Entities have no Jackson annotations (`@JsonProperty`...) — an Entity is a pure domain model; JSON serialization is the DTO's job.

---

## 5. Response envelope (matches FE `src/lib/api.ts`)

All API responses go through one unified envelope:

```java
public record ApiResponse<T>(boolean success, T data, ErrorDetail error) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }
    public static <T> ApiResponse<T> fail(String code, String message) {
        return new ApiResponse<>(false, null, new ErrorDetail(code, message));
    }
}

public record ErrorDetail(String code, String message) {}
```

- Every Controller returns `ResponseEntity<ApiResponse<T>>`.
- Errors are handled centrally via `@RestControllerAdvice` (`GlobalExceptionHandler`), not scattered try/catch blocks in Controllers.

---

## 6. Exception handling

- Define domain-specific exceptions extending `RuntimeException`, e.g. `ResourceNotFoundException`, `InvalidCredentialsException`, `DuplicateEmailException`.
- `GlobalExceptionHandler` maps each exception type to the appropriate HTTP status + `ApiResponse.fail(...)`.
- Validation errors (`MethodArgumentNotValidException`) → HTTP 400 with a list of field errors in `error.message` or a dedicated field for more detail.
- Never leak stack traces or internal messages (raw SQL, root exception) through the API.

---

## 7. Validation

- Use Jakarta Bean Validation on request DTOs (`@NotBlank`, `@Email`, `@Size`...); avoid manual validation in the Service except for complex business rules (e.g. checking whether an email already exists).
- Controller methods use `@Valid @RequestBody`.

---

## 8. Security / Auth

- Use Spring Security. JWT for stateless session-based API — no `HttpSession`.
- Google OAuth2 via `spring-boot-starter-oauth2-client`.
- Hash passwords with `BCryptPasswordEncoder`; never store plaintext.
- Never hardcode secrets (JWT secret, OAuth client secret, DB credentials) in code — use environment variables / `application.yml` + `.env` (never commit `.env`).
- Public endpoints (register/login/health) are explicitly declared in `SecurityConfig`; everything else requires auth by default.

---

## 9. Database & migrations

- Use Flyway; migration scripts live in `src/main/resources/db/migration`, named `V{n}__description.sql`.
- Never use `spring.jpa.hibernate.ddl-auto=update` in real environments — use `validate` or `none`; schema changes go through Flyway.
- Entities use `@Column(nullable = ...)` explicitly; avoid ambiguous optional fields with no constraints.

---

## 10. Lombok

- Use `@Getter`/`@Setter`, `@Builder`, `@NoArgsConstructor`/`@AllArgsConstructor` to cut boilerplate on Entities/DTOs.
- Avoid `@Data` on Entities (generates `equals/hashCode` from all fields, which is error-prone with JPA lazy loading and bidirectional relationships).

---

## 11. Testing

- Unit tests for Services (mock the Repository with Mockito).
- Integration tests for Controllers/Repositories using `@SpringBootTest` + Testcontainers (real Postgres, not H2, to stay close to production behavior).
- Test naming: `should_xxx_when_yyy()` or `methodName_condition_expectedResult()`.
- Coverage is not a goal in itself, but all important business logic (auth, XP calculation, spaced repetition) must be tested.

---

## 12. Logging

- Use SLF4J (`private static final Logger log = LoggerFactory.getLogger(Xxx.class)`), never `System.out.println`.
- Never log sensitive data (passwords, tokens, or child-related PII if parent accounts are added later).

---

## 13. Format & style

- 4-space indentation, no tabs.
- Apply Google Java Format or Spotless (recommended as a Maven plugin to enforce automatically via CI).

---

## 14. Git commit convention

Follow Conventional Commits:

```
feat(auth): add google oauth login
fix(pet): correct energy decay calculation
chore(deps): add flyway-core
```