## Summary

Describe the change and its user or operational impact.

## Validation

- [ ] `make test-unit` or the equivalent Maven command was run.
- [ ] Integration tests were run when persistence, Redis, messaging, or external adapters changed.
- [ ] Relevant API/manual checks were run.
- [ ] Documentation and OpenAPI were updated when public behavior or configuration changed.

## Compatibility and data

- [ ] No API, configuration, or database compatibility impact.
- [ ] A backward-compatible Flyway migration is included; no applied migration was edited.
- [ ] A release/version update is required and is described below.

<!-- Describe any compatibility, migration, rollback, or release consideration. -->

## Security review

- [ ] No public endpoint, authorization, secret, payment, or personal-data behavior changed.
- [ ] Public endpoint/authorization changes were reviewed and tested.
- [ ] No credentials, tokens, or personal data were added to code, logs, fixtures, or documentation.
- [ ] Payment/webhook signature and idempotency behavior was reviewed where applicable.

## Checklist

- [ ] The branch name and PR title follow the project convention.
- [ ] The change is scoped and has appropriate tests.
- [ ] New follow-up work is recorded in `docs/markdowns/IMPROVEMENT_CHECKLIST.md` when needed.
