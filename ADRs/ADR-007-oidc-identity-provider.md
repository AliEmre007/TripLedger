# ADR-007: Use an OIDC Identity Provider

**Status:** Accepted  
**Date:** 13 July 2026

## Context

TripLedger requires authentication, MFA for Finance and Administrator financial functions, deactivation handling, and secure session policy. Implementing password and MFA storage directly would add unnecessary security risk.

## Decision

Use a mature OIDC identity provider. TripLedger stores local user mapping, organisation, role, and status, but not plaintext passwords or MFA secrets.

## Alternatives

- Build custom username/password/MFA.
- Use only framework-local session accounts.
- Defer MFA.

## Consequences

Benefits:

- reduces authentication implementation risk;
- supports MFA through provider controls;
- keeps application focused on authorisation and domain rules.

Costs:

- identity-provider configuration becomes operational dependency;
- tests must verify claim mapping and MFA policy;
- provider migration has medium cost.

## Controls

- validate issuer, audience, signature, expiry;
- check user active status;
- require MFA claim for protected financial functions;
- configure identity provider as code where practical.
