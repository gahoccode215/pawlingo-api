# Coding Standards — pawlingo-api

Applies to all code in this Spring Boot repo (Java 21 / Spring Boot 4.1.0).

---

## 1. Package structure

Top level is organized by **feature (package-by-feature)** — auth, user, vocab, progress... — so it scales cleanly as modules are added. **Within** each feature package, organize by layer:

```
com.pawlingo.api
├── auth
│   ├── controller/
│   │   └── AuthController.java
│   ├── service/
│   │   ├── AuthService.java        (interface)
│   │   ├── JwtService.java          (no interface — single implementation, not swapped/mocked at the interface level)
│   │   └── impl/
│   │       └── AuthServiceImpl.java
│   └── dto/
│       ├── request/     (RegisterRequest, LoginRequest...)
│       └── response/    (RegisterResponse, LoginResponse, MeResponse...)
├── user
│   ├── entity/
│   │   └── User.java
│   ├── enums/
│   │   ├── Goal.java          (also referenced directly by auth DTOs, not user-only)
│   │   └── AuthProvider.java
│   └── repository/
│       └── UserRepository.java
├── vocab
├── progress
├── common
│   ├── response/        (ApiResponseDTO<T>, ErrorDetail)
│   ├── exception/        (GlobalExceptionHandler, BusinessException, ErrorCode)
│   ├── security/          (JwtAuthenticationFilter, JwtAuthenticationEntryPoint)
│   ├── config/            (SecurityConfig, OpenApiConfig...)
│   └── util/
└── PawlingoApiApplication.java
```

- `service/` holds the interface directly; the implementation lives in `service/impl/` as `*ServiceImpl` (e.g. `AuthService` + `AuthServiceImpl`). Controllers and other services depend on the interface type, never the impl, so Spring injects by interface and the impl stays swappable/mockable.
- A service only needs this interface+impl split when it's a business service consumed elsewhere via its interface (e.g. `AuthService`). A narrow technical utility used by exactly one caller (e.g. `JwtService`, only used inside `auth`) can stay a concrete `@Service` class in `service/` without an interface — don't split it just for consistency.
- `repository/` holds `*Repository` interfaces (`extends JpaRepository`).
- `entity/` holds only `@Entity`-annotated classes. Enums go in a sibling `enums/` folder, not inside `entity/` — an enum is a shared domain value type (often referenced directly by DTOs across feature packages, e.g. `Goal` in `auth`'s request/response DTOs), not something owned exclusively by the entity.
- Each feature package typically has: `controller/`, `service/` (+`service/impl/`), `repository/`, `entity/`, `enums/`, `dto/request/`, `dto/response/` — add only the ones a feature actually needs.

---

## 2. Naming conventions

- Classes: `PascalCase`. Entities are singular (`User`, not `Users`).
- Methods/fields/variables: `camelCase`.
- Constants: `UPPER_SNAKE_CASE`.
- Request DTOs: `XxxRequest` (e.g. `RegisterRequest`). Response DTOs: `XxxResponse` (e.g. `LoginResponse`).
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
public record ApiResponseDTO<T>(boolean success, T data, ErrorDetail error) {
    public static <T> ApiResponseDTO<T> ok(T data) {
        return new ApiResponseDTO<>(true, data, null);
    }
    public static <T> ApiResponseDTO<T> fail(String code, String message) {
        return new ApiResponseDTO<>(false, null, new ErrorDetail(code, message));
    }
}

public record ErrorDetail(String code, String message) {}
```

> Named `ApiResponseDTO` (not `ApiResponse`) to avoid a class-name clash with springdoc-openapi's own response types once Swagger is added.

- Every Controller returns `ResponseEntity<ApiResponse<T>>`.
- Errors are handled centrally via `@RestControllerAdvice` (`GlobalExceptionHandler`), not scattered try/catch blocks in Controllers.

---

## 6. Exception handling

- Domain/business errors go through a single `BusinessException(ErrorCode)` (`common/exception`) instead of one class per error — `ErrorCode` is an enum carrying `HttpStatus` + default message per case (`DUPLICATE_EMAIL`, `INVALID_CREDENTIALS`, ...). Adding a new business error is a new enum constant, not a new `.java` file.
- `GlobalExceptionHandler` has one `@ExceptionHandler(BusinessException.class)` that reads `ex.getErrorCode()` to build the `ApiResponseDTO.fail(...)`.
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