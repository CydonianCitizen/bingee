# v1.3.0 — Details and History implementation notes

Status: previous agreed scope verified and closed on 2026-10-22. The owner subsequently reopened
v1.3.0 for M6–M8; M6 and M7 are verified in their documented scopes; final M8 remains planned. Code version 1.3.0/code 11.
No release has been published. Earlier dated checkpoints below retain their then-pending gates;
the [final closure record](../BINGEE_DEVELOPMENT_ROADMAP_AND_CHECKLIST.md#v130-closure-record--2026-10-22) supersedes them. The [canonical roadmap](../BINGEE_DEVELOPMENT_ROADMAP_AND_CHECKLIST.md#v130--major-uiux-1-details-as-a-true-personal-tracker-screen)
and the [Details](superpowers/plans/2026-10-19-v1.3.0-details.md) and
[History](superpowers/plans/2026-10-20-history-timeline.md) implementation plans record the delivered scope and evidence.

## M6 — Episode Details and editable viewing date

The episode list retains its checkbox and adds a dedicated Details action. The new cached page
shows episode metadata, respects the spoiler preference and offers watched/unwatched and viewing
date editing. It reuses the parent Details ViewModel, existing date dialog and Room observations.
Date correction changes only the existing `watched_at`, with a timestamp comparison to reject
stale writes; unwatch is never undone by an old editor. Local time/precision are retained in the
current device zone. Series completion dates, counts, Room 9 and Backup v3 remain unchanged.
The shared date dialog also retains its unsaved choice/date across recreation.

The [canonical M6 record](../BINGEE_DEVELOPMENT_ROADMAP_AND_CHECKLIST.md#m6--episode-details-and-editable-viewing-date)
documents tests, native compositions and limits. Its 99 distinct Android cases are scoped
regressions, not a new complete-app suite. The new page has no manual TalkBack certification;
the earlier owner-accepted Details/History checks retain their original scope. The renewed final
global audit/release gates remain M8. No commit, signing, publication or personal-app installation.

## M7 — Home, Your bingee, Search and genre presentation

Home moves its saved-refresh timestamp below discovery. Your bingee aligns its title with
Settings using Home's naturally sized header; History spans both Collection columns using the same tile component as the other sections,
with its icon and label centred together as one group and no count. Empty shelves use centred outlined Search buttons. The Search page title becomes
Search/Cerca, retaining TMDB attribution. The extended Statistics ranking initially shows four
genres and expands on See more only when needed; expansion survives recreation and resets on
media-scope changes. Ranking identities/counts, radar, podium and persistence are unchanged.

The [canonical M7 record](../BINGEE_DEVELOPMENT_ROADMAP_AND_CHECKLIST.md#m7--focused-home-your-bingee-and-search-layoutcopy)
records 59 distinct native production cases per EN/IT locale, representative theme/width/font
compositions, 540 JVM tests, Spotless, lint, debug/AndroidTest builds and scoped Ponytail review.
No complete-app Android rerun, manual TalkBack or final release/R8 claim is made here. Room 9,
Backup v3 and version/code remain unchanged; no commit or publication. Global final audit and
renewed closure remain M8.

## Implemented behavior

- The existing hero precedes a personal summary, synopsis, Series seasons/episodes, and secondary metadata. Favorite stays in the toolbar; rating editing opens on intent.
- Series wording distinguishes Watch Later, Watching, Abandoned, Caught up, Watched and neutral available-episode completion. Coverage limitations qualify local counts. Existing canonical tracking rules are unchanged.
- Next-episode context uses the existing projected position and shared eligibility policy. Navigation opens the relevant season and reaches its lazy row once; it does not mark an episode watched. Unwatched titles remain hidden with spoilers enabled.
- Watched dates belong to watched Movies. The date is shown once with a tonal Edit action; marking unwatched is secondary. Series date editing requires an actual completion record and matching complete progress; a record removed during editing causes an explicit failed write. Ongoing Series dates refer to available episodes. Clearing a custom date preserves automatic completion evidence.
- Rating Save/Remove preserve 1–10 semantics; Cancel restores the saved value without writing. Read/write errors preserve the draft. Membership read errors offer a separate local retry.
- Cached episode rows remain usable without a fetch timestamp. Narrow/large-font rows omit decorative thumbnails to preserve title space; wider rows retain them. Long next-episode titles are visually limited to two lines without shortening their semantic text. Title refresh and season episode refresh have distinct EN/IT labels.

## Compatibility and verification limits

Room 9, Backup v3, v1/v2/v3 restore, dependencies, cache and backup retention are unchanged.
No new provider, continuation algorithm or whole-list collector is introduced.

## Details verification checkpoint — 2026-10-19

- 508 JVM tests pass. Spotless, lintDebug (0 errors, 38 warnings), debug, AndroidTest and unsigned release/R8 builds pass. The final delivery run reuses unchanged release work; the preceding full run executed R8.
- All 287 Android instrumentation tests pass on an isolated Android 13/API 33 AVD at 411 dp, including Details, Room and migration tests. The test package is separate from the personal installation. Two later JVM-only coverage tests do not change the production snapshot verified on Android.
- Read-only global Ponytail source audit and final change review found no remaining introduced source defect. This is distinct from completing runtime accessibility/adaptive gates.
- A separate real-app smoke with two synthetic titles and networking disabled verifies cached Movie/Series Details from a new process. Marking the next Series episode persists across another cold process start; Details then shows Caught up and actual available-episode completion evidence. The future episode remains disabled, and Home keeps its future release without offering continuation for the now-complete available episodes.
- Native 320 dp/Italian/font 1.5 inspection informed and confirmed the narrower episode layout and Movie action hierarchy. Adaptive coverage remains partial: some screenshot captures contained previous compositor frames and were rejected. Repeated Windows QEMU access violations also interrupted the matrix, including at fixed resolution after the successful full suite and offline smoke. They are infrastructure failures, not failed app test assertions.

The milestone remains open. Complete the documented 320/360/411/600 dp, EN/IT, Light/Dark and large-font/editor matrix on a stable Android environment; verify TalkBack and the remaining navigation/error/lifecycle combinations. The real-app rating-write smoke was interrupted by QEMU and is not claimed passed; automated rating, draft, cancellation, error and recreation tests passed. No publication or signed-artifact smoke is claimed.

Temporary logs, screenshots, audit reports and synthetic fixtures remain ignored under `.audit/`.
Existing v1.2.5 performance limits and the deferred index evaluation are not reopened.

## Subsequent Home calendar addition — 2026-10-19

Home shows one release per source-qualified Series across date groups: today's/earliest future
release, otherwise its latest recent release. Same-day ties prefer an episode over a season
premiere, then season/episode number and the existing deterministic ordering. A premiere can
still represent the nearest known release. Movies remain uncollapsed. Selection is local to
`HomeViewModel`; persisted events, widgets, notifications, schema and backup are unchanged.

Reviewing and agreeing the Home/calendar presentation remains an open **v1.3.0** activity.
This small change does not implement that wider review or close the milestone.
The updated host gate passes: 511 JVM tests in 83 suites (zero failures, errors or skips),
Spotless, lintDebug (zero errors, 38 existing warnings) and assembleDebug. Three new HomeViewModel
regressions fail before the fix and pass after it; they cover many releases, same-day selection,
qualified identity, Movie/Series ID collisions, recent/premiere fallback, date rollover and updated
repository emissions. The focused Home/calendar/widget JVM run also passes. A scoped Ponytail
review finds no additional production layer or persistence/network operation; this does not
replace the milestone's final global audit or native verification.
The Android instrumentation, native smoke and release/R8 evidence above predates this Home change;
those checks have not been repeated for it.

## Subsequent M1 test coverage — 2026-10-19

The first open M1 derivation-test item is now verified: zero-episode and relationship-precedence
cases were added; insufficient coverage with an old completion record and Watching wording were
strengthened. Production is unchanged. The focused run passes 57 JVM tests; the current full host
gate passes 513 tests in 83 suites (zero failures/errors/skips), Spotless, lint (zero errors,
38 existing warnings) and debug build. M2 preview/visual checks are next. No Android device is
connected; no native or R8 rerun is claimed. The milestone and Home/calendar review remain open.

## Subsequent M2 previews and hierarchy — 2026-10-19

Seven debug-only fixtures now cover unwatched/watched Movies, Watching/Caught up/Abandoned Series,
outside-collection history and insufficient metadata. Preview tooling and seven parameterized
Android tests share these fixtures; the production composables are unchanged. On an isolated
package on HONOR CMA-LX1 / Android 13, all seven hierarchy tests pass in EN and all 33 selected
Details/preview tests pass in IT. They verify hero/personal/synopsis order, unique membership,
Favorite and Series actions, expected relationship wording and an initially collapsed rating editor.

Native captures were inspected at EN/360 dp/font 1.0/Light, IT/360 dp/font 1.3/Dark and
IT/320 dp/font 1.5/Light. The physical display remains 720×1600 px at 320 dpi (360 dp wide);
font, width and theme variants are overrides inside the test composition, not changes to phone
preferences or proof of the complete B3 matrix. Capture tools/images remain ignored under `.audit/`.
The review confirms an already-open M2 item: Resume tracking still uses a secondary TextButton
and needs the primary prominence required by the plan. No production fix is included in this step.
M2, TalkBack, the remaining native/editor/lifecycle matrix and the milestone remain open.
The final host gate passes 513 JVM tests (83 suites, zero failures/errors/skips), Spotless,
lintDebug (zero errors, 38 existing warnings), debug and AndroidTest builds. Standard package
identities are restored in the build outputs; the temporary probe is excluded. The two M2-only
phone packages created for this pass were removed; existing packages and display preferences remain.

## Subsequent M2 primary Resume action — 2026-10-19

The open Resume-prominence item above is resolved: abandoned Series in the collection use the
standard primary filled Material 3 Button. Abandon and collection removal remain secondary.
The existing callback, membership condition and updating-state guard are preserved; Room,
tracking rules, backup, cache and strings are unchanged.

On HONOR CMA-LX1 / Android 13, a new regression fails before the fix because Resume lacks the
primary fill and passes after it. It also checks one Resume action, the callback, the disabled
second touch while pending, and secondary Abandon after resumed state is observed. The focused
EN test and all 34 selected IT Details/preview tests pass. Actual native captures confirm
EN/360 dp/font 1.0/Light and IT/320 dp/font 1.5/Dark rendering; the latter uses a Compose test
container/font/theme override on the same physical 360 dp phone, not a separate device configuration.

The current standard host gate passes 513 JVM tests in 83 suites (zero failures/errors/skips),
Spotless, lintDebug (zero errors, 38 existing warnings), debug and AndroidTest builds. Temporary
capture code is excluded from normal builds; both temporary phone packages were removed, and
existing packages/display preferences are preserved. Evidence stays ignored under `.audit/`.
This closes only the M2 action-prominence item. Remaining M2 lifecycle/error combinations,
TalkBack, the complete native matrix, final global audit and release gates remain open; the
Home/calendar review is still part of v1.3.0. No R8 or whole-app Android rerun is claimed here.

## Subsequent M2 residual verification — 2026-10-19

The last M2 functional-checklist item is verified on HONOR CMA-LX1 / Android 13, in a separately
packaged test installation. Added regressions cover rapid Save/Remove commands at the ViewModel,
disabled repeated touches while pending, removal error retaining rating/draft, cancellation and
editor closure only after successful removal. Existing error/read/retry and save/restoration tests
also pass. Fault states are controlled test inputs, not induced damage to the phone's storage.

A new integration test uses the real MainActivity, Navigation graph, ViewModel and Room: a draft
survives visiting another title and Back, then actual Activity recreation (a different Activity
instance is asserted). The Room rating and timestamps remain unchanged through those operations
and Cancel. Explicit Save updates the rating; subsequent recreation/Back preserve that full record
and keep the editor closed. This tests Activity recreation, not process death.

All 7 selected navigation tests pass in EN, and all 35 Details/preview tests pass in IT. The
standard host gate passes 514 JVM tests in 83 suites (zero failures/errors/skips), Spotless,
lintDebug (zero errors/38 existing warnings), debug and AndroidTest builds. No production change
was needed; source/resource hashes match the beginning of this step. Earlier scoped long-title
and missing-artwork preview evidence remains applicable. Temporary packages were removed, existing
packages and display preferences preserved, and build outputs retain standard package identities.

M2's functional checklist is verified within this scope. The complete state/adaptive matrix,
TalkBack, process-death coverage, M3–M5, final A5 audit and release gates are separate and remain
open. The v1.3.0 Home/calendar review remains open. No whole-app Android or R8 rerun is claimed.

## Subsequent M3 episode-navigation verification — 2026-10-19

The first M3 checklist item is verified on HONOR CMA-LX1 / Android 13 in an isolated test
installation: 2 focused EN tests and all 36 IT Details/preview tests pass. The cases reach E50
in a lazy list from a closed or already-open season, keep cached rows usable after a refresh
error, and navigate again after explicit retry. With no valid local episode rows the command is
absent; the existing season control exposes loading, error and explicit retry, then navigation
becomes available when the target exists. No additional fetch is triggered just to populate it.

The ViewModel regression checks preservation of both empty and cached season content, no
automatic retry, and rejection of duplicate requests while a refresh is active. Native error
and success states use controlled fixtures; these are not live TMDB/network-failure tests.

The standard host gate passes 515 JVM tests in 83 suites (zero failures/errors/skips), Spotless,
lintDebug (zero errors/38 existing warnings), debug and AndroidTest builds. No whole-app Android
or R8 rerun is claimed for this verification.

Production sources/resources are unchanged. Temporary test packages are removed; preexisting
installation and display settings are preserved. Raw evidence stays ignored under `.audit/`.
Target invalidation/title changes, assistive-technology checks, the rest of M3–M5 and the
Home/calendar review remain open. No milestone closure, commit, push or publication is implied.

## Subsequent M3 stale-target verification — 2026-10-20

The M3 stale-target item is verified on HONOR CMA-LX1 / Android 13: 2 new focused EN tests and
all 38 IT Details/preview tests pass, including the previous error/retry paths. A disappearing
episode removes its summary command; restoring its data does not resume the cancelled command.
Switching to another Series with the same S1E50 position also cancels the old command. Expanded
rows arriving afterwards do not trigger scrolling; a fresh physical Go click reaches the correct
row, without writing watch state. These are controlled Compose state transitions, not new live
provider or real Activity navigation coverage.

Production is unchanged. The standard host gate passes Spotless, lintDebug (zero errors/38
existing warnings), debug and AndroidTest builds; Gradle reuses the unchanged 515-test JVM
suite in 83 suites as UP-TO-DATE, with zero failures/errors/skips. No whole-app Android or R8
rerun is claimed. Temporary phone packages are removed and the preexisting installation is
preserved; evidence remains ignored under `.audit/`.

The M3 reachability/assistive-technology item remains open, along with M4–M5/global gates and
the Home/calendar review. The milestone remains open; no commit, push or publication occurred.

## Subsequent M3 row accessibility verification — 2026-10-20

The final M3 checklist item is verified on HONOR CMA-LX1 / Android 13 at 360 dp and font scale 1.0.
The reached E50 row fits fully below the toolbar and inside the viewport, with a touch target
of at least 48 dp. Compose exposes one toggle with the expected role/state; the Android
accessibility tree exposes its screen-reader-focusable, actionable group and localized state.

Two focused EN/IT runs pass with TalkBack actually bound and touch exploration active, without
UiAutomation suppressing it. E50 accepts accessibility focus and activation. A spontaneous
metadata refresh preserves accessibility focus on E49 and the scroll position, with no watch
callback. Go uses semantic activation in TalkBack mode; ordinary mode uses a physical click.
These checks validate Android accessibility actions and focus, not manual gesture traversal,
auditory speech quality, other assistive services or the full device/font/theme matrix.

All 39 ordinary IT Details/preview tests pass. The standard host gate passes Spotless,
lintDebug (zero errors/38 existing warnings), debug and AndroidTest builds; the unchanged 515
JVM tests in 83 suites remain valid and are reused as UP-TO-DATE, with no failures/errors/skips.
Production is unchanged. Saved accessibility settings are restored exactly; temporary packages
are removed and the preexisting installation is preserved. Evidence stays ignored under `.audit/`.

The scoped M3 checklist is verified. M4–M5, the Home/calendar review, global audit/release gates
and v1.3.0 remain open. No whole-app Android/R8 rerun, commit, push or publication is claimed.

## Subsequent M4 content verification — 2026-10-20

The first M4 checklist item is verified on HONOR CMA-LX1 / Android 13 at 360 dp and font scale 1.0:
2 new focused EN tests and all 41 IT Details/preview tests pass. Long season/episode titles wrap
without text-layout overflow; the complete episode title can be brought into the viewport.
Saved rows remain visible/actionable during season refresh, with the localized cached-content
message and the selected season's refresh control disabled while pending. Title and season
refresh labels remain distinct.

Spoiler hiding preserves watched titles and removes unwatched, future and special titles from
the displayed rows and their unmerged text semantics. Revealing titles keeps future episodes
disabled; touching the future row generates no episode callback. Specials retain their separate
section, explicit expansion and season-zero action identity. These are controlled UI fixtures,
not new live-provider or persistence tests.

Production is unchanged. The host gate passes Spotless, lintDebug (zero errors/38 existing
warnings), debug and AndroidTest builds; the unchanged 515 JVM tests in 83 suites are reused
as UP-TO-DATE with zero failures/errors/skips. Temporary packages are removed and the original
phone package inventory/settings are preserved; raw evidence remains ignored under `.audit/`.

The adaptive thumbnail/font matrix, image-blur branch at wider widths and full per-season
loading/error/retry/offline check remain open M4 work. M5, Home/calendar, global/release gates
and the milestone remain open. No whole-app Android/R8 rerun, commit, push or publication occurred.

## Subsequent M4 adaptive thumbnails and blur verification — 2026-10-20

The M4 adaptive-layout item is verified on HONOR CMA-LX1 / Android 13: all 18 EN/IT combinations
of 320/360/411 dp and font scales 1.0/1.3/1.5 pass 36 focused test executions. These are controlled
Compose-window density/font configurations on one physical phone; system display/font settings
remain unchanged. The existing thumbnail yields its space at 320/360 dp and at font 1.3/1.5;
it remains visible at 411 dp/font 1.0. Long titles wrap without overflow and can be reached by scroll.

A synthetic local checkerboard uses the real image-loading/rendering path, without network or
new dependencies. Pixel checks and captures confirm blur on unwatched images and preserved
sharpness on watched images; spoiler hiding also removes the unwatched text. No thumbnail size,
blur value or production behavior changed. The result does not generalize to every real image,
other devices or the full M5 state/editor/theme matrix.

All 42 ordinary IT Details/preview tests pass. The host gate passes Spotless, lintDebug (zero
errors/38 existing warnings), debug and AndroidTest builds; the unchanged 515 JVM tests in 83
suites are reused as UP-TO-DATE with zero failures/errors/skips. Temporary packages are removed,
the original phone inventory/settings are preserved, and evidence remains ignored under `.audit/`.

M4's per-season loading/error/retry/offline item remains open, along with M5, Home/calendar and
global/release gates. No whole-app Android/R8 rerun, milestone closure, commit, push or publication.

## Subsequent M4 per-season and offline verification — 2026-10-20

The last scoped M4 item is verified on HONOR CMA-LX1 / Android 13 at 360 dp/font 1.0. Loading,
NetworkUnavailable and explicit retry for one season keep another cached season readable;
the callback targets only the requested season. Existing native cases also cover saved rows
through Refreshing/Error and successful row arrival after retry. These transitions use controlled
fixtures, without real provider calls. Three focused EN tests pass.

A separate real MainActivity/navigation/ViewModel/Room test runs with Wi-Fi and mobile data disabled
and verifies that Android has no active network. Both saved synthetic seasons remain readable
after failed refresh and explicit retry, with their complete stored relations and timestamps
unchanged. A real Activity recreation preserves consultation. This isolated installation has no
credential: its refresh fails as Unauthorized before networking, not as an HTTP response or a socket
failure. No live-provider recovery or process-death certification is claimed.

All 43 ordinary IT Details/preview tests and 8 navigation tests pass. The focused 29 JVM tests and
subsequent full 515-test/83-suite JVM run execute successfully, with zero failures/errors/skips.
SpotlessCheck, lintDebug (zero errors/38 existing warnings), debug and AndroidTest builds pass.
Only tests and documentation change; production, Room schema, backup and cache policy stay unchanged.
Phone radio settings are restored, temporary packages removed and original inventory preserved.
Detailed evidence and the small offline runner remain ignored under `.audit/`.

The local M4 checklist is verified. M5, Home/calendar, the full state/theme/adaptive matrix and
global/release gates remain open. No whole-app Android/R8 rerun, milestone closure, commit, push
or publication occurred.

## Subsequent M5 state-matrix verification — 2026-10-20

The first open M5 item is verified on HONOR CMA-LX1 / Android 13 at 360 dp/font 1.0, IT/Light.
The new native matrix passes 67 cases: 16 Movie and 36 Series combinations of meaningful viewing
state, collection membership, rating absent/present and favorite off/on, plus 15 targeted cases.
Abandoned Series are tested in collection; impossible Movie/Series state combinations are excluded.
The targeted cases cover canceled/ended/unknown production wording, incomplete coverage inside and
outside collection, missing local seasons, future-only episodes, a retained unwatched special after
regular completion, and Movie/Series stale cache through idle, refreshing and failed refresh.

Assertions verify localized state and membership/watch/tracking actions, compact rating, favorite
toggle state, incomplete-data warnings, saved-data banners and absence of unsolicited callbacks.
A separate test verifies six local-read loading/error transitions for Movie progress, rating and
Series content without hiding membership. All 44 ordinary IT Details/preview regressions also pass,
including full loading/error/retry, spoilers, future rows and specials. Nine representative native
captures are inspected; the tests use controlled UiState fixtures with real UI, not live API calls
or a Room persistence scenario for every state combination.

Only tests and documentation change. The host gate passes SpotlessCheck, lintDebug (zero errors/38
existing warnings), debug and AndroidTest builds. The unchanged 515 JVM tests in 83 suites, with
zero failures/errors/skips, are reused as UP-TO-DATE. Temporary packages are removed and the original
phone inventory is preserved; detailed matrix, logs and captures remain ignored under `.audit/`.

The first M5 checkbox is verified. This is not the Cartesian product with every provider error,
language, theme, density, font or editor state. The separate adaptive/editor matrix, TalkBack,
fresh-process offline/concurrency/integration, Home/calendar and final global/release gates remain
open. No whole-app Android/R8 rerun, M5/milestone closure, commit, push or publication occurred.

## Subsequent M5 adaptive/editor verification — 2026-10-20

The second M5 item passes on HONOR CMA-LX1 / Android 13: all 48 combinations of EN/IT,
Light/Dark, 320/360/411/600 dp and font 1.0/1.3/1.5 execute representative Movie and Series
fixtures successfully, for 96 focused executions. The fixtures include long content, an expanded
long episode title, loading/error and missing local season metadata. Rating and watched/completion
date editors preserve cancel-without-saving and explicit confirmation. Text layout checks verify
the requested font inside the separate Android dialog, not only the main content. Sixteen native
captures are collected and representative samples inspected.

Only the test process receives density/font/night-mode resource overrides; original resources
are backed up and restored after each case. Personal display/font settings stay unchanged.
These are simulated window configurations on one phone; 600 dp does not certify tablet hardware.
The full 48-way product applies to these two fixtures, not all 67 states, every custom date-picker
branch, TalkBack or all application screens. Calibration attempts are excluded from final coverage.

All 46 ordinary IT Details/preview tests pass after an incomplete run reporting missing Compose
hierarchies. ADB had reconnected; a fresh isolated control and complete rerun pass without product
changes. SpotlessCheck, lintDebug (zero errors/38 existing warnings), debug and AndroidTest builds
pass. The unchanged 515 JVM tests are reused as UP-TO-DATE. Production, Room and backup stay
unchanged; temporary packages are removed and the original phone inventory preserved.

M5 TalkBack, fresh-process offline/concurrency/integration, Home/calendar and final audit/release
gates remain open. Evidence stays ignored under `.audit/`. No whole-app Android/R8 rerun,
M5/milestone closure, commit, push or publication occurred.

## Horizontal History correction — 2026-10-21

Your history now opens at the oldest retained dated Movie/Episode and scrolls continuously toward
newer activity on the right. Dated connected points, title/poster content and expandable Series/day
groups replace month/year selection and main page controls. Regular spacing is not an elapsed-time
scale. Automatic loading retains at most 90 parent items with 20 children per expanded group;
Details return preserves the viewport. Existing date, progress, special, restore and offline rules
remain unchanged, as do Room 9, Backup v3 and dependencies.

The initial repeated monthly-query approach was rejected after a 100,000-episode read took
33,888 ms on the HONOR Android 13 test phone. One all-history aggregate with exact Java ZoneRules
offsets reduces the same read to 840–880 ms over three samples (median 857 ms). This is debug,
in-memory Room transaction timing with one open group, not UI/frame, heap, battery or disk evidence.
The [History plan](superpowers/plans/2026-10-20-history-timeline.md) records sample limits and fallback
behavior. Permanent tests cover exact date transitions and a bounded query count across 120 months.

Final checks: 530 JVM tests with zero failures/errors/skips; Spotless and debug/AndroidTest builds
pass. Lint passes with 0 errors and 54 warnings, including a known History screen-width warning.
The native EN batch passes 25 tests (including 14 DAO); the latest UI/navigation IT batch passes
8 tests. Four controlled width/font/theme/language configurations pass; 320 and 600 dp/font 1.5
are repeated after fixing multiline episode-button clipping. A fresh process without INTERNET
shows the synthetic 2016–2026 Room history, opens a group and returns from Details (6.894-second
test execution, not an application launch-time measurement). Scoped visual review confirms the
timeline endpoints and readable text. The isolated app remains available for owner assessment;
the runner was removed and existing apps/accessibility settings were preserved.

Implementation and scoped automatic/visual verification are complete. Owner acceptance and a new
manual TalkBack check remain pending. The overall v1.3.0 milestone and postponed Details M5 manual
gate remain open. No commit, push, signing or publication was performed.

### Two-filter alternating layout — 2026-10-21

The owner's follow-up removes the combined filter from History and opens on Movies. Episodes remains
the separate Series/day view. A saved combined selection starts again on Movies without its old
cursor, groups or viewport anchor; existing Movies/Episodes selections retain their restoration.
Consecutive points place content above and below one central rail. Each card keeps its existing
vertical scroll so expanded episodes and long text remain reachable within that side. The bounded
horizontal windows, dates, queries, persistence, backup format, dependencies and app version are unchanged.

Verification: all 533 JVM tests execute without failures/errors/skips; Spotless, lint and standard
debug/AndroidTest builds pass. On the isolated HONOR / Android 13 installation, 14 Room History tests
pass, followed by 10 UI/navigation tests in each of EN/IT and 8 focused executions across four
controlled width/font/theme combinations (320/360/411/600 dp, font 1.0/1.3/1.5). Geometry checks cover
the shared rail, alternating sides and forward/reverse window replacement; expanded groups and
episode pagination are reachable on both sides. Native screenshots confirm narrow dark and wider
light layouts. These controlled phone configurations do not certify a physical tablet or manual
TalkBack. The temporary runner is removed; package inventory and accessibility/font settings are
preserved. Owner acceptance, manual TalkBack and the overall milestone remain open.

### Compact History cards and clearer copy — 2026-10-21

The owner approved compact cards, closer dates and corrected text while retaining the alternating
rail and existing behavior. Smaller posters sit beside titles; the repeated media-type line is
removed and the horizontal node width is reduced. Above font scale 1.3 decorative posters are
omitted to preserve text space. Dates use centred labels next to their points. The visible filters
are Movies/Series, with the existing internal Episodes value and Series/day grouping unchanged.
Expanded groups offer Hide episodes and empty text describes the selected media type. Episodes
continue to expand and scroll inside their own card; no dedicated panel is introduced.

Room 9, Backup v3, date rules, queries, automatic windows, Details navigation, dependencies and
v1.3.0/code 11 remain unchanged.

Verification on the isolated HONOR / Android 13 app: 28 IT tests (13 UI, one navigation, 14 Room),
14 EN UI/navigation tests and 16 focused executions across 320/360/411/600 dp with font 1.0/1.3/1.5
pass. Four new UI regressions fail before the change and pass afterward: compact dimensions without
the repeated type, actual date-text alignment/distance on both sides, group-toggle wording and
type-specific empty states. Existing tests retain automatic forward/reverse windows, Details return,
recreation and episode pagination on both sides. The full JVM suite executes 533 tests without
failures/errors/skips. Native screenshots confirm the compact layout in light/dark and the large-font
poster omission. These are controlled phone configurations, not physical tablet or manual TalkBack
proof. The temporary runner is removed; package inventory and accessibility/font settings are preserved.
The final host gate completes on 2026-10-22: Spotless, lint and standard debug/AndroidTest builds
pass. Owner acceptance and manual TalkBack remain pending; v1.3.0 is not closed by this increment.

## Approved Home/calendar refinements — 2026-10-22

The owner approved four scoped refinements: Today/Upcoming/Recent presentation, compact release
rows and date labels, access to all saved events already observed locally, and clearer empty,
cached-read-error and update text. Home keeps the current one-release-per-Series overview as the
default. An optional Show all / Show summary control uses complete groups from the same coherent
Room snapshot and read window (seven previous days and cached future dates); no extra observation,
provider call, schema or portable preference is added. Read failures retain both views together.
The presentation mode uses Compose saveable state. Date headings retain the year and full
accessible date; decorative posters yield text space above font scale 1.3.

After clarification the owner also approved the section reorder on 2026-10-22: calendar,
Continue Watching, featured Movies, featured Series. Only the existing UI blocks move; continuation
eligibility, actions and omission when empty are preserved. Widgets, notifications, follow and
continuation policies, Room 9, Backup v3, dependencies and v1.3.0/code 11 are unchanged.
Native UI, visual/adaptive and TalkBack checks remain pending; no Android device was connected
at this Home implementation checkpoint. The later manual Details/History pass does not cover them.
Neither milestone closure nor commit/push/publication is implied.

Host verification executes all 535 JVM tests without failures/errors/skips, including 20 Home
ViewModel tests. The Home category-order regression fails before implementation, then passes;
another test covers complete snapshots, qualified Movie/Series identities, live updates, cached
read failures and one observation. Spotless, lint (zero errors, 54 warnings), debug and AndroidTest
builds pass. Two added native
tests compile for full-view restoration/navigation/live updates and compact cards/full accessible
dates; the existing date-wording test now checks the actual headings. These native tests have
not run and do not certify the layout or TalkBack behavior on a phone.
The same host gate passes again after the approved section reorder; a source-order check fails
before the move and passes afterward. No Android device is connected at this checkpoint.

## Owner-led manual TalkBack and History acceptance — 2026-10-22

The owner confirms the scoped manual checks on HONOR CMA-LX1 / Android 13 at 360 dp/font 1.0,
with English fixture UI, real TalkBack bound and touch exploration enabled:

- Movie rating/date editors open; changing and cancelling preserves values, understandable
  title/actions and useful focus.
- Series next-episode S1 E2 navigation, episode name/state, season exploration, return without
  accidental progress writes and Favorite state announcements work as expected.
- The current compact alternating History exposes only Movies/Series. Dates/titles read in
  chronological order, cards on both sides are reachable, horizontal two-finger scrolling works,
  Series groups expand/collapse with understandable state, and Details return retains position.
  The owner explicitly approves this interaction.

These reports complete the scoped Details M5 manual accessibility and current History acceptance
checkboxes. They supplement the previous technical tests; no fresh instrumentation or JVM run is
claimed here. Current isolated debug builds pass, update only the two existing synthetic apps and
remove INTERNET. The standard debug artifact is rebuilt afterward without installation. Production
Details/History sources are unchanged. All five original accessibility settings are verified
restored and package inventory is unchanged; the personal app is preserved. Raw evidence remains
ignored under `.audit/accessibility-2026-10-22/`.

This is not global accessibility certification or a full device/language matrix. Integration,
offline/concurrency, native Home verification and final audit/release gates remain open. No
milestone closure, commit, push, signing or publication is implied.

## Details integration, offline and concurrency — 2026-10-22

The scoped M5 integration item is verified on HONOR CMA-LX1 / Android 13, 360 dp/font 1.0:

- Eleven native regressions pass: two real Room metadata/calendar/progress/date concurrency tests
  and nine Activity/navigation tests. A Detail episode write survives actual Activity recreation,
  disappears from Home continuation and the Watching collection, then reappears in both after
  unwatching. Earlier progress timestamps and the unwatched future episode are preserved.
- A separate seed and two cold verification phases run in distinct processes (final PIDs 28170,
  28260, 28415), with explicit force-stop between invocations. Saved Movie date/rating and Series
  episodes remain readable; a Movie rating change to 9 and a Series episode write through the UI
  survive the second cold process. The isolated target has no INTERNET permission. Personal radio
  settings are unchanged; this is app-level offline proof, not real TMDB socket failure/recovery.
- Concurrent Movie refresh/progress/date writes retain stable identity, membership, Favorite,
  rating, timestamp and custom date. Concurrent Series refresh/unwatch/date writes do not resurrect
  removed completion evidence. These cases do not prove every possible scheduler interleaving.

Fixture writes now use `DebugDatabaseEntryPoint` to reach the app's Room singleton. An initial
second-instance fixture passed alone but failed in sequence because shared observations were not
invalidated; the final eleven-test sequence passes after this test-only correction. The hook is
excluded from release; production logic, Room 9, Backup v3, dependencies and version are unchanged.

The final host gate executes all 535 JVM tests without failures/errors/skips. Spotless, lint
(0 errors, 54 warnings) and debug/AndroidTest builds pass; no lint warning points at the new test
or debug hook. Standard APK application IDs are restored. Only the two newly created isolated
packages are removed, package inventory matches baseline, and all eight accessibility/radio
settings are unchanged. The personal app and its data are preserved. Raw probes, rejected fixture
attempts and evidence remain ignored under `.audit/integration-2026-10-22/`.

Native Home control/adaptive verification, the unfiltered final Android suite, release/R8 checks,
global Ponytail audit and milestone closure remain open. No commit, push or publication.

## Native Home, full Android suite and release/R8 — 2026-10-22

Point 4 passes without production or permanent-test changes:

- The complete unfiltered `connectedDebugAndroidTest` executes 415 tests on HONOR CMA-LX1 /
  Android 13: zero failures, errors or skips. No temporary probe is compiled into this suite.
- All 15 HomeScreenTest cases pass separately in EN and IT, including saveable all-events mode,
  live updates/navigation, compact rows/full accessible dates, cached errors, empty/loading,
  refresh actions and continuation behavior.
- Eight additional native composition variants pass: 320 dp/IT/font 1.5, 360 dp/IT/font 1.3,
  411 dp/EN/font 1.0 and 600 dp/EN/font 1.5, each Light/Dark. They exercise calendar/continuation/
  featured order, summary/full switching, title callbacks, long text, cached-error, empty/loading
  states. Sixty-four screenshots are collected; eight representative samples are visually
  inspected across all variants, with no blocked interaction found. These are resized compositions
  on one phone, not a physical tablet, full Cartesian matrix or manual Home TalkBack certification.

The standard host gate passes Spotless, lint (0 errors, 54 warnings), debug/AndroidTest and
`assembleRelease`; the unchanged 535 JVM result is reused as UP-TO-DATE, not claimed rerun.
`minifyReleaseWithR8` actually executes. The frozen unsigned APK is 1.3.0/code 11, application ID
`com.cydoniancitizen.bingee`, 6,421,094 bytes, with SHA-256 recorded in the ignored artifact manifest.
The debug fixture entry point is absent from the release mapping. Signature inspection confirms
the expected unsigned artifact; no signing, release installation or signed/minified smoke is claimed.

Only the newly owned `nativefinal130` and `.test` packages are installed and removed. Final package
inventory, nine accessibility/radio/font settings and display size/density exactly match baseline;
the personal app and data are preserved. Standard debug/test APK IDs are restored without probes,
and the frozen release hash remains unchanged. Raw results, screenshots and the APK stay under
`.audit/final-native-2026-10-22/`, with the host log beside that folder. No commits or pushes.
Global Ponytail audit, final documentation alignment and milestone closure remain separate and open.

## History H1–H6 checkpoint — 2026-10-20

Historical checkpoint: on 2026-10-21 the owner approved replacing the monthly UI with a continuous
horizontal all-history timeline. The correction checkpoint above records its own verification; the
results in this checkpoint do not certify the new interaction or its accessibility.

The owner assigned Your history / La tua storia to v1.3.0/code 11. Your bingee opens a read-only
diary of retained watched Movies and Episodes, with month/type filters, daily Series groups,
Specials and bounded replacement pages. It includes progress outside the collection and abandoned
Series. Unwatch removes an item; repeat viewings and original viewing zones are not reconstructed.
Movie manual dates now determine the same month in History and monthly Statistics. Specials remain
excluded from the existing statistics. No progress timestamp is rewritten.

One transactional Room snapshot prevents mixed parent/child generations. History retains at most
30 parents and 20 children per open group; Profile keeps no extra full diary. Return/recreation
preserve filters and viewport. Restore clears transient local-ID state while retaining month/filter;
the signal brackets the existing transaction and is not serialized. Room 9, Backup v3, legacy
v1/v2 restore, dependencies, cache and backup retention policies stay unchanged.

The host JVM suite passes 528 tests; a full 388-test native run on HONOR CMA-LX1 / Android 13
passes. One subsequent
focused native test additionally checks History after parse/validation/restore of each v1/v2/v3
format, including manual Movie dates and abandoned-Series Specials. The full run predates that
additional test; it is not reported as a 389-test whole-suite execution. Eight EN/IT Light/Dark
adaptive cases pass at 320/360/411/600 dp with specified font scales; 600 dp is simulated on the
phone. Separate fresh-process tests without the isolated app's INTERNET permission pass for local
Movies, groups, Specials and Details return. Native navigation and a retained, unsubscribed History
owner across a real restore are covered separately; no complete SAF file-picker test is claimed.

Synthetic debug measurements use 3 warmups and 20 samples per case. The slowest complete snapshot
at 100k Episodes, 300 Series and 100 manually dated Movies measures median 756.62 ms / p95 763.98 ms
with up to 30 groups open. The provisional 1000 ms parent / 250 ms individual-child read budgets
are met on this phone. These are warm in-memory data-read measurements, not release/frame/cold-disk
guarantees or proof of CPU/GPU/battery/RAM savings. Existing indices suffice for these scenarios.
Read-only Ponytail review findings about saved expansion keys and independent Flow generations
are fixed; the global audit sampled high-risk existing flows without unrelated refactors.

Debug/test/release builds, R8 and standard lint pass; lint reports zero errors and 52 warnings.
An intermediate lint run crashed while release Hilt sources were regenerated; running lint after
the build passes without suppressions. An earlier native accessibility failure came from the phone
Control Center covering the app; dismissing it makes the unchanged test and subsequent full run
pass. Existing Details/Home work is preserved. Detailed reports, fixtures, tools and captures stay
ignored under `.audit/history-timeline/`.

H1–H6 are delivered and verified with these limits. The owner reports a positive manual TalkBack
check for filters, month/year, group expansion, information and Details return: the controls tried
are clear and reachable. This is owner-reported evidence on that fixture; pagination/retry and
other state combinations retain their automated coverage, without an exhaustive manual claim.
The ready offline test installation is separate from the personal app. The existing Details M5,
Home/calendar and encompassing v1.3.0 acceptance gates remain independent. This checkpoint does not
close v1.3.0 or authorize a commit, push or release publication.

## M5 accessibility checkpoint — 2026-10-21

On HONOR CMA-LX1 / Android 13 at 360 dp/font 1.0, eight focused runs pass with the real
TalkBack service bound and touch exploration enabled: the three new native accessibility tests
and the existing M3 episode-row test, each in EN and IT. All 49 ordinary IT Details/preview
regressions also pass after TalkBack is restored to its initial state.

Coverage includes Compose button roles plus native reachable actions, favorite checked state,
distinct collection/resume/season controls, rating range/adjustment, editor cancellation without
writes and reachable collapsed controls. M3 rechecks the unobscured episode checkbox, retained
focus during metadata updates and one activation callback. Ordinary tests physically tap both
edges of the slider's 48 dp interaction region; its smaller semantic thumb bounds alone do not
demonstrate a deficient touch target. No production fix is needed or retained.

An injected swipe did not advance TalkBack focus in either locale; its cause remains unconfirmed.
The final native tests consequently make no speech, gesture or reading-order claim. The owner
chose to try the manual pass later, so the M5 accessibility checkbox remains open. The isolated
«bingee M5 test» app stays installed with three synthetic local Movie/Watching/Abandoned titles;
the test runner is removed. All five saved accessibility settings match their original values,
including TalkBack disabled. The personal app, its data and previous test installations are preserved.

Spotless, 528 JVM tests, debug/AndroidTest builds and lint pass (zero errors, 52 warnings outside
the new test). The JVM task executes once on the restored production sources, then is reused as
UP-TO-DATE by the final unchanged-source gate. Scoped Ponytail review finds no production change
or added dependency; fixtures, failed calibration attempts, tools and evidence remain ignored
under `.audit/v1.3.0/m5-accessibility/`.

Manual speech/reading-order/gesture validation is still required. Fresh-process offline,
concurrency/integration, Home/calendar and final global/release gates remain separate open work.
This checkpoint does not close M5, v1.3.0 or global v1.3.4 accessibility hardening. No commit,
push or publication occurred.


## Final audit and agreed-scope closure — 2026-10-22

The final global Ponytail audit and Impeccable native synthesis find no new demonstrated blocker
in the reviewed changed/shared paths. No further production or test change is required in this
step. Current statuses, owner acceptance/TalkBack evidence, Home section ordering and the
version-specific checklist are aligned; earlier dated pending gates remain historical records.
The [canonical closure record](../BINGEE_DEVELOPMENT_ROADMAP_AND_CHECKLIST.md#v130-closure-record--2026-10-22)
classifies fixed work, pre-existing/deferred limits and justified complexity, and records B1–B7.

Verification relies on the unchanged source snapshot: 535 JVM tests actually passed at the
integration checkpoint (UP-TO-DATE in the later host gate), 415 unfiltered Android tests passed,
15 Home component tests per EN/IT locale and eight native Home configurations passed. R8 actually
ran for the frozen unsigned 1.3.0/code 11 artifact. Final documentation checks validate repository-local
Markdown paths/headings, whitespace, source preservation and an empty Git index. No extra Android
or performance run is attributed to this documentation-only step. Evidence remains ignored in
`.audit/closure-v130/` and the earlier verification directories.

The agreed v1.3.0 scope is closed. Room 9, Backup v3 and restore v1/v2/v3 are unchanged. Existing
performance, retry, notification crash-interval and migration/index deferrals remain. Manual Home
TalkBack, full Cartesian UI coverage, real provider failure/recovery and signed
release runtime smoke are not certified. Global v1.3.4 hardening is not complete. This final step
does not operate on the phone and preserves all prior app/test work. No commit, push, signing, tag
or publication is performed.

Owner clarification: manual TalkBack for Details and the current History timeline was already
completed and accepted; the Home-only manual check was not performed. Tablet verification is
explicitly excluded from the agreed scope and is not outstanding work or a closure requirement.
