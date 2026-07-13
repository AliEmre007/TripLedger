# PoC Artefacts

- `results.json` contains the executed assertions and measured output.
- `allocation_concurrency.sqlite` contains the transactional allocation experiment.
- `tenant_constraints.sqlite` contains the organisation-scoped foreign-key experiment.
- `show_results.py` prints the machine-readable result summary.

The PoCs were executed by the Stage 3 package generator. The result artefacts are evidence of the run; the same behavioural tests must be reimplemented in the selected production stack.
