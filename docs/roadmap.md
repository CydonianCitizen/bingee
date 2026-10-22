# bingee Roadmap

This document retains earlier delivery summaries. The [canonical development roadmap and checklist](../BINGEE_DEVELOPMENT_ROADMAP_AND_CHECKLIST.md) defines the active implementation order and release gates.

## Completed agreed scope in `v1.3.0` — 2026-10-22

Details, current History and the approved Home/calendar addition are verified and closed
within the documented scope. Final global Ponytail audit finds no new demonstrated blocker;
canonical docs and version-specific checklists are aligned. Room 9 and Backup v3 are unchanged.
The [closure record](../BINGEE_DEVELOPMENT_ROADMAP_AND_CHECKLIST.md#v130-closure-record--2026-10-22)
records 415 Android tests, 535 JVM tests (reused in the final host gate), Home EN/IT checks,
owner-led Details/History TalkBack and unsigned release/R8 proof. Retained limits include manual
Home TalkBack, full Cartesian coverage, real provider failure/recovery and signed release runtime
smoke. Tablet verification is explicitly excluded by the owner, not outstanding closure work.
Manual Details/current History TalkBack remains completed and accepted. Existing performance/index/cache
deferrals and v1.3.1–v1.3.4 remain separate.
No commit, push, signing or publication in this closure task.

### Earlier v1.3.0 checkpoints

The dated records below preserve their original pending gates; the closure record above supersedes them.

The code targets 1.3.0/code 11. Details now places a compact personal summary before the synopsis,
derives Series relationship wording without changing canonical tracking rules, opens rating editing
on intent, and navigates to the projected next episode. Meaningful completion dates require an actual
record; season refresh scope and long-title allocation are clearer. The hero, Room 9, Backup v3,
cache and backup retention policies are unchanged. See the [implementation notes](release-notes-1.3.0.md)
and [M1–M5 plan](superpowers/plans/2026-10-19-v1.3.0-details.md).

The Details checkpoint passes: 508 JVM and 287 isolated Android 13 tests, Spotless, lint, debug and
unsigned release/R8 builds. Source audit and a fresh-process offline smoke with synthetic data pass.
Adaptive coverage remained partial at that checkpoint after repeated host QEMU crashes; TalkBack
was still pending then, and remaining navigation/lifecycle combinations need verification. The milestone remains open. No commit
or publication is implied; see the notes for the exact coverage and rejected screenshot captures.

The default Home calendar summary displays one release per Series: today's/earliest future,
otherwise the latest recent release. Movies remain visible; persisted events, widgets and
notifications retain their existing behavior. On 2026-10-22 the owner approved four refinements:
Today/Upcoming/Recent order, compact calendar rows and dates, access to all locally observed saved
events, and clearer empty/cache/update wording. These are implemented; subsequent native checks
pass with 15 Home tests per EN/IT locale and eight controlled width/font/theme variants. The owner subsequently approved moving Continue
Watching after the calendar and before the featured Movies and Series rows; this is implemented
without changing continuation policy or actions.
The wider v1.3.1 Home redesign remains separate. The earlier Home checkpoint passes 511 JVM
tests, Spotless, lint (zero errors, 38 existing warnings) and debug build. Android and release/R8
checks above predate this Home change; the later full 415-test Android and release/R8 checkpoint
below covers the current source snapshot.

The subsequent M1 derivation-test item is verified, bringing the current full JVM suite to 513
passing tests; Spotless, lint and debug build pass. Production is unchanged by this test-only
follow-up. The subsequent M2 pass adds seven shared debug preview fixtures and verifies hierarchy
on HONOR CMA-LX1 / Android 13: seven EN hierarchy tests and 33 selected IT Details/preview tests pass.
The scoped native visual variants are documented in the notes. The subsequent Resume change uses
a primary filled Material 3 button for abandoned Series; Abandon/removal remain secondary. Its
new phone regression passes after failing before the fix; 34 selected IT Details/preview tests
pass. The later M2 residual pass verifies the functional checklist with real Activity recreation,
navigation back and Room rating/timestamp checks, plus pending/error/duplicate-touch regressions.
All 7 selected EN navigation and 35 IT Details/preview tests pass; the host gate passes 514 JVM
tests, Spotless, lint (zero errors/38 existing warnings) and debug/AndroidTest builds. Production
is unchanged by this verification step. M3, the remaining native matrix, TalkBack and final
audit/release gates stay open; this does not close v1.3.0.

The first M3 test item is subsequently verified on the same isolated phone installation: 2
focused EN and 36 IT Details/preview tests pass, covering closed/open seasons, missing rows,
loading, error and explicit retry before episode navigation. The standard host gate passes
515 JVM tests, Spotless, lint (zero errors/38 existing warnings) and debug/AndroidTest builds,
with production unchanged. M3 target invalidation and assistive-technology checks remain open.

The subsequent 2026-10-20 M3 stale-target check passes 2 new EN and 38 IT Details/preview tests
on the same phone: disappearing targets and title changes cancel pending commands; subsequent
data does not restart scrolling, and a fresh Go reaches the valid row. Production is unchanged;
the host gate passes, reusing 515 unchanged JVM tests as UP-TO-DATE. M3 assistive-technology
checks, later phases and the milestone remain open.

The final M3 row check subsequently passes on HONOR / Android 13 at 360 dp/font 1.0: the row
fits below the toolbar, exposes one actionable accessible toggle, and retains scroll/focus on
spontaneous refresh. Two focused EN/IT runs use active TalkBack; all 39 ordinary IT Details/preview
tests pass. The host gate passes with 515 unchanged JVM tests reused as UP-TO-DATE. Production
and personal accessibility settings are preserved. The scoped M3 checklist is verified; manual
gesture/audio checks and the complete adaptive matrix are not implied. M4–M5, Home/calendar,
global gates and v1.3.0 remain open.

The first M4 content item subsequently passes on the same phone at 360 dp/font 1.0: 2 new
focused EN and 41 IT Details/preview tests cover wrapping long titles, text spoiler rules,
disabled future rows, separate specials and localized season-refresh messages with saved rows
retained. Production is unchanged; the host gate passes with 515 JVM tests reused as UP-TO-DATE.
M4's adaptive thumbnail/font matrix and complete per-season loading/error/retry/offline check
remain open, as do M5, Home/calendar and the global/release gates.

The subsequent M4 adaptive check passes all 18 EN/IT width/font combinations (320/360/411 dp,
font 1.0/1.3/1.5) on the phone through controlled Compose-window configurations: 36 focused
executions and 42 ordinary IT Details/preview tests pass. Existing thumbnail omission protects
text space; the visible 411 dp/font 1.0 branch verifies actual spoiler blur with a local synthetic
image. Production and system display/font settings are unchanged; the host gate passes with
515 JVM tests reused as UP-TO-DATE. M4 per-season loading/error/retry/offline, the broader M5
matrix, Home/calendar and global/release gates remain open.

The final scoped M4 item subsequently passes: per-season loading/error/retry leaves another cached
season readable, and an actual disconnected Room/Activity test preserves two saved seasons after
failed refresh, explicit retry and recreation. The isolated app has no credential, so that refresh
fails before networking; socket errors and successful retry are separately controlled fixtures.
Three focused EN tests, 43 IT Details/preview tests and 8 navigation tests pass, alongside 29 focused
and 515 unfiltered JVM tests actually executed, Spotless, lint and debug/AndroidTest builds.
Production is unchanged; phone connections and package inventory are restored. The local M4
checklist is verified. M5, Home/calendar, the wider matrix and global/release gates remain open;
neither process-death coverage nor milestone closure is implied.

The first M5 state-matrix item subsequently passes on the same phone at 360 dp/font 1.0, IT/Light:
67 state cases (52 valid membership/rating/favorite combinations and 15 targeted metadata/cache
cases), one test with six local-read loading/error transitions, and 44 Details/preview regressions.
Controlled UiState fixtures exercise real native UI; these are not live-provider or Room persistence
checks for every combination. Production is unchanged; Spotless, lint and debug/AndroidTest builds
pass, reusing the unchanged 515 JVM tests as UP-TO-DATE. Temporary packages are removed and the
phone inventory preserved. The separate language/theme/adaptive/editor matrix, assistive-technology,
integration, Home/calendar and global/release gates remain open. M5 and v1.3.0 remain open.

The second M5 item subsequently passes all 48 EN/IT x Light/Dark x 320/360/411/600 dp x
font 1.0/1.3/1.5 configurations for representative Movie/Series long-content and open-editor
fixtures: 96 focused executions and 46 ordinary IT Details/preview regressions pass. Actual
font scale is checked inside the date dialog as well. These are controlled configurations on
the phone, including process-local resource overrides restored after each test, not physical
tablet or full 67-state Cartesian coverage. Production and personal settings are unchanged;
host checks pass with the unchanged 515 JVM tests reused as UP-TO-DATE. Temporary packages
are removed. M5 TalkBack, integration/offline/concurrency, Home/calendar and final audit/release
gates remain open; neither M5 nor v1.3.0 is closed.

The 2026-10-21 M5 accessibility checkpoint passes eight focused runs with active TalkBack in
EN/IT and 49 ordinary IT Details/preview regressions on the HONOR / Android 13. Native roles,
state, editor cancellation, touch targets and episode focus are covered without production
changes. An injected swipe did not advance focus; reading order, gestures and speech still
require the owner's manual check, deferred by the owner. The M5 checkbox remains open.
The isolated synthetic app stays ready; its runner is removed and original accessibility
settings restored. See the [checkpoint and limits](release-notes-1.3.0.md#m5-accessibility-checkpoint--2026-10-21).
Integration/offline/concurrency, Home/calendar, final gates and milestone closure remain open.

On 2026-10-22 the owner completes the manual Details checks: Movie rating/date editors cancel
without changing values and leave useful focus; title/actions are understandable. Series S1 E2
navigation, episode name/state, season exploration, return and Favorite state announcements pass
by the owner's report. The owner also confirms chronological reading/scrolling of both timeline
sides, Movies/Series filters, group expansion/collapse and Details return, and explicitly accepts
the current compact alternating timeline. The scoped M5 accessibility and History acceptance
checkboxes are complete. These are English-UI manual checks on HONOR / Android 13, 360 dp/font 1.0,
not global accessibility certification. The two existing isolated apps were updated without
INTERNET; all five original accessibility settings and package inventory are verified restored/
unchanged. Production is unchanged by this verification. The subsequent scoped integration
checkpoint below completes offline/concurrency; native Home verification and final gates remain open.

The subsequent 2026-10-22 Details integration checkpoint passes 11 native regressions on the same
HONOR Android 13 phone: metadata/progress/date concurrent Room writes, real Activity recreation,
rating draft/save/back and cached-season retry, plus episode actions reflected in both Home and
the Watching collection. A separate seed and two force-stopped process launches have distinct PIDs;
saved Movie date/rating and Series cache remain readable, a rating change to 9 and an episode write
survive another cold process, and the future episode remains unwatched. The isolated app has no
INTERNET permission; personal Wi-Fi/mobile/airplane settings are untouched. This proves app-level
offline behavior, not real TMDB socket failure/recovery. Fixture seeding now uses a debug-only
entry point to the app's Room singleton so shared observers receive its invalidations. No production
logic changes. All 535 JVM tests execute and pass; Spotless, lint (0 errors/54 warnings) and debug/
AndroidTest builds pass. The two newly created packages are removed; package inventory and eight
accessibility/radio settings match baseline. Native Home and full Android/R8 were still pending
at this integration checkpoint; the subsequent verification below completes them.

The subsequent 2026-10-22 point 4 checkpoint passes the complete unfiltered
`connectedDebugAndroidTest`: 415 tests, zero failures/errors/skips on HONOR / Android 13. Home
also passes all 15 component tests in each of EN/IT and eight controlled native compositions:
320 dp/IT/font 1.5, 360 dp/IT/font 1.3, 411 dp/EN/font 1.0 and 600 dp/EN/font 1.5, each Light/Dark.
Mode switching, title callbacks, section order, cached-read error, empty/loading and long copy are
exercised; eight screenshot samples are inspected from 64 captures. These are phone compositions,
not a physical tablet, full Cartesian matrix or manual Home TalkBack pass. Standard host checks
pass with the unchanged 535 JVM result reused. `assembleRelease` actually runs R8 and produces
the frozen unsigned 1.3.0/code 11 APK; no release installation/signing/smoke is claimed. Only the
new isolated packages are removed; package inventory, nine settings and display size/density
match baseline. No production change. Global audit and final documentation/milestone closure
remain open; commit, push and publication are separate explicit actions.

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

## Completed agreed scope in `v1.2.6` — 2026-10-19

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
No additional implementation is selected. Final scope review, a Ponytail maintenance pass over
the documentation changes, stored-evidence validation and the selected-scope checks are complete.
The agreed v1.2.6 maintenance scope is closed on 2026-10-19 without an app-version change or
publication. Spotless, JVM, lint and debug-build gates succeeded, with JVM results reused by Gradle
as UP-TO-DATE; no new Android suite or cache experiment was required for this documentation-only
change. The [closure record](../BINGEE_DEVELOPMENT_ROADMAP_AND_CHECKLIST.md#v126-closure-record--2026-10-19)
retains verification limits and deferred work. No later milestone is started by this closure.

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

## History addition in v1.3.0 — in progress

On 2026-10-21 the owner approved correction to one continuous horizontal timeline, oldest retained
dated Movie/Episode first and newer activity toward the right, with regular spacing and expandable
Series/day groups. Automatic bounded loading replaces the month/year controls and main page buttons.
Implementation, automated checks and scoped visual validation are complete: 530 JVM tests, focused
native/restore/adaptive/offline checks, Spotless, lint and debug/test builds pass as detailed in the
plan. On 2026-10-22 the owner confirms the manual TalkBack pass and accepts the current compact
alternating interaction, including filters, reading order, scrolling, groups and Details return.
The H1–H6 evidence below applies to the earlier monthly UI. Details M5 manual checks also pass in
their exercised scope; the subsequent scoped integration/offline/concurrency checkpoint also passes.
The later native Home/full Android/release-R8 checkpoint also passes in its documented scope;
global audit and final v1.3.0 closure remain open.

The owner's subsequent refinement keeps only Movies/Series, opens on Movies and alternates
content above/below a central rail. Existing single-type saved selections and bounded loading remain;
a saved combined selection clears its transient cursor/groups/anchor and restarts on Movies.
Room, date rules, backup format and application version remain unchanged.
The next approved refinement compacts each card, centres its date near the point and corrects
filter, group-toggle and empty-state wording. Series/day groups keep their existing inline
expansion and internal Episodes filter; no separate episode panel is introduced.

On 2026-10-20 the owner assigned [History H1–H6](superpowers/plans/2026-10-20-history-timeline.md)
to v1.3.0/code 11. This is an offline diary of retained watched progress, with grouped episodes,
month/type selection and bounded replacement pages. Movie manual dates take precedence in both
the diary and monthly statistics. Repeat viewings and manual episode dates remain outside scope.
The implemented read model uses one Room transaction for overview, parents and requested children,
with replacement pages of 30 rows and 20 Episodes per open group. The destination-owned ViewModel
retains filter/month/anchor and resets transient local-ID state across restore. It does not retain
another complete viewing dataset in Profile. Room 9, Backup v3 and later planned version codes
remain unchanged. History H1–H6 verification is recorded in the plan, including the owner-reported
positive TalkBack check on the exercised controls. This closes the scoped History implementation
and verification; the overall v1.3.0 milestone and its independent Details/Home gates remain open.

## Deferred

- A large (4×4) home screen widget remains to be designed.

- A year recap and a later viewing-mode split remain planned rather than implemented.
- FTS, cache eviction infrastructure, generalized job-management abstractions, and other P3-7 optimizations remain measurement-driven work.
- Accounts, cloud sync, social features, streaming availability, recommendations, Jikan, and cross-provider deduplication remain out of scope.
