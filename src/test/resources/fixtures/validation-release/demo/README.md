# End-to-End Validation Demo Dataset

This synthetic dataset supports `VR-026`.

It packages the final validation-release story as source files plus expected evidence:

- exact booking reconciliation;
- OTA settlement with explicit deduction evidence;
- cancellation and refund;
- ambiguous payment;
- duplicate import;
- FX payment with exchange-rate evidence;
- short settlement with seeded partial-match evidence;
- forbidden financial action;
- restore rehearsal evidence.

The contract is declared in `demo-manifest.json` and verified by `EndToEndDemoDatasetTest`.

The files are intentionally synthetic and contain no real customer, card, passport, medical, or bank data.
