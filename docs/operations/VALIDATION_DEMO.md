# End-to-End Validation Demo

## Purpose

`VR-026` packages the validation release into a synthetic end-to-end dataset.

The dataset lives in:

```text
src/test/resources/fixtures/validation-release/demo
```

It demonstrates:

- exact booking reconciliation;
- OTA settlement with commission and fee evidence;
- cancellation and refund;
- ambiguous payment review;
- duplicate import with no duplicate effect;
- FX payment with exchange-rate evidence;
- short settlement discrepancy evidence;
- forbidden financial action;
- backup restore evidence.

## Validate the Dataset

Run:

```bash
make demo-validate
```

This runs `EndToEndDemoDatasetTest`, which verifies:

- all required scenarios are present;
- every manifest source file exists and has content;
- CSV and JSON row counts match the manifest;
- scenario ids are represented in source data;
- expected economics totals reconcile to the fixture amounts;
- FX evidence converts to the expected target amount;
- forbidden-action and restore evidence expectations are declared;
- fixtures do not contain prohibited real-data markers.

## Demo Order

1. Import `bookings.csv`.
2. Import `supplier_obligations.csv`.
3. Import `financial_events.csv`.
4. Add exchange-rate evidence from `exchange_rate_evidence.json`.
5. Run economics, matching, reconciliation, discrepancy, and timeline reads for the scenario bookings.
6. Re-import `bookings_duplicate.csv` and confirm duplicate row handling.
7. Attempt the forbidden financial import as an Operations actor and confirm the stable error.
8. Run the backup/restore rehearsal from `BACKUP_RESTORE_REHEARSAL.md`.

The dataset is synthetic and must remain free of real customer, payment, passport, medical, or bank data.
