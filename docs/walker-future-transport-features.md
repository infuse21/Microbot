# Walker future transport features

## Scope decision — 2026-09-15

The user explicitly moved Boat/Last Boat and remaining dynamic Respawn support into a
separate feature backlog. They were not supported by the legacy walker and are not
Phase 6 migration requirements. This supersedes earlier implementation-blocker labels
in the [migration plan](walker-unification-plan.md) and
[live-testing ledger](walker-live-testing-backlog.md). It does not mark them implemented
or live-tested, and does not waive the migration's ownership/deletion gates.

## Boat / Last Boat

Pending: a directed destination contract using verified player landing coordinates,
owned-boat availability, focus eligibility, selector dispatch and landing confirmation.
Do not use boat navigation coordinates or dock map markers as player landing tiles.
Unknown dock IDs must fail closed, not fall back to Port Sarim.

Research checkpoint: RuneLite cs2-scripts revision
`c5de38b0013a8521c4dce343ee6538fcccb23a60`, procedures 8997/9081/9013/9016/9094,
establishes owned == 1, greater focus >= 2, and invalid port sentinels 253/254/255.
The boat selector uses a boat-slot callback and `cc_resume_pausebutton`, not ordinary
chat dialogue. Exact player landings remain unverified. See the live-testing ledger
for the detailed source audit; no Boat teleport acceptance is claimed.

## Remaining dynamic Respawn destinations

Six explicit destination flags currently resolve directed destinations. The all-zero
case does not distinguish Lumbridge from Prifddinas reliably and remains excluded.
Pending: authoritative selection evidence for those remaining cases, refresh and
dispatch validation, and regression tests. Do not interpret unknown as Lumbridge.
Preserve the already implemented fail-closed handling; this deferral does not remove it.
