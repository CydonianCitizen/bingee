# Bingee 1.2.5 — cleanup, software optimization and documentation (in progress)

These notes describe implemented work on the development version `1.2.5` / versionCode 9.
The [canonical checklist](../BINGEE_DEVELOPMENT_ROADMAP_AND_CHECKLIST.md#v125-checklist) tracks
remaining performance work and verification; these notes do not announce a published release.

## Implemented cleanup

- C01–C03 remove the unused Library entry-count API/query, repository-only one-shot membership
  API and obsolete season-summary interface/binding. Observable membership, DAO checks used by
  TV Time and transactional season writes remain.
- C04–C05 place fake-only in-memory search and observation fallbacks in the debug source set.
  Library and calendar contracts require explicit implementations; production keeps its existing
  Room reads and writes. Release compilation excludes debug fixtures and helpers.
- C06–C07 remove an unused completion getter and import timestamp flag. Real movie/series
  timestamps, explicit watched dates, source precision and approximation warnings remain intact.

Current documentation distinguishes development status, historical decisions and the remaining
performance checklist. Room schema 9, Backup v3 export, v1/v2/v3 restore, user data, privacy,
navigation and visual behavior are unchanged by the cleanup.

## Implemented software optimizations

- Widget updater reads stop without installed widgets. Only changes to displayed content wake
  installed types; the small poster widget does not subscribe to the release calendar.
- Profile producers follow active UI consumers with a five-second grace period. Cached state and
  user writes survive a pause. Collection filtering and dashboard calculations leave Main; rapid
  Statistics selections and Room updates cannot publish an obsolete result.
- Continue Watching shares its daily Room projection, with observable read failures and recovery.
  Personal Viewing reuses one regular-episode aggregation for counts and runtime; unchanged DAO
  results avoid repeated domain mapping.
- Details episodes and Home releases are individual lazy items. Season surfaces, date grouping,
  qualified keys, progress actions, special seasons and accessibility targets remain intact.
- Episode reconciliation removes old map keys directly while retaining local IDs and watch history.
  Monthly statistics use one activity pass and cooperate with cancellation.
- Manual refresh uses at most three workers rather than one coroutine per title. It still processes
  the full manual selection and preserves the order used for the representative error.

- Backup import avoids a document-sized UTF-8 character buffer and decoded String. Export selects
  portable metadata in SQL before loading the snapshot, retaining user-owned history outside the
  library. Byte/record limits, v1/v2/v3 compatibility and transactional restore remain intact.

## Physical verification

A repeated release R8 comparison on HONOR CMA-LX1 / Android 13 confirms substantially cheaper
season expansion and lower expanded-season/Home PSS on the synthetic fixtures. Startup and already
smooth scrolling show no clear gain. Statistics PSS is higher in two after snapshots; retention,
allocations and GC during producer restart still need investigation. These are partial measurements
without images, not a blanket performance or memory improvement.

## Remaining work

The [performance checklist](checklist-1.2.5-performance.md) separates implementation and functional
verification from runtime measurements. Remaining physical scenarios, the full memory matrix,
the Statistics PSS investigation, the final milestone audit and the release gate remain open.
Measured gains apply to the tested synthetic scenarios; battery-life improvements have not been
measured.
