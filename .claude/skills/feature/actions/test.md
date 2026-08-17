# Test Action

1. Read current-feature.md to understand what was implemented
2. Identify Service methods and business logic added/modified for this feature (auth rules, XP/energy calculations, spaced repetition, etc.)
3. Check if tests already exist for these methods
4. For methods without tests that have testable logic, write tests:
   - Unit tests using JUnit 5 + Mockito for Service logic (mock Repositories)
   - Integration tests using `@SpringBootTest` + Testcontainers for Controllers/Repositories where the feature touches the database or HTTP layer
   - Test happy path and error cases (validation failures, not-found, unauthorized, etc.)
   - Do not write tests just to write them. Use your best judgement
5. Run `mvn test` (or `./mvnw test`) to verify all tests pass
6. Report test coverage for the new feature code