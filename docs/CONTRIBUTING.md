# Contributing Workflow

## Branches

- `main` is the stable branch.
- Work on short-lived branches named after the backlog item, for example `vr-006-error-model`.

## Pull Requests

Each pull request must include:

- linked `VALIDATION_RELEASE_BACKLOG.md` item;
- summary of behavior change;
- validation commands run;
- screenshots or response examples when API/UI behavior changes;
- documentation updates when contracts, schema, security, or operations change.

## Review Rules

Review must check:

- tenant isolation;
- role enforcement;
- exact money behavior;
- idempotency;
- immutable financial evidence;
- audit requirements;
- tests and failure cases;
- docs updated with behavior changes.

## Merge Gate

Do not merge unless:

```bash
make verify
```

passes or the failure is documented as environment-only and reproduced in CI.
