# bingee 1.2.5 — cleanup, measured optimizations and documentation

The agreed scope of `1.2.5` / versionCode 9 is complete as of 18 October 2026: final verification,
the full Android suite and documentation alignment. The [canonical checklist](../BINGEE_DEVELOPMENT_ROADMAP_AND_CHECKLIST.md#v125-checklist)
records closure and accepted limits. These notes do not announce a published release.

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

## Follow-up fixes and local storage

- Statistics no longer discards in-flight loading during brief navigation. UI rating shelves stop
  retaining duplicate episode histories; filter changes reuse the existing history input. Fixes
  reduce the observed loading/filter stalls while preserving current selection and animation.
- Widget next-episode text can wrap in Italian. Both widgets follow portable language changes,
  including accessible button descriptions, in the current process.
- Continue Watching excludes movies and series without a currently available next regular episode
  before materializing detailed rows. Collection state, incomplete coverage, history and qualified
  TMDB identity remain intact. On the 10,000-title fixture with 90% complete series, warm SQL median
  falls from 3,174.20 to 224.56 ms (92.9%); the other two large compositions cost about 3.4% more.
- Compact UTF-8 JSON preserves the v3 contract and v1/v2/v3 restore. The same synthetic export falls
  from 16,636,440 to 9,745,199 bytes (41.4% smaller); manually reading it is less convenient.
- Startup best-effort IO cleanup removes only abandoned app-private TV Time ZIP copies. Original
  archives and active imports remain protected; the restart check removes 8,390,045 abandoned bytes.
- User-visible branding uses lowercase `bingee` in EN/IT. Backup settings now describe export v3
  and restore v1/v2/v3 correctly in both languages.

## Physical verification

Measurements use isolated optimized R8 builds on HONOR CMA-LX1 / Android 13 and synthetic data.
Repeated season expansion comparisons show cheaper expansion and lower expanded-season/Home PSS.
The subsequent Statistics investigation and fix remove duplicate UI history retention; the full
calculation input remains available. Startup and already smooth scrolling show no clear gain.

Launcher checks cover both widgets, instance lifecycle, callbacks, visible changes, themes,
EN/IT, resize and synthetic poster replacement/fallback. Profile lifecycle checks preserve cached
state and new Room data after background/foreground cycles without duplicate producers.

Final release-runtime checks cover offline navigation in four configurations spanning EN/IT,
Light/Dark, 320–600dp and font scales 1.0–1.5. A 47,518,199-byte (45.32 MiB) backup restores 1,000
titles, 2,700 seasons and 27,000 episodes/progress rows in 10.225 s. Re-export in 14.858 s matches
the original JSON and bytes; preview leaves Room unchanged, and an injected restore failure rolls
back all tables in 17.294 s. No OOM, crash or ANR was observed in these scenarios. This is not a
continuous peak-memory measurement or a complete system file-picker UI test.

After the final EN/IT wording fix, the full unfiltered Android suite passes **276/276**, with zero
failures/errors/skips, plus one separate Italian backup-screen check. Spotless, lint, debug,
AndroidTest and optimized release builds pass; lint has zero errors and 38 warnings. Gradle reuses
the valid 496 JVM results as UP-TO-DATE because their inputs are unchanged. The corrected strings
are present in both normal debug and release APKs. Existing device apps, ten widget bindings and
display/font settings are preserved; diagnostic packages are removed.

## Accepted limits and next milestone

The current Film/Series animation is retained by owner choice. Residual warm-filter frames above
16.7 ms remain; rejected variants did not show a reliable gain. The wider image/heap/GC/device and
TalkBack matrices are not certified, and battery-life gains have not been measured. These limits
are recorded in the [performance closure record](checklist-1.2.5-performance.md#stato-di-chiusura--18-ottobre-2026),
without reopening completed work or presenting unmeasured improvements as facts.

The owner deferred the next global audit, potential justified dead-code/over-engineering fixes and
study of future optimization tradeoffs to **v1.2.6**. That audit was not run for this closure. New
optimization directions require a benefits/costs decision; a smaller poster-cache limit, schema
compaction and other disk proposals remain unapproved. Room 9 and Backup v3 remain unchanged.
