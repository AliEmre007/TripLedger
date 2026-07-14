# Validation Release Fixtures

`VR-001` is represented by executable fixtures under:

```text
src/test/resources/fixtures/validation-release
```

The fixture manifest maps each file to the acceptance criteria it covers and, for CSV imports, the expected accepted, duplicate, rejected, warning, and batch-status outcomes.

Covered criteria:

- import behavior: `AC-011` through `AC-021`;
- economics and explanation: `AC-033` through `AC-036`;
- matching and cross-currency behavior: `AC-040` through `AC-042`.

These fixtures are not production sample data. They are synthetic rule examples for automated tests and later implementation slices.
