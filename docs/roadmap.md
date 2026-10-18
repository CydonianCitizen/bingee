# bingee Roadmap

This document retains earlier delivery summaries. The [canonical development roadmap and checklist](../BINGEE_DEVELOPMENT_ROADMAP_AND_CHECKLIST.md) defines the active implementation order and release gates.

## Completed agreed scope in `v1.2.5` — 2026-10-18

Version 1.2.5/code 9 is closed within the owner's agreed scope: final checks, full Android suite
and documentation alignment. Publication remains a separate explicit action. Room 9 and Backup v3
with v1/v2/v3 restore are unchanged.

- C01–C07 remove proven unused APIs/aliases and move fake-only helpers into debug, preserving
  production Room behavior, progress, timestamp precision and import warnings.
- Widget instance gating, Profile consumer ownership and off-Main work, shared Room projections,
  lazy episodes/releases, direct episode-map reconciliation, single-pass monthly statistics and
  bounded manual refresh are implemented. Later fixes address Statistics loading/navigation and
  filter stalls, widget EN/IT text/accessibility and unnecessary complete-series materialization.
- Compact JSON and startup cleanup of abandoned private import ZIPs reduce measured disk use,
  preserving original archives, active imports and backup compatibility.
- Final isolated R8 checks on HONOR / Android 13 cover offline navigation, EN/IT, Light/Dark,
  narrow/large layouts and a 45.32 MiB restore/re-export/rollback scenario. The full Android suite
  passes 276/276; a separate Italian backup-screen check passes. Build/lint are green, with 38
  lint warnings and the valid 496 JVM results reused by Gradle as UP-TO-DATE.

The [implementation notes](release-notes-1.2.5.md) and [performance closure record](checklist-1.2.5-performance.md#stato-di-chiusura--18-ottobre-2026)
preserve measured benefits, costs and coverage limits. Residual warm Film/Series frames above
16.7 ms remain accepted with the current animation. Unexecuted image/heap/device matrices and
additional optimization proposals are not new closure requirements. Raw evidence remains ignored
under `.audit/`; no global performance or battery-life guarantee is claimed.

## In progress: `v1.2.6` — investigation and planning

The owner started the v1.2.6 study on 2026-10-18, following the read-only audit, and subsequently
accepted its recommendation to retain the current Coil image-cache configuration. The image-cache
investigation is complete: the baseline measurements did not justify reducing the limit on the
tested Android 17/API 37 emulator with its fixed dataset of 16 titles. This conclusion does not generalize
to large libraries, other devices or prolonged use. Further image-cache measurements are deferred
until a concrete problem emerges; they are not an open milestone gate. The numerical thresholds
proposed in the temporary report remain hypotheses to reassess, not approved requirements.
Reports, evidence, tools and the repeatable protocol remain under ignored `.audit/`.

Review the two season/episode prefix indices and the genre index with the next Room migration
required for other reasons. Removal requires Android query-plan confirmation, non-destructive
migration tests, data-integrity checks and paired latency measurements. No Room bump, index removal
or `VACUUM` is planned now; host synthetic savings are neither Android results nor immediate file
shrinkage. Backup-share retention, URL compaction, `temp_ui.xml` removal and general refactors remain
outside this study. The app remains 1.2.5/code 9; completed v1.2.5 optimizations stay closed. This
maintenance step precedes the v1.3 UI cycle; see the
[canonical plan](../BINGEE_DEVELOPMENT_ROADMAP_AND_CHECKLIST.md#v126--global-audit-and-targeted-maintenance).
No additional implementation is currently selected. Final verification of the v1.2.6 closure
criteria remains open; completion of this investigation does not close the whole milestone.

## Implemented in `v1.2.4`

- UI instrumentation uses resources and existing semantics for ordinary controls, with representative Italian execution and production Notification Center content.
- Optimized release fixes preserve the Glance episode callback, give changed-title detail intents their own navigation entry, and keep active widgets observing local changes.
- Home retains cached content and offers local Retry when calendar, membership or Continue Watching reads fail. Shared progress consumers stay subscribed through transient errors and receive recovery.
- Statistics derives radar sizing from the actual container, preserving full-width phone rendering.

See [v1.2.4 implementation notes](release-notes-1.2.4.md) for the historical delivery record. The selected hardware-efficiency follow-up is recorded in v1.2.5 above; the next global audit belongs to v1.2.6. Older open-gate statements do not reopen the agreed v1.2.5 scope.

## Implemented in `v1.2.3`

- Date actions wrap rather than lose the clear-date label at narrow widths and larger fonts.
- Actual Favorite toggles use a consistent selected state; shortcuts and filters keep their distinct roles.
- Search and Home describe saved membership as Collection, preserving genuine Watch Later and other tracking states.
- Details artwork is decorative beside the visible title; notification thumbnails use the shared poster rounding.
- Statistics opens from both direct Collection launcher shortcuts, with the existing Back/Up path preserved.
- Populated Your bingee Watching/Favorites shelves use saveable, typed provider keys, fixing a crash found by the final full Android suite.

See [v1.2.3 release notes](release-notes-1.2.3.md) for the changed behavior. The canonical checklist records verification and release readiness separately.

## Delivered in `v1.2.0`

- The Details screen rebuilt around a backdrop hero, with a top app bar that fades in over the artwork on scroll and one scrolling list for seasons and episodes.
- Continue Watching after the Home featured rows, showing the last watched and next episode and marking the next one watched in one tap, with undo.
- Two home screen widgets: a 2×2 poster of the series in progress with a next-episode button, and a 4×2 view that adds the next two releases. Both follow the in-app theme choice (ADR 0027).
- Oswald for headings and section titles, Inter for body text; the watchlist is marked with a bookmark throughout.
- A root surface that keeps screens outside a Scaffold, such as the startup check, readable in the dark theme.

## Delivered in `v1.1.0`

- English and Italian localization with active `MissingTranslation` lint coverage.
- Home, Search, Your bingee, Continue Watching, Notification Center, and settings subpages.
- The Your bingee redesign of the personal top-level destination: actionable Watching, collection shortcuts with counts, Favorites, and a personal statistics preview. The destination keeps its internal `profile` route.
- Statistics 2.0: exact viewing analytics, a monthly histogram for a selected year, and a personal ratings histogram with selected-result shelves. The taste radar, genre ranking, and profile genre podium use restored provider-qualified genre identity; Backup v2 preserves genres offline. Legacy names without IDs remain uncounted until a Details refresh.
- Branding and system-UI work: the current bingee light/dark theme, edge-to-edge shell, and system-bar icon appearance following the active theme.
- Release-build optimization is configured in `app/build.gradle.kts` (`release { optimization { enable = true } }`), so R8 is no longer a separate pending batch.
- Room v5 local database, local release calendar, local notifications, Backup v2 (v1 import supported), TV Time additive import, and manual GitHub update checks.
- Canonical serial state and a low-risk Details Abandoned control.

## Current data portability hardening

- Current Room v9 stores portable appearance, spoiler, and collection-view settings with the other restorable preferences. Room v8 introduced their one-time DataStore bridge; v9 adds recoverable technical notification claims. Backup v3 exports portable data, keeps v1/v2 imports compatible, and rejects documents that exceed the 50 MiB or v3 record ceilings. TV Time shares the current 50,000-media, 100,000-season, and 100,000-episode limits. Legacy v1/v2 episode reads retain their 500,000-record ceiling.

## Deferred

- A large (4×4) home screen widget remains to be designed.
- `La tua storia` / History, a year recap, and a later viewing-mode split remain planned rather than implemented.
- FTS, cache eviction infrastructure, generalized job-management abstractions, and other P3-7 optimizations remain measurement-driven work.
- Accounts, cloud sync, social features, streaming availability, recommendations, Jikan, and cross-provider deduplication remain out of scope.
