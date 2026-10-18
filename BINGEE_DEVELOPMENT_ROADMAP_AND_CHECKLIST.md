# Bingee — Development Roadmap & Agent Execution Checklist

> **Purpose**
> This file is the canonical implementation roadmap for the next Bingee development cycle. It has two sections:
>
> 1. **PART I — Complete roadmap:** every planned change, the target app version, its scope, constraints, and acceptance criteria.
> 2. **PART II — Agent execution checklist:** the checklist the implementation agent must follow and tick for every change.
>
> This file is **canonical project documentation** and must be version-controlled. Generated audit reports are temporary artifacts and belong under `.audit/` only; they must not be committed by default.

---

# Baseline

Historical baseline before this roadmap started:

- App version: **v1.2.2**
- versionCode: **6**
- Room database: **9**
- Backup format: **3**
- Architecture: single-module Android app; Kotlin + Compose + Hilt + Room + Retrofit + WorkManager + Glance
- Product identity: **private personal Movie/TV Series tracker**, local-first where possible
- Not a social network, feed, public-profile product, or recommendation engine
- Current Details direction: **cinematic shared media hero + local-first personal tracker**
- Current repository health: **HEALTHY WITH TARGETED DEBT**

> Baseline correction (2026-10-04): pre-flight found Room 9 / Backup 3 already present at HEAD `837647f44a83ad9f9eab229877e71c925fb86de8`. The owner authorized continuing after the discrepancy was reported. Modification #1 preserves these existing persistence versions; no schema, migration or backup-format change is made.

## Current milestone status — 2026-10-18

The agreed **v1.2.5 / versionCode 9 scope is complete**: final checks, the full Android suite and
documentation alignment are closed. Room remains 9; export remains Backup v3 with v1/v2/v3 restore.
No release publication, signing, tag or commit is implied by this status.

The owner explicitly deferred the next global audit, potential over-engineering/dead-code fixes and
future optimization study to **v1.2.6**. This replaces the original final-audit requirement for
v1.2.5; that audit was not executed during the v1.2.5 closure. Completed optimizations remain closed.
The current Film/Series animation and residual frames above 16.7 ms are accepted for this scope. Unexecuted
performance matrices are declared limits, not additional v1.2.5 gates. New optimization directions
require an owner decision on benefits and costs under AGENTS.md section 22.

The [v1.2.5 checklist](#v125-checklist) and [performance closure record](docs/checklist-1.2.5-performance.md#stato-di-chiusura--18-ottobre-2026)
define the final scope. Earlier dated implementation records below retain their historical
"in progress" and "gate open" statements; those are superseded by this closure record.

The subsequent v1.2.6 read-only audit and isolated image-cache study have been performed. The owner
accepted keeping the existing cache configuration and closed that investigation on 2026-10-18.
Its bounded result and deferrals are recorded in the [v1.2.6 scope](#v126--global-audit-and-targeted-maintenance);
the whole v1.2.6 milestone has not been declared complete.

## Global development rule from this point forward

**Every numbered modification in this roadmap must be followed by a GLOBAL read-only audit of the entire project before that modification can be marked complete.**

The explicit v1.2.5 closure exception above takes precedence: do not run a new global audit or
reopen completed optimization work for that milestone. The deferred audit belongs to v1.2.6.

The audit exists to prevent progressive accumulation of:

- dead code;
- obsolete compatibility paths;
- duplicated business rules;
- speculative abstractions;
- unused dependencies;
- accidental architecture drift;
- UI/UX inconsistencies;
- accessibility regressions;
- responsive regressions;
- test bloat or stale tests;
- documentation drift.

The global audit must use, when available:

- **Impeccable** for UI/UX, accessibility, responsive behavior, hierarchy, Material 3 consistency and product coherence;
- **Ponytail Audit** for dead code, over-engineering, redundant abstractions, duplicate logic and unnecessary complexity;
- relevant **official Android Agent Skills** for Compose/adaptive UI, edge-to-edge, R8, testing, security and other Android-specific checks.

The audit is **read-only first**. It must distinguish:

- newly introduced debt caused by the current modification;
- pre-existing debt unrelated to the modification;
- justified complexity that should be left alone.

A modification is not complete until newly introduced P0/P1 issues and newly introduced proven dead code/over-engineering have been resolved or explicitly blocked with owner approval.

Pre-existing low-priority debt must not automatically expand the current scope.

---

# PART I — COMPLETE ROADMAP

# Version map

| Version | versionCode | Theme | Main objective |
|---|---:|---|---|
| **v1.2.3** | **7** | Small correctness / clarity fixes | Close low-risk UI/UX and semantics defects already proven by audits |
| **v1.2.4** | **8** | Technical / test / release polish | Remove small current technical risks without changing product architecture |
| **v1.2.5** | **9** | Dead-code cleanup + targeted performance improvements + documentation sync | Lean, resource-efficient baseline before the large UI cycle |
| **v1.2.6** | **10 (planned)** | Audit and targeted maintenance | Audit current code, address proven dead code/over-engineering and study future optimization tradeoffs |
| **v1.3.0** | **11 (planned)** | Major UI/UX #1 | Rebuild Details around a clear personal-tracker hierarchy |
| **v1.3.1** | **12 (planned)** | Major UI/UX #2 | Reorganize Home around current personal activity |
| **v1.3.2** | **13 (planned)** | Major UI/UX #3 | Make Collection a coherent personal-management surface |
| **v1.3.3** | **14 (planned)** | Major UI/UX #4 | Unify Bingee's cross-screen UI language and interaction hierarchy |
| **v1.3.4** | **15 (planned)** | Major UI/UX #5 | Comprehensive responsive + accessibility hardening |

> Version numbers may only move forward. Room and Backup versions must **not** be bumped merely because the app version changes. Schema/format bumps require an actual persistence contract change.

The inserted v1.2.6 reserves the next code and shifts later planned codes only; the application
configuration remains v1.2.5/code 9. No later milestone is started by this documentation change.

---

# v1.2.3 — Small correctness, clarity and semantics fixes

## Goal

Close the small, evidence-backed UI/UX defects that do not require a major screen redesign.

This release must remain intentionally narrow. It must not start any of the five major v1.3.x redesigns.

## 1. Fix watched/completion-date action layout under narrow width and large fonts

### Current problem

Runtime audit confirmed that the date action row can lose the visible **“Cancella data”** label at approximately 320dp / Italian / font scale 1.5, and becomes extremely narrow at approximately 360dp / Italian / 1.3.

### Required change

- Make the date action layout resilient to narrow width and large font scale.
- The clear/remove action must remain visibly identifiable.
- The Save/Edit/Cancel semantics must remain unambiguous.
- Verify both Movie watched-date and Series completion-date variants.
- Preserve existing persistence behavior.
- Do not introduce a new date component architecture unless necessary.

### Acceptance criteria

- 320dp + Italian + font 1.5: clear-date action remains visible and understandable.
- 360dp + Italian + font 1.3: no character-by-character collapse.
- 411dp normal layout remains visually balanced.
- English remains correct.
- Touch targets remain at least standard Material size.

---

## 2. Standardize Favorite toggle visual state where it is currently inconsistent

### Current problem

The Collection grid Favorite toggle can use Material primary/gold for the checked state while Details and Collection list use red/error semantics.

### Required change

For **actual Favorite toggles**:

- unselected heart: neutral/appropriate on-surface state;
- selected heart: consistent Favorite state treatment, expected to be red/error-family unless the owner explicitly changes the product convention;
- preserve artwork-specific contrast where needed;
- preserve shortcut/filter differences where they are not actual toggles.

Do not force the dashboard Favorites shortcut, dedicated Favorites filter, or static decorative heart to behave like a toggle.

### Acceptance criteria

- Details, Collection grid and Collection list communicate the same Favorite meaning.
- Checked semantics remain correct.
- Light/Dark contrast remains acceptable.

---

## 3. Stop equating generic Library membership with Watch Later in user-facing copy

### Current problem

Search and some Home/provider acquisition surfaces can present generic saved membership as **Watch Later**, even when a Series is Watching, Watched or Abandoned.

### Product rule

These concepts are different:

- **Library / Collection membership** = title belongs to the user's personal collection.
- **Watch Later** = canonical derived state for an unwatched/unstarted title.
- **Favorite** = independent personal marker.
- **Watching** = active Series progress.
- **Watched** = completed according to current canonical rules.
- **Abandoned** = explicit override.

### Required change

- Replace membership-only wording such as “In Watch Later” with membership-accurate wording such as **“In collection / In raccolta”** where the underlying state is only membership.
- Preserve genuine Watch Later wording only when the actual canonical state is Watch Later.
- Do not change persistence or state derivation.

### Acceptance criteria

- A Watching/Watched/Abandoned Series is never mislabeled as Watch Later solely because it is in Library.
- Removing from collection is clearly described as membership removal, not state-specific Watch Later removal.

---

## 4. Remove redundant Details artwork semantics

### Current problem

Backdrop, poster and visible title can each expose title-bearing accessibility semantics, potentially producing repeated title announcements.

### Required change

- Keep the visible title as the primary identity announcement.
- Mark purely decorative artwork as decorative where appropriate.
- Preserve meaningful image fallback semantics if an image itself conveys state that is not otherwise exposed.
- Do not merge unrelated interactive controls.

### Acceptance criteria

- Details no longer exposes three redundant title identity nodes.
- Back/Favorite/Refresh remain separately actionable and labeled.
- No title identity is lost.

---

## 5. Fix Notification poster clipping-layer inconsistency

### Current problem

Notification thumbnails can receive an outer 8dp clip while the shared poster component also applies its own 16dp rounding.

### Required change

- Keep the compact Notification poster size.
- Ensure there is one intentional effective clipping radius.
- Prefer reusing the shared poster behavior unless compact notifications genuinely require a distinct shape.
- Do not create a Notification-specific image component unless needed.

---

## 6. Validate the direct Collection shortcut → Statistics navigation risk

### Current concern

A launcher shortcut can enter Collection directly while Statistics may assume a Your Bingee ancestor exists in the back stack.

### Required operation

- Reproduce the route from a clean app/task state.
- Attempt Collection direct shortcut → Statistics.
- If it works correctly, document **NO DEFECT** and do not change code.
- If it fails, fix navigation ownership without creating synthetic back-stack machinery unless required.

### Acceptance criteria if a fix is required

- Statistics can be opened from all supported Collection entry paths.
- Back/Up behavior remains predictable.
- Normal Your Bingee → Collection → Statistics navigation is unchanged.

---

## 7. v1.2.3 documentation update

Update canonical docs only for user-visible semantics actually changed in this release.

Do not perform the broad documentation sync reserved for v1.2.5.

---

# v1.2.4 — Technical, test and release polish

## Goal

Resolve remaining small technical risks before beginning the large v1.3.x UI series.

No major UI redesign.

## 1. Reduce instrumentation tests' dependency on English display strings

### Current problem

Some Compose/instrumentation tests identify controls through literal English copy. This creates unnecessary fragility when copy changes and limits locale-independent testing.

### Required change

- Audit instrumented UI tests for literal-English selectors.
- Prefer stable semantics, roles, state descriptions, content descriptions, or resource-backed expectations where appropriate.
- Do not add test tags everywhere merely to make tests easy.
- Keep tests user-behavior oriented.
- Preserve tests where visible text itself is the behavior being tested.

### Acceptance criteria

- Key cross-screen tests do not fail simply because ordinary copy changes.
- At least representative Italian execution remains possible where relevant.

---

## 2. Release-runtime smoke coverage

### Required runtime smoke

Validate an R8/minified release artifact for:

- cold launch;
- TMDB credential validation;
- Search;
- Movie Details;
- Series Details;
- add/remove Library;
- Favorite;
- progress persistence;
- Your Bingee;
- Statistics;
- Settings;
- Notifications/deep link;
- launcher shortcuts;
- Glance/widget path if configured;
- relaunch persistence.

### Required checks

- no `FATAL EXCEPTION`;
- no `NoClassDefFoundError`;
- no `ClassNotFoundException`;
- no `VerifyError`;
- no release-only Gson/R8 regressions.

Do not add keep rules unless a demonstrated release failure requires them.

---

## 3. Review Home observation error handling

### Current concern

Some Home observation paths may silently swallow errors while other Home paths surface recoverable failures.

### Required operation

- Map current behavior.
- Decide whether the silent path is intentionally non-user-actionable.
- If intentional, leave it and document why.
- If accidental, align it with existing error/result conventions using the smallest change.

Do not create a global error bus.

---

## 4. Review shared Flow retry policy

### Current concern

A shared library-progress flow has a documented unconditional resubscribe delay after observation failure.

### Required operation

- Confirm the retry remains bounded and cancellation-safe.
- Confirm it does not spin while no subscriber exists.
- Confirm production consumers do not become permanently stale.
- Change only if there is a concrete behavior issue.

Do not replace a working simple retry with a complex backoff framework without evidence.

---

## 5. Statistics radar sizing hardening

### Current state

The radar uses actual canvas constraints for much of its geometry but still uses window width for part of its height cap.

### Required change

- Make radar sizing derive from its actual available container rather than the entire window where practical.
- Preserve current visual design and data semantics.
- Verify current phone layout remains unchanged or equivalent.
- Verify 600dp/adaptive layouts do not depend on full-window assumptions.

This is a layout correctness refinement, not a chart redesign.

---

## 6. v1.2.4 documentation update

Document only technical behavior materially changed by the above work.

---

## 7. Global hardware-efficiency audit

### Goal

Audit the entire current project for efficient use of device resources before closing v1.2.4.

### Required operation

- Review CPU and main-thread work, Compose recomposition, list composition and rendering, including expensive chart and image operations.
- Review memory usage, allocations, image sizing/cache ownership, retained objects and lifecycle cleanup.
- Review Room query cost, indexes, shared observations, database writes and disk I/O.
- Review network calls, refresh batching, cancellation, retries and cache freshness.
- Review battery impact, WorkManager scheduling, notifications and Glance/widget updates, including work performed without active consumers.
- Use reproducible measurements or bounded profiling for suspected bottlenecks where feasible. Separate source-level risks from measured runtime problems.
- Rank findings by severity, user impact and evidence. Record device/build configuration, reproduction steps and measurement limits.
- Apply only small, evidence-backed fixes compatible with v1.2.4 scope; defer structural changes or speculative optimizations to an explicitly planned milestone.

### Acceptance criteria

- The global audit covers CPU/rendering, memory, database/disk, network and battery/background work.
- Measured issues and unmeasured hypotheses are clearly distinguished; no performance improvement is claimed without comparable before/after evidence.
- Any applied fix preserves local-first behavior, user data, cancellation and existing functionality, with proportionate regression verification.
- Temporary reports and profiling evidence remain under `.audit/`; only deliberately adopted policies enter canonical documentation.

This audit complements the mandatory per-modification and final global audits; it does not replace them.

---

# v1.2.5 — Dead-code cleanup, targeted performance improvements and canonical documentation sync

## Goal

Enter the major UI cycle with a deliberately lean, documented baseline.

**Final status:** the agreed scope is closed. The original plan below explains the work selection;
the owner-approved closure record and [checklist](#v125-checklist) govern remaining/deferred work.

This version combines the dedicated cleanup with the targeted performance follow-up selected from
the v1.2.4 hardware-efficiency audit. Preserve existing product behavior and use measurements to
decide conditional or structural optimizations.

## 1. Run a fresh GLOBAL dead-code / over-engineering audit

Do not rely only on previous audit results. Re-scan the current v1.2.4 codebase.

Audit:

- unused production symbols;
- fake-only interface members;
- obsolete repository methods;
- dead DAO queries;
- wrappers with no current invariant;
- one-use abstractions that no longer justify themselves;
- stale compatibility paths;
- unused resources;
- stale strings/plurals;
- unreachable branches;
- obsolete test fixtures;
- duplicated rules that became redundant after v1.2.3/v1.2.4;
- unnecessary direct dependencies;
- stale documentation references.

Use call-site evidence before declaring anything dead.

## 2. Re-evaluate previously identified candidates

Re-check, do not blindly delete:

- `observeEntryCount` chain;
- repository-level `isInLibrary` if still unused;
- fake-only/default `observeContinueWatching` path;
- `SeasonSummaryStore` seam;
- unused portions of `LibraryOrganization`;
- stale helper aliases;
- dead UI wrappers;
- obsolete test-only seams.

If a candidate became useful, keep it.

## 3. Cleanup rules

- Delete only proven dead code.
- Prefer deletion over replacing dead code with a new abstraction.
- Do not split large files merely because they are large.
- Do not rewrite working architecture.
- Do not introduce future-history/year-recap support.

## 4. Full canonical documentation sync

Bring canonical docs to the actual v1.2.5 codebase:

- README;
- PRODUCT;
- AGENTS;
- architecture;
- roadmap;
- privacy;
- backup docs;
- schema docs;
- import docs;
- ADR index/references;
- release notes where maintained.

Historical ADR content must remain historical; do not rewrite old decisions to match current implementation.

## 5. Broken-link and stale-reference scan

- Scan all tracked Markdown.
- Validate relative links.
- Remove links to deleted generated audit reports.
- Confirm `.audit/` remains ignored.
- Confirm canonical docs remain tracked.

## 6. Follow up on the hardware-efficiency audit

Use the [v1.2.5 performance checklist](docs/checklist-1.2.5-performance.md) for implementation order,
regression coverage and acceptance criteria:

- avoid widget observations without installed instances and irrelevant visual updates;
- tie expensive UI observations/calculations to active consumers;
- reduce repeated Room aggregates and Continue Watching projections;
- virtualize individual episodes and release events within the existing single scrolling lists;
- move Collection filtering/sorting off Main;
- remove quadratic episode-map reconciliation scans;
- simplify monthly aggregation and make expensive CPU work cooperatively cancellable;
- measure backup memory before selecting any pipeline change.

These are source-supported opportunities, not established Android runtime regressions. Preserve
data integrity, portability, local-first behavior and existing v1.2.4 recovery/release fixes.
Keep raw audit/profiling artifacts outside canonical documentation. Manual-refresh batching and
app-owned Baseline Profiles remain conditional on evidence, not automatic scope additions.

## 7. Establish v1.3.0 clean baseline

Original criteria, reconciled with the owner's closure decision:

- no additional product blockers emerged from the agreed final checks;
- C01–C07 cleanup and its previously recorded scoped reviews are retained;
- final verification is green within the recorded device/scenario limits;
- the new global audit is explicitly deferred to v1.2.6, not marked executed;
- documentation is current; v1.2.6 maintenance precedes v1.3.0.

---

# v1.2.6 — Global audit and targeted maintenance

## Goal

Audit the current baseline after v1.2.5 without automatically reopening completed optimizations.
The owner started the v1.2.6 investigation on 2026-10-18. The approved scope is image-cache
measurement and isolated experiments, plus database-index planning. The owner has now accepted
retaining the current image-cache configuration and closed that investigation. The application
version remains 1.2.5/code 9; the whole milestone still requires its final closure verification.

- Run the deferred read-only global audit with Ponytail, distinguishing defects, proven dead code,
  over-engineering, justified complexity and verification limits.
- Apply justified over-engineering/dead-code fixes with call-site evidence, small changes and
  proportionate regression coverage; preserve data, compatibility and existing behavior.
- Study optimization opportunities for v1.2.6 and later releases. For each direction, specify
  measurable benefits, CPU/GPU/RAM and disk costs, latency/offline effects and a stop condition.
  Prefer the agreed local-storage direction where worthwhile. Study does not authorize every fix
  or optimization; the owner chooses new directions before implementation.
- Keep accepted v1.2.5 limits visible. Reopen a closed optimization only for a demonstrated
  regression or an explicit new request; do not turn every unexecuted matrix into a release blocker.
- Update canonical docs and verify only the changes actually selected. No schema or backup bump
  follows from the app-version change alone.

### Closed image-cache investigation and deferred index review

- The completed study inspected Coil 3.4.0 and measured storage categories, repeated navigation,
  restart, offline image availability, network activity and loading times on an isolated Android
  installation, preserving the personal app. On the tested Android 17/API 37 emulator and fixed
  dataset of 16 titles, no utility was demonstrated for reducing the image-cache limit. The owner
  confirmed retaining the current production configuration; no lower-limit experiment or change
  is needed to close this investigation.
- This result applies only to that device and dataset, not to large libraries or prolonged use.
  Further measurements are deferred until a concrete problem emerges and are not a closure gate.
  All numerical thresholds proposed in the temporary report remain hypotheses to reassess, not
  approved requirements or acceptance criteria. Reports, tools, evidence and the repeatable protocol
  stay under ignored `.audit/`; no CPU/GPU/RAM/battery benefit is claimed.
- Revisit `index_seasons_local_media_id`, `index_episodes_local_season_id` and
  `index_media_genres_source_genre_id` only with the next Room migration required for other reasons.
  Any removal requires confirmation of the actual Android query plans, non-destructive migration
  tests, data-integrity checks and paired Android query-latency measurements. No Room version bump,
  index removal or `VACUUM` is authorized now. The audit's host-SQLite synthetic estimates are not
  Android measurements and do not establish an immediate reduction of an existing database file.
  These candidates are deferred work, not outstanding v1.2.6 implementation or closure requirements.
- Backup-share retention, image-URL compaction, removal of `temp_ui.xml`, general refactors and
  dependency changes are outside this scope. Any future production image-cache limit would require
  new evidence and an explicit owner choice before implementation (AGENTS.md section 22).

---

# v1.3.0 — Major UI/UX #1: Details as a true personal-tracker screen

## Goal

Preserve the cinematic Details identity while making the user's personal relationship with a title immediately understandable.

## Product direction

**Hero → Personal summary → Synopsis → Detailed tracking/editing → Secondary metadata**

Do not revert the current hero.

## 1. Introduce a compact personal summary immediately below the hero

The summary must communicate state without exposing every editor by default.

### Movie summary information

At minimum evaluate and implement an appropriate presentation for:

- Library membership;
- Favorite state where useful without duplicating the toolbar toggle;
- watched/unwatched state;
- effective watched date when watched;
- current rating/unrated state;
- next meaningful action.

Do not duplicate the entire lower editing UI.

### Series summary information

At minimum:

- explicit personal relationship state;
- watched / currently trackable episode progress;
- season progress where useful;
- caught-up state where semantically applicable;
- next actionable episode when available;
- metadata-coverage limitations when they prevent a trustworthy completion judgment.

## 2. Make Series relationship state explicit

User-facing state vocabulary must clearly distinguish:

- Watch Later / Da vedere;
- Watching / In visione;
- Abandoned / Abbandonata;
- complete finished Series / Vista;
- caught up with an ongoing Series / In pari.

Important:

- Do not persist a new state merely for UI wording if it can be safely derived.
- Do not change canonical tracking semantics without an explicit separate product decision.
- “In pari” must not imply that an ongoing Series is permanently finished.

## 3. Add concise next-actionable-episode context

When a Series has an episode the user can meaningfully continue with, Details should identify it without forcing manual season searching.

Potential information:

- season/episode number;
- episode title;
- availability date if useful;
- direct scroll/open/action affordance if appropriate.

The source of truth must match Continue Watching policy. No second continuation algorithm.

## 4. Reduce rating default density

Move from an always-expanded editing experience toward:

- readable current rating/unrated state;
- editing on explicit user intent;
- preserve integer 1–10 semantics;
- preserve Save/Remove behavior and chronology;
- preserve accessibility.

Do not create a custom rating system incompatible with current data.

## 5. Make watched/completion date presentation semantically appropriate

### Movie

- Date editing should clearly belong to watched state.
- If date selection can mark an unwatched Movie as watched, the action must communicate that consequence.
- Existing effective date and custom date behavior must not become contradictory.

### Series

- Completion-date editing must only be presented when a genuine completion record/state makes it meaningful.
- Do not show a control that appears to save when persistence will ignore it.

## 6. Move Abandoned relationship control to title-level ownership

- Abandoned/Resume tracking must no longer require scrolling beyond arbitrarily long season content.
- Keep it visually secondary and reversible.
- Preserve episode history.
- Preserve current canonical override semantics.

## 7. Improve season/episode scan rhythm

Without redesigning the tracking model:

- reduce unnecessary visual noise;
- preserve collapsed-season scanning;
- preserve inline expansion;
- preserve Specials separation;
- preserve stale/loading/error states;
- improve long episode title allocation;
- keep future episodes visible but correctly disabled;
- make bulk season actions clearly scoped.

## 8. Clarify refresh scope

Title-level refresh and per-season episode refresh must not look like the same operation if they update different metadata scopes.

## 9. Preserve hero and media identity

Protect:

- backdrop;
- poster overlap;
- collapsing toolbar;
- contextual overlay controls;
- type/year/runtime-or-season-count orientation;
- cache-first content behavior.

## 10. Details verification matrix

At minimum:

- Movie / Series;
- new title / Library title;
- Watch Later / Watching / In pari / complete / Abandoned;
- rated / unrated;
- Favorite / not Favorite;
- Light / Dark;
- EN / IT;
- 320 / 360 / 411dp;
- font 1.0 / 1.3 / 1.5;
- long title;
- missing artwork;
- stale cached data;
- partial Series metadata;
- expanded season;
- future episode.

---

# v1.3.1 — Major UI/UX #2: Home centered on current personal activity

## Goal

Make Home answer: **“What is relevant for me right now?”** before provider browsing.

## 1. Re-evaluate section order

Target conceptual hierarchy:

1. Continue Watching / immediate personal continuation
2. Release calendar / current personal release activity
3. provider Featured Movies
4. provider Featured Series

Exact ordering may be adjusted only if runtime evidence gives a better personal-tracker hierarchy.

## 2. Protect Continue Watching semantics

- actionable Series only;
- no Movies;
- no Specials;
- no completed/caught-up Series without an actionable episode;
- no Abandoned Series;
- same canonical continuation policy used elsewhere.

## 3. Improve release-calendar role

- clearly distinguish release information from continuation actions;
- resolve Abandoned-series inclusion policy deliberately;
- align with Notification/release-center policy where appropriate without forcing identical scopes.

## 4. Keep Featured as secondary exploration

- preserve Featured if useful;
- make it visually and hierarchically secondary to personal activity;
- do not turn Featured into personalized recommendations;
- do not add ranking/recommendation algorithms.

## 5. Improve Home empty states

Empty personal sections should explain how the user can create useful Home content without pretending Featured is personal activity.

## 6. Preserve local-first behavior

- Room-backed content first;
- network refresh asynchronous;
- cached personal content retained during refresh failure.

## 7. Home verification matrix

Test:

- empty account;
- active Watching Series;
- upcoming release;
- stale cache;
- no credentials;
- Light/Dark;
- EN/IT;
- 320/360/411dp;
- font 1.0/1.3/1.5.

---

# v1.3.2 — Major UI/UX #3: Collection as a coherent management surface

## Goal

Make Collection the clearest place to browse, filter and manage personal titles without requiring the user to remember hidden route/state assumptions.

## 1. Make active Collection scope explicit

Clearly communicate when the user is viewing:

- Watch Later;
- Watching;
- Watched;
- Favorites;
- Abandoned.

Do not rely only on navigation history to explain the active scope.

## 2. Remove impossible filter combinations

Examples:

- Watching and Abandoned are Series-only; Movie filter choices must not create inherently empty nonsensical combinations.

Use the smallest context-aware control adaptation.

## 3. Clarify local search scope

The user must understand that Collection search filters the currently active personal scope, not global TMDB.

Evaluate whether current fragmented category search remains acceptable or whether a broader personal-title search is warranted. Do not add a global “All” mode unless the product owner explicitly approves it after evaluating complexity.

## 4. Align Grid and List management capabilities

Audit and intentionally decide where actions belong:

- Favorite;
- watched date;
- Library removal;
- Details navigation;
- rating display;
- progress display.

Grid and List do not need identical visual controls, but switching presentation must not unpredictably remove essential management capability.

## 5. Make sort semantics truthful

Resolve labels such as **Recently added** where the actual sort key differs by scope:

- library addition;
- Favorite addition;
- viewing activity.

Either make labels contextual or use terminology that matches the underlying chronology.

## 6. Improve filter discoverability

- retain horizontal scrolling only if later filters remain discoverable;
- avoid hidden state;
- preserve compact-width usability;
- do not create multi-row control overload without evidence.

## 7. Preserve Collection's role

Do not move deep episode tracking, detailed rating editing or abandonment management into Collection merely for convenience. Details remains the one-title relationship owner.

## 8. Collection verification matrix

Cover all scopes, grid/list, Movies/Series where valid, empty/non-empty, long titles, EN/IT, Light/Dark, 320/360/411dp, font 1.0/1.3/1.5.

---

# v1.3.3 — Major UI/UX #4: Cross-screen Bingee UI language unification

## Goal

Make Bingee feel like one product without flattening justified screen-specific behavior.

This release is about **shared visual/interaction rules**, not making all screens identical.

## 1. Define top-bar families

Create a small explicit set of justified chrome families, for example:

- top-level content screen;
- personal dashboard/collapsing screen;
- immersive artwork Details;
- secondary destination/settings screen.

Then align screens to those families.

Preserve justified exceptions.

## 2. Standardize Favorite meaning

- actual toggle state meaning consistent everywhere;
- selected semantics consistent;
- visual selected state consistent within artwork/non-artwork constraints;
- shortcut/filter/static heart contexts remain distinct where semantically different.

## 3. Standardize action hierarchy

Define and apply rules for:

- primary acquisition/action;
- secondary/reversible action;
- low-priority text action;
- destructive irreversible action;
- reversible relationship state;
- loading/pending action.

Do not color every reversible action red.

## 4. Standardize error / empty / loading expectations

Align:

- blocking error hierarchy;
- recoverable inline error;
- stale cached-content warning;
- empty-state title/body/action;
- loading announcement;
- retry/configuration action patterns.

Keep compact dashboard inline states where full-page states would be excessive.

## 5. Align section hierarchy

Review:

- section title roles;
- helper-copy roles;
- content spacing;
- dividers;
- cards versus bare shelves.

Do not force Statistics, Settings and media shelves into one identical layout.

## 6. Resolve release-center naming and scope clarity

Review whether the current user-facing **Notifications** destination name accurately describes a followed-Series release center containing past/current/future release events rather than a read/unread inbox.

If renamed:

- update navigation copy;
- empty state;
- settings references where needed;
- accessibility labels;
- docs.

Do not change system notification delivery semantics merely because the screen name changes.

## 7. Align Settings chrome

Resolve accidental drift between Settings index and subpages while preserving Settings' functional density.

## 8. Product-language consistency

Across the app, audit and align:

- Collection/Library;
- Watch Later;
- Watching;
- Watched;
- In pari / caught up;
- Abandoned;
- Favorite;
- release/update terminology;
- viewed/completed statistics wording.

Do not change domain persistence names merely to match UI copy.

---

# v1.3.4 — Major UI/UX #5: Comprehensive responsive + accessibility hardening

## Goal

After the previous UI changes settle, validate the whole application under real constraints and fix remaining responsive/accessibility debt globally.

This is a dedicated hardening release, not a redesign release.

## 1. Full responsive matrix

At minimum:

### Widths

- ~320dp
- ~360dp
- ~411dp
- >=600dp

### Font scale

- 1.0
- 1.3
- 1.5

### Locale

- English
- Italian

### Theme

- Light
- Dark

Prioritize adversarial combinations:

- 320dp + Italian + 1.5;
- 360dp + Italian + 1.3;
- 411dp normal;
- >=600dp with adaptive navigation/layout.

## 2. Screen coverage

Cover:

- Home;
- Search;
- Your Bingee;
- Collection;
- Movie Details;
- Series Details;
- Statistics;
- release/Notifications center;
- Settings;
- key dialogs;
- key loading/empty/error states.

## 3. Fix fixed-width text starvation

Review:

- hero poster + text;
- episode still + text/actions;
- Collection list poster + metadata/actions;
- dashboard shortcuts;
- date/action rows;
- filter rows;
- chart labels.

Avoid brittle per-device magic numbers.

## 4. Wide-screen behavior

- prevent absurd full-width text lines;
- ensure rail layouts use actual remaining content width;
- review chart/container sizing;
- introduce max-width/content constraints where warranted;
- do not build a tablet two-pane architecture unless the product benefits justify it.

## 5. Accessibility semantics

Audit globally:

- headings;
- roles;
- selected/checked state;
- disabled state;
- touch targets;
- decorative imagery;
- duplicate identity announcements;
- progress descriptions;
- chart descriptions;
- filter states;
- season/episode controls;
- Favorite;
- navigation icons;
- dialogs;
- live regions;
- error recovery.

## 6. TalkBack/manual pass

Where practical, perform a bounded manual TalkBack pass of the highest-value flows:

- Home → Details;
- Search → Details;
- Series episode tracking;
- Collection filtering;
- Statistics interaction;
- Settings/backup destructive confirmation.

Do not claim spoken-order correctness without actually testing it.

## 7. Contrast

Validate:

- Light/Dark system bars;
- text over artwork;
- selected/unselected controls;
- disabled controls;
- chart marks;
- progress indicators;
- error/destructive controls.

## 8. Final UI system audit

Run a final Impeccable whole-app audit and confirm:

- remaining differences are intentional;
- no major screen still uses an obsolete visual dialect;
- no P0/P1 accessibility or responsive issue remains;
- large UI work did not accumulate redundant components or dead styling paths.

---

# PART II — AGENT EXECUTION CHECKLIST

# A. Mandatory checklist for EVERY numbered modification

> **Rule:** A numbered modification from Part I cannot be marked complete until every applicable item below is checked.

## A1. Pre-flight

- [ ] Read this roadmap section completely before changing code.
- [ ] Inspect `git status --short`.
- [ ] Record branch and HEAD.
- [ ] Confirm current `versionName` and `versionCode`.
- [ ] Confirm current Room version.
- [ ] Confirm current Backup version.
- [ ] Identify unrelated working-tree changes and preserve them.
- [ ] Load Impeccable when the modification touches UI/UX/accessibility/responsive behavior.
- [ ] Load Ponytail Audit for the post-change global audit.
- [ ] Load relevant official Android skills when the modification touches Android-specific concerns.
- [ ] Map the current implementation and all real call sites before editing.
- [ ] Write down the existing invariant/behavior being preserved.

## A2. Scope protection

- [ ] Confirm the change belongs to the CURRENT roadmap item/version.
- [ ] Do not implement tasks from later versions.
- [ ] Do not add future History / Year Recap architecture.
- [ ] Do not bump Room unless the persistence schema truly changes.
- [ ] Do not bump Backup unless the portable format truly changes.
- [ ] Do not introduce a framework/service/interface merely to remove a few repeated lines.
- [ ] Do not split files merely because they are large.
- [ ] Do not refactor unrelated code while “already in the area.”

## A3. Implementation

- [ ] Make the smallest change that satisfies the product requirement.
- [ ] Reuse existing canonical policies/helpers before adding new ones.
- [ ] Keep Movie/Series asymmetry where semantics require it.
- [ ] Keep Library membership independent from Favorite/rating/history semantics.
- [ ] Preserve Specials exclusion from canonical regular-Series completion.
- [ ] Preserve current local-date source for date-bound availability logic.
- [ ] Preserve TMDB-first navigable identity rules.
- [ ] Preserve cache-first/local-first behavior.
- [ ] Preserve user data during visual/UX changes.
- [ ] Add/modify strings in EN and IT together.
- [ ] Use plurals/resources rather than concatenated user-facing text where applicable.

## A4. Focused verification

- [ ] Add or update focused tests only where behavior ownership changed.
- [ ] Run the narrowest relevant JVM tests.
- [ ] Run the narrowest relevant connected tests when Android/runtime behavior changed.
- [ ] If UI changed, inspect at least one Light and one Dark runtime state.
- [ ] If layout changed, inspect relevant narrow-width/large-font combination.
- [ ] If locale-sensitive, inspect Italian.
- [ ] Do not use arbitrary sleeps to stabilize reactive tests.
- [ ] Classify failures before changing production code.

## A5. Mandatory GLOBAL project audit after the modification

Run a read-only whole-project audit covering all of the following, even if the modification touched one small area:

### Architecture

- [ ] Feature → domain → data boundaries still hold.
- [ ] No new UI → DAO/Retrofit leakage.
- [ ] No unjustified repository/ViewModel responsibility growth.
- [ ] No new speculative layer/interface/factory.

### Ponytail / simplicity

- [ ] Search for newly dead production symbols.
- [ ] Search for wrappers added but no longer needed.
- [ ] Search for single-use abstractions created by the change.
- [ ] Search for duplicated business rules created by the change.
- [ ] Search for obsolete branches/resources left behind.
- [ ] Estimate complexity delta.
- [ ] Explicitly identify justified complexity that should remain.

### Persistence

- [ ] Room migrations/schemas still coherent.
- [ ] Backup format/restore behavior still coherent.
- [ ] No accidental persistence semantics change.
- [ ] No personal-state loss path introduced.

### Flow / concurrency

- [ ] No duplicate broad Room collectors introduced.
- [ ] No stale replay/`first()` test anti-pattern introduced.
- [ ] No blocking work on Main introduced.
- [ ] Cancellation remains correct.
- [ ] Retry behavior remains bounded.

### Performance

- [ ] No obviously eager/unbounded list rendering introduced without need.
- [ ] No repeated expensive mapping/query introduced in recomposition.
- [ ] No unnecessary caching added.
- [ ] Any performance concern is labeled measured/source-supported/speculative.

### Security / privacy

- [ ] No token/credential logging.
- [ ] No secret moved into BuildConfig/export/backup.
- [ ] No exported component/intents broadened unintentionally.
- [ ] Private-tracker data model remains private by default.

### UI / UX — Impeccable

- [ ] Hierarchy remains coherent.
- [ ] New UI uses existing Bingee typography/color/spacing unless intentionally changed.
- [ ] New controls have appropriate action weight.
- [ ] Loading/empty/error states remain coherent.
- [ ] No accidental competing visual dialect introduced.

### Accessibility

- [ ] Interactive icons have labels.
- [ ] Toggle state is exposed.
- [ ] Decorative images are not unnecessarily announced.
- [ ] Touch targets remain adequate.
- [ ] Headings/roles/state descriptions remain correct.
- [ ] No duplicate semantic node introduced.

### Responsive

- [ ] No new clipping at 320/360/411dp.
- [ ] No new large-font failure.
- [ ] Italian strings do not break the changed surface.
- [ ] 600dp behavior does not assume full window width where inappropriate.

### Tests

- [ ] New tests verify behavior rather than implementation details.
- [ ] No redundant low-value test duplication.
- [ ] No English-literal selector added unnecessarily.
- [ ] No flaky timing workaround added.

### Documentation

- [ ] Canonical docs still match changed behavior.
- [ ] No generated audit report added to `docs/`.
- [ ] `.audit/` remains ignored.
- [ ] Historical ADRs were not rewritten as current documentation.

## A6. Audit remediation gate

- [ ] Fix all P0 issues introduced by the modification.
- [ ] Fix all P1 issues introduced by the modification.
- [ ] Remove proven dead code introduced by the modification.
- [ ] Remove unjustified abstractions introduced by the modification.
- [ ] Do not automatically absorb unrelated pre-existing P2/P3 debt into scope.
- [ ] Record pre-existing findings separately if discovered.
- [ ] Re-run focused verification after remediation.

## A7. Modification completion record

- [ ] Record files changed.
- [ ] Record production LOC added/removed approximately.
- [ ] Record abstractions added/removed.
- [ ] Record any intentional duplication left and why.
- [ ] Record any deferred finding and target version.
- [ ] Confirm Room version.
- [ ] Confirm Backup version.
- [ ] Confirm app version remains the correct target for the current milestone.

---

# B. Release gate checklist for EVERY version

## B1. Version

- [ ] Set exact `versionName` from the Version Map.
- [ ] Set exact `versionCode` from the Version Map.
- [ ] Confirm no accidental Room/Backup bump.

## B2. Full verification

Run and report exact PASS / FAIL / NOT RUN:

- [ ] `git diff --check`
- [ ] focused JVM tests
- [ ] full `testDebugUnitTest`
- [ ] focused connected tests where relevant
- [ ] full `connectedDebugAndroidTest`
- [ ] `spotlessCheck`
- [ ] `lintDebug`
- [ ] `assembleDebug`
- [ ] `assembleRelease`
- [ ] `assembleDebugAndroidTest`

For release-oriented milestones also:

- [ ] build/sign the intended release artifact when requested;
- [ ] verify signer;
- [ ] smoke exact signed/minified artifact;
- [ ] compute SHA-256 only after the final artifact is frozen.

## B3. UI regression matrix for UI-touching releases

At minimum:

- [ ] 320dp / Italian / font 1.5
- [ ] 360dp / Italian / font 1.3
- [ ] 411dp / normal font
- [ ] 600dp where relevant
- [ ] Light
- [ ] Dark
- [ ] representative Movie
- [ ] representative Series
- [ ] empty state
- [ ] populated state
- [ ] loading/error state where changed

## B4. Final global audit

Even though every modification already receives a global audit, run one final whole-project synthesis before closing the version.

- [ ] architecture
- [ ] Ponytail / over-engineering
- [ ] dead code
- [ ] duplication/business rules
- [ ] Room/Backup
- [ ] Flow/concurrency
- [ ] performance
- [ ] R8/release robustness
- [ ] tests
- [ ] security/privacy
- [ ] Impeccable UI/UX
- [ ] accessibility
- [ ] responsive/adaptive
- [ ] localization
- [ ] documentation

The final audit must explicitly classify:

- [ ] FIXED IN THIS VERSION
- [ ] PRE-EXISTING / DEFERRED
- [ ] JUSTIFIED / LEAVE ALONE

## B5. Documentation

- [ ] Update canonical docs required by the release.
- [ ] Update roadmap status.
- [ ] Update release notes if maintained.
- [ ] Validate Markdown links.
- [ ] Confirm no `.audit/` artifact staged.

## B6. Git hygiene

- [ ] `git status --short` reviewed.
- [ ] Only intended files staged.
- [ ] No build artifacts staged.
- [ ] No secrets staged.
- [ ] No generated audit reports staged.
- [ ] No unrelated user work overwritten.

## B7. Release decision

Choose exactly one:

- [ ] READY TO COMMIT
- [ ] NOT READY

If READY TO COMMIT, report:

- [ ] recommended commit scope/message;
- [ ] exact version/versionCode;
- [ ] Room/Backup versions;
- [ ] test totals;
- [ ] remaining deferred findings.

---

# C. Version-specific agent checklists

# v1.2.3 checklist

- [x] Bump to v1.2.3 / code 7.
- [x] Fix date-action responsive failure.
- [x] Verify Movie and Series date variants.
- [x] Standardize real Favorite toggles.
- [x] Replace Library-as-Watch-Later wording where semantically wrong.
- [x] Remove redundant Details artwork semantics.
- [x] Resolve Notification poster clipping-layer inconsistency.
- [x] Validate direct Collection shortcut → Statistics navigation.
- [x] Fix navigation only if reproduced.
- [x] Update directly affected canonical docs.
- [x] Run mandatory GLOBAL audit after each numbered modification.
- [x] Run final v1.2.3 global audit.
- [x] Pass release gate.

Modification #1 completion record (2026-10-04): Part II A1–A7 applied; shared date actions now use native Compose `FlowRow`. Movie watched dates and genuinely completed Series dates passed 320dp / IT / 1.5 / Dark, 360dp / IT / 1.3 / Light and 411dp / EN / 1.0 / Light runtime checks, including labels, button roles, touch targets, Edit/Cancel/Save/Clear and retained progress after clearing. Focused JVM tests (25) and connected Details/Series DAO tests (26) passed; changed-file Spotless, lintDebug and debug/test builds passed. Full Spotless still reports pre-existing violations in unrelated files.

- [x] Mandatory GLOBAL read-only audit for **modification #1 only**, covering all eleven A5 categories with Impeccable, Ponytail Audit and official Android adaptive/security skills. No introduced P0/P1, dead code or unjustified abstraction found; pre-existing findings deferred to their planned items. Evidence and the scoped A checklist are temporary under `.audit/v1.2.3-mod1/`. This does **not** complete the all-modifications audit item, final v1.2.3 global audit or release gate above.

Modification #2 completion record (2026-10-04): Part II A1–A7 applied; Collection grid now explicitly uses neutral `contentColor`, semantic `error` for `checkedContentColor`, and the same artwork-safe surface container in both states. Details white-overlay interpolation and opaque error state, Collection list tint, textual overflow action, navigation shortcuts/shelf and Favorites filter remain intact. Native checked roles, resource-backed action labels, round-trip toggling and >=48dp touch targets passed. Final runtime evidence covers populated title Details (overlay and fully opaque toolbar), list and grid in both states at 320dp / IT / 1.5 and 411dp / EN / 1.0, each in Light and Dark. Focused JVM tests (34), connected Favorite tests (3), four runtime matrix cases, changed-file Spotless, lintDebug and debug/test builds passed. Full Spotless fails only on pre-existing formatting in unrelated files. Production Kotlin +3/-5 LOC; no production abstraction or dependency added. App 1.2.3 / code 7, Room 9 and Backup 3 unchanged; no migration or backup-format change. Only `ProfileScreen.kt`, focused `ProfileScreenTest.kt` and this canonical roadmap changed; no commit or staging performed.

- [x] Mandatory GLOBAL read-only audit for **modification #2 only**, covering all eleven A5 categories with Impeccable 4.2.2, Ponytail Audit and official Android adaptive/security skills. No introduced P0/P1, regression, dead code or unjustified abstraction found. Existing retry/Home/radar concerns remain v1.2.4; source-proven unused API/seam candidates and documentation drift remain v1.2.5; other v1.2.3 and v1.3.x UI work remains deferred. Evidence, findings classification and the scoped A checklist are temporary under `.audit/v1.2.3-mod2/`. This does **not** complete the all-modifications audit item, final v1.2.3 global audit or release gate above.

Modification #3 completion record (2026-10-04): Part II A1–A7 applied. Search now labels membership actions Add to collection / Remove from collection; Home Featured uses Add to collection / In collection with a Material plus/check instead of the saved bookmark. EN/IT resources share the same membership meaning. Existing add/remove behavior, Home's add-only saved state, Details Library terminology, genuine Watch Later categories/counts/shortcuts and Favorite independence remain intact. No ViewModel/domain/persistence change or new reactive query. Focused JVM tests (24), connected Search/Home tests (18), full Spotless, lintDebug (0 errors; 51 pre-existing warnings), assembleDebug and assembleDebugAndroidTest passed. Native runtime checks cover populated Movie and Series membership states on Search/Home at 320dp / IT / font 1.3 / Dark and 411dp / EN / font 1.0 / Light, including action callbacks, native button roles and >=48dp touch targets. Production Kotlin +20/-22 and resources +6/-8 LOC; no abstraction or dependency added. App 1.2.3 / code 7, Room 9 and Backup 3 unchanged. Files: SearchScreen.kt, HomeScreen.kt, one corrected YourBingeeScreen.kt comment, EN/IT strings.xml, SearchScreenTest.kt, HomeScreenTest.kt and this roadmap. Existing modification #2 work preserved; no commit or staging performed for #3.

- [x] Mandatory GLOBAL read-only audit for **modification #3 only**, covering all eleven A5 categories with Impeccable 4.2.2, Ponytail Audit and official Android adaptive/security guidance. No introduced P0/P1, regression, dead code, unjustified abstraction or additional query cost found. Details artwork/Notification clipping remain their later v1.2.3 items; existing Home error handling, retry/radar/test-selector concerns remain v1.2.4; unused API/seam candidates and broad documentation drift remain v1.2.5. Inventory, scoped checklist, audit and runtime evidence are temporary under `.audit/v1.2.3-mod3/`. This does **not** complete the all-modifications audit item, final v1.2.3 global audit or release gate above.

Modification #4 completion record (2026-10-05): Part II A1–A7 applied after reading this entire canonical roadmap before implementation inspection. Clean pre-flight on `main` at `c4baae4aee0f3303377654d56cb5a66ee31998f1`, with full Spotless PASS. Runtime/source mapping confirmed separate title-bearing backdrop and poster descriptions plus the visible title heading, with no hero-wide merge. Details now passes `contentDescription = null` through the existing MediaPoster API and both backdrop image branches; loaded, missing and failed artwork remains decorative. Original title, metadata, complete heading text despite visual ellipsis, toolbar labels/actions/Favorite state and all rendering/loading/cache configuration remain unchanged. Removed only the now-unused backdrop description resources in EN/IT. Two focused semantics tests cover Movie/Series, missing/non-null local artwork, authoritative title heading, retained original title/metadata and accessible toolbar. Focused JVM tests (18), connected Details/MediaPoster tests (17), full Spotless, lintDebug (0 errors; 51 pre-existing warnings), assembleDebug, assembleDebugAndroidTest and diff check passed. Pixel_9 runtime checks cover Movie/Series at 411dp / EN / font 1.0 in Light/Dark with loaded, missing and failed local artwork, including separate >=48dp toolbar targets and callbacks. No visible UI change: 11/12 captures are byte-identical; one synthetic poster capture differs only along a 441-pixel internal raster column, with unchanged geometry/title/toolbar. Runtime semantics PASS; TalkBack NOT RUN. Production Kotlin +4/-3 and resources +0/-4 LOC; no abstraction or dependency added. App 1.2.3 / code 7, Room 9 and Backup 3 unchanged. Files: DetailHero.kt, MediaDetailsScreenTest.kt, EN/IT strings.xml and this roadmap. Modifications #1–#3 preserved; #5–#7, final v1.2.3 audit and release gate remain open.

- [x] Mandatory GLOBAL read-only audit for **modification #4 only**, covering all eleven A5 categories with Impeccable 4.2.2, Ponytail Audit and official Android adaptive/security/Compose accessibility guidance. No introduced P0/P1, accessibility regression, dead code, unjustified abstraction, persistence/concurrency change or additional image work found. Notification clipping and shortcut navigation remain #5/#6; existing Home failure handling, retry, radar and test-selector concerns remain v1.2.4; unused API/seam candidates and broad documentation drift remain v1.2.5. Shared poster defaults and meaningful imagery outside Details are protected. Inventory, scoped checklist, audit, verification logs, runtime trees and visual evidence are temporary under `.audit/v1.2.3-mod4/`. This does **not** complete the all-modifications audit item, final v1.2.3 global audit or release gate above.

Modification #5 completion record (2026-10-05): Part II A1–A7 applied from clean `main` at `76e7919f2c49301bc8c81043590baf59bcadce71`. Notifications now delegates thumbnail clipping entirely to the existing `MediaPoster` Material shape (16dp), removing the redundant outer 8dp clip and unused import. The 48×72dp size, card shape, loading/fallback behavior, decorative image semantics, text and navigation remain unchanged. Notifications ViewModel JVM tests (12), existing Notifications/MediaPoster connected tests (5), full Spotless, lintDebug (0 errors; 51 pre-existing warnings), assembleDebug and assembleDebugAndroidTest passed. An ignored harness exercises the actual production screen and lifecycle-owned ViewModel with synthetic local fixtures: 411dp / EN / font 1.0 in Light/Dark before and after, plus 320dp / IT / font 1.0 / Dark and a resized 600dp / EN / font 1.0 / Light window after. Loaded/missing/failed artwork, compact dimensions, decorative semantics, card touch bounds and Details/Back callbacks passed. Both 411dp before/after comparisons have zero different pixels. Final standard APK builds exclude the temporary harness. No permanent implementation-shaped test was added for this modifier deletion. Large-font runtime and TalkBack NOT RUN. Production Kotlin +1/-4 LOC; no abstraction, dependency or reactive query added. App 1.2.3 / code 7, Room 9 and Backup 3 unchanged. Files: NotificationsScreen.kt and this roadmap. Modifications #1–#4 preserved; #6–#7, final v1.2.3 audit and release gate remain open.

- [x] Mandatory GLOBAL read-only audit for **modification #5 only**, covering all eleven A5 categories with Ponytail Audit, Impeccable 4.2.2 and official Android adaptive/intent-security guidance. No introduced P0/P1, regression, dead code, unjustified abstraction or persistence/concurrency change found. Existing Home failure handling, retry, radar and test-selector concerns remain v1.2.4; NotificationsScreenTest's test-local replacement also merits production-coverage review during v1.2.4 test/release work. Unused API/seam candidates and broad documentation drift remain v1.2.5. Inventory, scoped checklist, audit, verification logs and runtime evidence are temporary under `.audit/v1.2.3-mod5/`. This does **not** complete the all-modifications audit item, final v1.2.3 global audit or release gate above.

Modification #6 completion record (2026-10-06): Part II A1–A7 applied from clean `main` at `04821de3a5850a8aed7405f75bcc2bf5d56c0f4b`. The production MainActivity/NavHost regression reproduced `IllegalArgumentException: No destination with route profile is on the NavController's back stack` when opening Statistics from the cold Watching launcher shortcut. Statistics now retains the existing Your Bingee ViewModel owner when present and otherwise shares the calling Collection entry, with its own entry as the final fallback. No synthetic ancestor, new route/argument, abstraction, dependency, data write or collector. Dashboard statistics selections remain shared across all five Collection scopes; both supported direct shortcuts keep Back/Up to Collection then Home. Startup/Profile JVM tests (18), production launcher/navigation connected tests (6), full Spotless, lintDebug (0 errors; 51 pre-existing warnings), assembleDebug, assembleDebugAndroidTest and diff check passed. Four new connected cases cover cold Watching/Watch Later, both warm shortcuts through real onNewIntent, verified new Activity instances and retained statistics scope after recreation, plus the ordinary dashboard/Collection paths. Resource/selected-control selectors avoid duplicate Movies/Series text and do not require an empty database. Known shortcut intents are consumed by MainActivity; cold tests use the existing Search cleanup pattern, while the neutral-launch warm test requests real Activity recreation directly to avoid ActivityScenario's stale intent-based stage matcher. The standard debug APK also passed native runtime checks outside instrumentation at 411dp / EN in Light / font 1.0 and Dark / font 1.3; Back after configuration recreation returned Statistics → Collection → Home. TalkBack, 600dp/full responsive matrix and release/minified smoke NOT RUN. Production Kotlin +8/-3 LOC; no abstraction added/removed. App 1.2.3 / code 7, Room 9 and Backup 3 unchanged. Files: BingeeNavHost.kt, LauncherShortcutNavigationTest.kt, the directly affected navigation paragraph in docs/architecture.md and this roadmap. Modifications #1–#5 preserved; #7, final v1.2.3 audit and release gate remain open.

- [x] Mandatory GLOBAL read-only audit for **modification #6 only**, covering all eleven A5 categories with Ponytail Audit and Impeccable 4.2.2 native Android guidance. The reproduced supported-path crash is fixed; no introduced P0/P1, dead code, unjustified abstraction, persistence/concurrency change or visual regression found. Existing Home failure handling, retry, radar, brittle old test selectors and Notifications test-local coverage remain v1.2.4; unused API/seam candidates and broad canonical version drift remain v1.2.5. Fresh inventory, scoped checklist, scans, audit, failed baseline/diagnostic logs, successful final verification and native UI evidence are temporary under `.audit/v1.2.3-mod6/`. This does **not** complete the all-modifications audit item, final v1.2.3 global audit or release gate above.

Modification #7 completion record (2026-10-06): Part II A1–A7 applied. README, docs/roadmap.md, docs/architecture.md and v1.2.3 release notes document the directly changed membership wording, Favorite state, wrapping date actions, decorative Details artwork, shared Notification poster rounding and shortcut/Statistics ownership. General version-prose synchronization remains v1.2.5. The existing #6 changes were preserved. The full Android gate reproduced a separate pre-existing P1 in populated Your Bingee Watching/Favorites shelves: LazyRow keys used ExternalMediaRef, which Android cannot save in a Bundle. After explicit owner approval, both shelves now use the existing Collection convention of provider/type/ID string keys. Four existing regressions and a new same-ID Movie/Series saved-state case pass; all ten dashboard tests pass. Production Kotlin for the authorized fix +3/-3 LOC; #7 documentation adds no production code. No abstraction, dependency, query, Room or Backup change. App 1.2.3 / code 7, Room 9, Backup 3. No commit or staging performed.

- [x] Mandatory GLOBAL read-only audit for **modification #7**, covering all eleven A5 categories, plus final v1.2.3 synthesis with Caveman ultra, Ponytail Audit and Impeccable native Android guidance. Fresh inventory/scans and source/test review confirm the six planned fixes and earlier export/preference/share/notification-claim/dispatcher/CI/dead-code fixes. Notification recovery retains the documented at-least-once limit; eager episode composition, radar/font constraints and broad documentation drift are explicitly not claimed resolved. The separately discovered dashboard crash was fixed only after owner approval. No newly introduced unresolved P0/P1, dead code or unjustified abstraction found. Reports remain temporary under `.audit/v1.2.3-mod7/` and `.audit/v1.2.3-final/`; historical audit documents and ADRs remain unchanged.

Final v1.2.3 release gate (2026-10-06): **READY TO COMMIT**. PASS: full JVM 460 tests, full Android 246 tests on API 35 (zero failures/errors/skips), focused dashboard 10 tests, full spotlessCheck, lintDebug (0 errors; 51 pre-existing warnings), assembleDebug, assembleRelease with R8 optimization, assembleDebugAndroidTest, git diff --check, and 47 relative Markdown links across 52 files. The final combined Gradle run completed successfully in 12m25s after the authorized fix. Room migration/backup/credential/notification and real shortcut/navigation tests are included. The first boot attempt failed because Android PackageManager was not ready; the next complete baseline run reproduced four genuine dashboard failures, now fixed. No tests disabled or timing sleeps added.

UI gate uses the recorded per-item native evidence at 320dp/IT/font 1.5, 360dp/IT/font 1.3 and 411dp/EN/font 1.0, Light/Dark, Movie/Series and the relevant state variants, plus current permanent connected regressions. No new geometry, pane or visual treatment changed in #7 or the authorized key fix. Full-app 600dp/font-2.0/TalkBack coverage and signed/minified runtime smoke remain their dedicated v1.2.4/v1.3.4 work, not claims of this gate. Release signing/publication was not requested. Deferred: Home errors/retry/radar/test hardening v1.2.4; source-proven cleanup candidates/general documentation sync v1.2.5; the larger UI/adaptive cycle v1.3.x. Recommended commit scopes: the preserved #6 navigation fix, the authorized dashboard-key regression fix, and targeted v1.2.3 documentation. Nothing staged; no generated audit report, secret or build artifact included.

# v1.2.4 checklist

- [x] Bump to v1.2.4 / code 8.
- [x] Audit English-literal instrumentation selectors.
- [x] Replace only brittle selectors with semantic/resource-based equivalents.
- [x] Run release/minified smoke including Glance, notification deep links and shortcuts.
- [x] Review Home observation error swallowing.
- [x] Change only if inconsistent behavior is proven.
- [x] Review shared Flow retry policy.
- [x] Change only if a concrete issue is proven.
- [x] Make radar sizing container-aware where window assumption remains.
- [x] Update directly affected docs.
- [ ] Run modification #7 global hardware-efficiency audit covering CPU/rendering, memory, database/disk, network and battery/background work.
- [ ] Classify measured bottlenecks separately from source-level hypotheses; verify any applied optimization with comparable evidence.
- [ ] Run mandatory GLOBAL audit after each numbered modification.
- [ ] Run final v1.2.4 global audit.
- [ ] Pass release gate.

Modification #1 completion record (2026-10-08): Part II A1–A7 applied from clean `main` at `fb1358a025fa681b1b4b07e73c0c75de18684bb6`. Ordinary instrumentation selectors now use existing resources, plurals, localized month arrays and semantic roles/state across screens and Activity navigation; fixture titles and deliberate visible-copy contracts remain literal. Explicit EN/IT vocabulary and singular/plural expectations remain. A test-only `TestLocaleRule` applies `uiLocale=en` (default) or `it` before Compose Activity creation, including native dialogs/menus, and restores the previous Android app locale without changing saved Bingee preferences. README documents the Italian suite command. Notifications tests now exercise the production content instead of a test-local replacement; the route retains ViewModel, Flow and snackbar ownership. Added loading/error/retry/Back coverage. No visual geometry, persistence, dependency or navigation change. Production Kotlin +22/-2 LOC; one route/content boundary reused by production and tests, no generic framework or new reactive query. App 1.2.4 / code 8, Room 9 and Backup 3 unchanged.

Verification: full JVM **460**, full connected Android **247**, and Italian component/package suite **105**, all with zero failures/errors/skips on Pixel_9 API 35. Full spotlessCheck, lintDebug (0 errors; 51 pre-existing warnings), assembleDebug, assembleDebugAndroidTest, diff check and 47 local Markdown links passed. Real Notifications content captures at 411dp / font 1.0 cover EN/Light and IT/Dark; headings, event copy, decorative posters and navigation remain coherent. Initial Italian failures were test-context and selector issues, corrected and rerun without disabling tests or adding sleeps. Full TalkBack, expanded-window/font-2.0 coverage and release/minified runtime remain their dedicated milestones. The requested global hardware-efficiency audit was added as planned #7 before #1 implementation; no hardware improvement or completed #7 is claimed.

- [x] Mandatory GLOBAL read-only audit for **modification #1 only**, covering all eleven A5 categories with Caveman ultra, Ponytail Audit, Impeccable native Android and applicable Android testing/adaptive/intent-security guidance. No introduced unresolved P0/P1, dead production code or unjustified abstraction found. Existing Home error handling/retry/radar remain #3–#5; hardware profiling hypotheses remain #7; source-proven cleanup candidates and general documentation drift remain v1.2.5. Reports, scoped checklist, diagnostic/final logs and visual evidence remain temporary under `.audit/v1.2.4-mod1/`. This does **not** complete the all-modifications audit item, final v1.2.4 global audit or release gate above.

Modification #2 completion record (2026-10-08): Part II A1–A7 applied from clean `main` at `0bffe236318fdd1f6f8c0b9dc5f61cc606ba9099`. The actual optimized APK reproduced three failures: R8 removed the reflective Glance callback constructor; warm Details intents for another qualified title reused the previous ViewModel; an active widget session retained its initial snapshot after a successful episode write. Fixes retain only the public no-argument constructor with `@Keep`, create a navigation entry when the qualified identity changes, and collect existing Room/date/theme inputs inside both Glance compositions. Same-title delivery remains single-top. Poster production follows URL changes. Two real-Activity regressions cover qualified identities, Back, duplicate delivery and recreation; two JVM regressions cover live snapshot updates, day rollover and cancellation. No dependency, SDK, schema, backup or signing configuration change. Production Kotlin +68/-19 LOC; no generic framework added. App 1.2.4 / code 8, Room 9 and Backup 3 unchanged.

Verification: full JVM **462** and focused real notification/launcher Android **12**, zero failures/errors/skips; full spotlessCheck, lintDebug (0 errors; 51 pre-existing warnings), assembleDebug, assembleDebugAndroidTest, optimized assembleRelease, diff check and 47 local Markdown links passed. The navigation tests ran after their final changes; the subsequent widget-only fix passed the final JVM/build gate and actual release widget checks. This does not claim a new full connected-suite run. The non-debuggable APK was signed only as a local test copy; every original code/resource ZIP member is unchanged, the installed hash matches, and optimized DEX exposes the callback's public constructor. On the dedicated API 35 launcher, the final artifact passed cold launch, credential validation, Movie/Series Search and Details, library add/remove, Favorites/rating/progress, Your Bingee, exact Statistics, Settings, a real worker notification after process death, all three native shortcuts and both native widget navigation/actions with immediate state refresh. Relaunch without an active network preserved Movie watched/rating/Favorite, Series 2/2 progress, calendar and 3h20m totals. EN/Light and IT/Dark visual checks passed at 411dp / font 1.0. Final app-scoped logs contain no fatal, class-loading, verification, Gson or reflective callback error.

- [x] Mandatory GLOBAL read-only audit for **modification #2 only**, covering all eleven A5 categories with Caveman ultra, Ponytail Audit and Impeccable native Android guidance; Android CLI/testing guidance supports the runtime verification. No introduced unresolved P0/P1, dead code or unjustified abstraction found. README and architecture document the directly changed release procedure and navigation/widget ownership. Reports, logs, APK hashes/mapping and synthetic runtime evidence remain ignored under `.audit/v1.2.4-mod2/`; owned proxy/logger/AVD processes were stopped. External synthetic HTTPS fixtures exercise the production app graph without debug fakes or real credentials. Host QEMU access violations required an isolated API 35 AVD/restart and are recorded as infrastructure failures. Real TMDB availability, full TalkBack/adaptive coverage and hardware CPU/heap/battery gains are not claimed. Existing Home/retry/radar work remains #3–#5; hardware hypotheses remain #7; cleanup/general documentation sync remains v1.2.5. This does **not** complete the all-modifications audit item, final v1.2.4 audit or release gate above.

Modification #3 completion record (2026-10-08): Part II A1–A7 applied from clean `main` at `895458c1f90365f3aa4702e5d768b4d27825e61d`. A finite failure-flow regression proved that Home ignored collection/continuation read errors and Local Retry could not restart their completed observations. Home now retains successful calendar, membership and continuation values, exposes source-owned recoverable errors, and reuses the existing localRetry/flatMapLatest to cancel and replace all three reads. Each warning clears only after its own successful read. The existing native ErrorState provides one resource-backed retry: cached calendar takes priority, followed by membership/continuation; an uncached calendar failure uses its original error item without a duplicate warning. Optional featured discovery and last-refresh metadata intentionally remain nonblocking. No global error bus, automatic retry framework, dependency, schema or backup change. Production Kotlin +45/-6 LOC and EN/IT resources +4/-0; App 1.2.4 / code 8, Room 9 and Backup 3 unchanged.

Verification: full JVM **466**, connected Home Android **12**, and new-case Italian narrow Android **2**, zero failures/errors/skips. Four new JVM cases cover finite failure recovery, repeated retry/cancellation/disposal, independent errors, retained calendar/timestamp and optional discovery. Two native cases cover retained content, partial/full recovery, initial failure, unique retry, heading/button semantics and >=48dp touch targets. Native visual checks passed retained/partial/recovered Light/font 1.0 and initial-failure Dark/font 1.5 at 411dp/EN and 320dp/IT on the dedicated API 35 AVD. Final spotlessCheck, lintDebug (0 errors; 51 pre-existing warnings), assembleDebug, assembleDebugAndroidTest, diff check and 47 local Markdown links passed. Temporary native capture code was removed before the final normal gate. These component tests use production HomeContent with synthetic local failure state; full TalkBack, whole-app adaptive coverage, fresh signed/minified runtime and hardware gains are not claimed. Architecture documents only the directly changed Home policy. Nothing staged or committed for #3.

- [x] Mandatory GLOBAL read-only audit for **modification #3 only**, covering all eleven A5 categories with Caveman ultra, Ponytail Audit and Impeccable native Android guidance. No introduced unresolved P0/P1, dead production code or unjustified abstraction found. Evidence, scoped checklist, logs, baseline regression and eight final native frames remain ignored under `.audit/v1.2.4-mod3/`. Shared Flow retry and radar remain #4/#5; global hardware-efficiency work remains #7; source-proven cleanup/general documentation sync remains v1.2.5. This does **not** complete the all-modifications audit item, final v1.2.4 audit or release gate above.

Modification #4 completion record (2026-10-10): Part II A1–A7 applied from clean `main` at `dcb7e610e23b5d58b7d8d365bf7e48db0352aa72`. A production-repository regression proved that shared progress failures closed Collection, Details membership and personal-viewing observations before the existing retry could deliver recovery. The three projections now preserve `Result` through `combine`, then unwrap inside the existing per-emission persistence mapper, reusing `persistenceRead`. The original subscriptions receive safe errors, recovery and later Room updates. Retry stays at a fixed one-second delay with no attempt cap while observed; the five-second final-subscriber grace period cancels the source and pending delay. No retry framework, production symbol, dependency, scope, query, schema, backup, UI or navigation added. Production Kotlin +39/-33 LOC (net +6); App 1.2.4 / code 8, Room 9 and Backup 3 unchanged.

Verification: full JVM **466**, connected repository Android **25** (21 existing and four new), zero failures/errors/skips. New tests cover all three consumers recovering on their original subscriptions, later real Room writes, cancellation of one consumer while others remain, repeated initial failures and the one-second minimum interval, no eager work before subscription, stopping persistent failures after the last consumer leaves, return after stop, and cancellation of a healthy source. Failures are injected into the shared upstream date source with real in-memory Room; physical storage faults are not simulated. The baseline fails with `ClosedReceiveChannelException`; the first incomplete mapper-only correction also fails, and the final complete correction passes. Final spotlessCheck, lintDebug (0 errors; 36 warnings in unchanged files), assembleDebug, assembleDebugAndroidTest and diff checks pass. Architecture documents the directly affected recovery policy. Full connected suite, fresh signed/minified smoke, TalkBack/adaptive matrix and hardware gains are not claimed. The owned readonly emulator session was stopped. Nothing staged or committed for #4.

- [x] Mandatory GLOBAL read-only audit for **modification #4 only**, covering all eleven A5 categories with Caveman ultra, Ponytail Audit, Impeccable native Android and Android testing/CLI guidance. No introduced unresolved P0/P1, dead production code or unjustified abstraction found. Evidence, logs, source scans and the scoped checklist remain ignored under `.audit/v1.2.4-mod4/`. Radar sizing remains #5; global hardware-efficiency work remains #7; source-supported cleanup and general documentation sync remain v1.2.5. This does **not** complete the all-modifications audit item, final v1.2.4 audit or release gate above.

Modification #5 completion record (2026-10-10): Part II A1–A7 applied from clean `main` at `1f5771d819185006620fb7a600c59be91d92f6f9`. The taste radar now derives its height cap from the existing BoxWithConstraints container instead of window width minus assumed screen padding; Canvas supplies the height inside that container. Labels, geometry, colors, relative Top 6 normalization, scopes and the existing 260dp/font-scale policy remain intact. A production-APK regression proves the original radar stays 390dp high inside a genuinely constrained 320dp pane of a 600dp window, despite its measured width being 272dp; the correction follows 390 → 272 → 390dp, including the resized empty frame. Three new native cases also cover native-phone parity and 320/360/411dp constraints with fonts 1.0/1.3/1.5. Six cropped/full-content PNGs on the native phone and full 600dp container are byte-identical before/after. Production Kotlin +7/-10 LOC (net -3); no production symbol, layout wrapper, subcomposition, dependency, query, schema or backup added. App 1.2.4 / code 8, Room 9 and Backup 3 unchanged.

Focused JVM statistics/ViewModel tests (24), all Statistics connected tests (13), the three new cases in Italian (3), full JVM suite (466), full Spotless, lintDebug (0 errors; 36 pre-existing warnings), assembleDebug, assembleDebugAndroidTest and diff check passed. Bounded native inspection covers Light/Dark, EN/IT, narrow/large-font, the 600dp window with a narrower pane and the empty radar. ForcedSize verifies constrained Compose layouts, not physical tablet hardware; TalkBack, font 2.0 and full foldable/release coverage are not claimed. Existing EN segmented-filter wrapping at 320dp/font 1.5 remains v1.3.4; it was not changed by radar sizing. Files: StatisticsScreen.kt, StatisticsScreenTest.kt, the directly affected architecture paragraph and this roadmap. Temporary capture code was removed; the owned read-only emulator session was stopped. No commit or staging performed for #5.

- [x] Mandatory GLOBAL read-only audit for **modification #5 only**, covering all eleven A5 categories with Caveman ultra, Ponytail Audit, Impeccable native Android and Android adaptive/testing/CLI guidance. No introduced unresolved P0/P1, dead production code or unjustified abstraction found. Evidence, source scans, scoped checklist, diagnostic/final logs and synthetic native images remain ignored under `.audit/v1.2.4-mod5/`. Global hardware-efficiency work remains #7; source-supported cleanup/general documentation sync remain v1.2.5; broader adaptive/accessibility work remains v1.3.4. This does **not** complete the all-modifications audit item, final v1.2.4 audit or release gate above.

Modification #6 completion record (2026-10-10): Part II A1–A7 applied from clean `main` at `3ac2e31a822351c9525a9f715612e8f3e85d1e42`. Added [v1.2.4 implementation notes](docs/release-notes-1.2.4.md), linked them from README and docs/roadmap.md, and checked the existing architecture policies against production source. The notes cover localized instrumentation/production Notifications content, the optimized Glance callback and active-session updates, qualified detail-intent navigation, Home local error/retry behavior, shared-progress recovery/pacing/cancellation and container-aware radar sizing. README now identifies the current development version and Room9; active architecture headings and the Library diagram no longer carry obsolete version labels. Historical ADRs and broader canonical synchronization remain unchanged. Production/test LOC delta is zero; no abstraction, dependency, version bump, schema, backup, UI or runtime behavior change. App1.2.4/code8, Room9 and Backup3 retained.

Acceptance review of #1–#5 confirms the required implementations, permanent regressions and recorded scoped evidence, with no blocking acceptance gap found. The real minified smoke is evidence for the #2 artifact, not a new smoke of current HEAD. The current documentation-only gate passes spotlessCheck, testDebugUnitTest, lintDebug, assembleDebug and assembleDebugAndroidTest; Gradle reuses unchanged inputs/results (466 JVM tests, zero failures/errors/skips; lint 0 errors, 36 existing warnings). Fresh local Markdown file/heading/source-line validation and diff check pass. No new connected/minified run, TalkBack certification or hardware measurement is claimed. Items #1–#6 are complete within their scope; #7, the final global audit and release gate remain open. No staging or commit performed for #6.

- [x] Mandatory GLOBAL read-only audit for **modification #6 only**, covering all eleven A5 categories with Caveman ultra, Ponytail Audit, Impeccable native source-review guidance and Verify and Stop. No introduced unresolved P0/P1, dead production code or unjustified abstraction found. Fresh source inventory/scans, #1–#5 acceptance review, link results, gate log and scoped checklist remain ignored under `.audit/v1.2.4-mod6/`. Hardware-efficiency work remains #7, cleanup/general documentation sync remain v1.2.5, and broader adaptive/accessibility work remains v1.3.4. This does **not** complete the all-modifications audit item, final v1.2.4 audit or release gate above.

# v1.2.5 checklist

- [x] Bump to v1.2.5 / code 9.
- [x] Close the agreed [performance scope](docs/checklist-1.2.5-performance.md#stato-di-chiusura--18-ottobre-2026), distinguishing verified work, accepted limits and deferred matrices.
- [x] Run fresh repo-wide Ponytail dead-code audit.
- [x] Re-evaluate every known historical dead-code candidate.
- [x] Remove only proven dead production code (C01–C07).
- [x] Remove or relocate obsolete resources/tests/fixtures only with evidence; preserve the regression suite.
- [x] Preserve the existing architecture.
- [x] Perform full canonical documentation sync.
- [x] Scan all tracked Markdown links.
- [x] Confirm `.audit/` ignored and canonical `docs/` tracked.
- [x] Retain previously recorded cleanup-batch reviews; record the owner's deferral of any new global audit to v1.2.6.
- [x] Complete final release-runtime checks and the full Android suite within the documented coverage.
- [x] Resolve the final backup wording finding in EN/IT: export v3, restore v1/v2/v3.
- [x] Close the agreed v1.2.5 verification gate and preserve the baseline for v1.2.6 before v1.3.0.

## Final closure record — 2026-10-18

The owner's three-step closure sequence is complete: final verification, full Android suite and
documentation alignment. No additional product issue emerged after correcting the backup wording.
Only documentation changes in the final alignment; no completed optimization is reopened.

Final verification used an isolated R8, non-debuggable release on HONOR CMA-LX1 / Android 13.
Navigation and offline cached screens passed in EN/IT, Light/Dark, widths 320/360/411/600dp and
font scales 1.0/1.3/1.5 across four configurations. These are device configuration checks, not a
certification across all devices or TalkBack. A synthetic 47,518,199-byte backup (45.32 MiB;
1,000 titles, 2,700 seasons, 27,000 episodes and progress rows) restored in 10.225 s and re-exported
in 14.858 s with identical JSON content and bytes. Preview preserved the database; a forced restore
failure rolled back all tables in 17.294 s. No OOM, crash or ANR was observed in those scenarios;
this does not establish a continuous memory peak or complete SAF-picker UI coverage.

After the EN/IT wording correction, the unfiltered Android suite passed **276/276** on the same
HONOR, zero failures/errors/skips. A separate Italian backup-screen test passed. Spotless, lint,
normal debug/AndroidTest and optimized release builds passed. Lint has zero errors and 38 warnings;
496 passing JVM results were reused by Gradle as UP-TO-DATE because their inputs were unchanged.
Both corrected translations were checked inside the normal debug and release APKs.

All existing device packages, ten widget bindings and display/font settings were preserved; own
diagnostic packages were removed. Raw evidence stays ignored under `.audit/final-verification/`
and `.audit/closure-android/`. Publication/signing/tagging/commit/push remain separate explicit
actions, not unfinished optimization tasks or implied parts of this closure.

Earlier dated records below are historical snapshots; their open-gate statements do not supersede
this final status. Residual warm-filter frames, broad heap/image/device matrices and further query
selection are recorded limits or future study, not claims of completed measurements.

## C01–C03 cleanup batch — 2026-10-10

The first v1.2.5 cleanup batch removes the unused Library entry-count API and DAO query (C01), the repository-only one-shot membership API (C02), and the obsolete season-summary interface, Hilt binding and no-op test fake (C03). Existing observable membership, the DAO checks consumed by TV Time and the concrete transactional season writes remain intact. Three existing instrumented membership regressions now assert through `observeEntry`. No tests were removed or disabled. The Kotlin diff is +11/-78, net -67 lines; production main is +2/-25, net -23 lines. Room stays at 9 and backup at 3.

The same 70 focused JVM tests passed before and after the cleanup. The full JVM suite passed all 466 tests in 80 suites with zero failures, errors or skipped tests. The host gate passed `spotlessCheck`, `testDebugUnitTest`, `lintDebug`, `assembleDebug`, `assembleDebugAndroidTest` and `assembleRelease` in 6m07s (44 tasks executed, 106 up-to-date). Lint reports zero errors and 51 existing warnings. Per owner direction, physical-device verification replaces the planned emulator pass: all 70 selected instrumented tests in seven suites passed on HONOR CMA-LX1 / Android 13, including Room transactions, imports and migrations. A locally signed, non-debuggable optimized release passed a bounded Home/Your Bingee/carousel smoke. Initial startup, frame and memory measurements are recorded under `.audit/v1.2.5-device/`; remaining performance coverage, widget checks and the full release gate remain pending.

A subsequent partial synthetic baseline uses an isolated optimized-release package on the same device, without changing the main app's data. Three fixtures cover 100/1,000/10,000 titles and 1,000/25,000/100,000 watched episodes. Search and statistics checks pass; 30 process-cold/cache-warm launches and nine season expansion/collapse cycles establish comparison data. Expansion at 300 episodes has repeatable frame stalls, supporting the existing lazy-episode item in the performance checklist. No performance fix or cleanup regression is claimed. Evidence stays ignored under `.audit/v1.2.5-synthetic/`; observers, widgets, refresh and backup heap scenarios remain pending.

A scoped global source audit covered all eleven A5 categories using Safe Refactor, Ponytail Audit, Impeccable native guidance and the existing Android testing strategy. No introduced unresolved P0/P1, dead code or unjustified abstraction was found. Local Markdown file targets and the diff check passed. Temporary evidence remains ignored under `.audit/v1.2.5-c01-c03/`. C04–C07, performance, full documentation sync, final global audit and release gate remain open. No staging or commit was performed.

## C04–C07 cleanup batch — 2026-10-10

The remaining four candidates are reconfirmed and implemented. C04 moves fake-only in-memory search into debug, retaining the production normalization/state/sort helpers. C05 removes fake-only default bodies from Library/calendar contracts and reuses the same algorithms through two debug-only observation helpers; existing Room implementations and the Notifications fake's custom bounded observation are unchanged. C06 removes the unconsumed completion getter and preserves assertions on precise movie/series timestamp fields and the explicit watched date. C07 removes only the unused import timestamp flag, retaining original text, instant, fractional precision and real approximation warnings. No test is removed or disabled. This batch reduces main source by another 46 net lines; C01–C07 reduce main by 69 net lines. App 1.2.5/code 9, Room 9 and backup 3 remain unchanged.

The 154 targeted JVM tests in 17 suites pass, including three new regressions for fake continuation eligibility/order, error recovery/cancellation, and the inclusive calendar bound. The full host gate passes: global Spotless, 469 JVM tests in 81 suites with zero failures/errors/skips, lint with zero errors and 51 pre-existing warnings, debug build, AndroidTest APK build and optimized release build. Compiled release classes exclude the debug helpers. A scoped global source audit covers all eleven A5 categories and finds no introduced unresolved P0/P1, dead code or unjustified abstraction; local file links and the diff check pass.

The widget performance item was initially mistaken for this cleanup scope: its prepared patch is parked only under ignored `.audit/` and is not included in the source tree. New device/emulator verification is deferred at the owner's request. Temporary evidence remains ignored under `.audit/v1.2.5-c04-c07/`. This batch does not complete performance work, full documentation sync, final milestone audit or the release gate. No staging or commit is performed.

## Documentation sync and local-link scan — 2026-10-11

Current documentation now agrees with development v1.2.5/code 9, Room 9, Backup v3 export with v1/v2/v3 restore, the implemented TV Time JSON ZIP profile and its limits, actual Library/Profile observations, and CI's offline smoke selection. README, PRODUCT, AGENTS, architecture, privacy, backup/import docs, roadmap, contribution/security guidance and ADR index were reviewed against source. The linked ADR index identifies superseded decisions; historical ADR bodies, old release notes, Room exports and JSON schemas are unchanged. The new v1.2.5 implementation notes describe completed cleanup while retaining the remaining performance/release work. Historical competitor research is labelled with its baseline. The old tracked `audit-24-09.md` report is archived byte-for-byte in ignored `.audit/archived/`.

The host gate `spotlessCheck testDebugUnitTest lintDebug assembleDebug --offline --console=plain` passes in 34s (one task executed, 62 up-to-date). Gradle reuses the unchanged-source results: 469 JVM tests in 81 suites, zero failures/errors/skips, and lint with zero errors and 51 existing warnings; this is not a fresh test execution. The local-link scan covers tracked Markdown plus the new implementation notes, including heading anchors. A fresh global source audit covers all eleven A5 categories and finds no introduced unresolved P0/P1, dead code or unjustified abstraction. Evidence stays ignored under `.audit/v1.2.5-docs/`. No application code, dependency, schema or Android runtime behavior changes; no device operation, staging, commit or push is performed. Performance work, final milestone audit and release gate remain open.

## Verification restart and physical-device test hardening — 2026-10-17

The local Android environment has been restored with JDK 21, SDK Platform 36.1, Build Tools 36.0.0 and ADB. A dedicated API 33 AVD completes boot with WHPX. Physical verification uses a separate application ID on HONOR CMA-LX1 / Android 13, preserving existing installed apps and their data. App v1.2.5/code 9, production code, dependencies, Room 9 and Backup 3 remain unchanged.

The first full physical run exposed five test-harness failures. Three AndroidTest files now wait conditionally for the first shortcut destination while startup/language changes settle, check complete notification bodies in explicit English and Italian contexts, compare the width-limited radar with its measured width under fractional test density, and bring the chart container into the vertical viewport before horizontal interactions. Selection, navigation, minimum-target and disabled-state assertions remain in place; no tests are removed, skipped or disabled. The five focused cases pass, followed by all 264 Android tests in 48 suites and 15 representative Italian Statistics/notification tests, with zero failures, errors or skips. The host checks pass with 486 JVM tests, no lint errors and 51 warnings; debug, AndroidTest and optimized release APKs build. The optimized DEX retains the widget callback's public no-argument constructor.

A separately packaged, locally test-signed R8 release is verified non-debuggable and passes a bounded HONOR smoke: credential-free onboarding, Home, Your Bingee, Settings, Light/Dark rendering and persistence of the Light preference after process restart. Its scoped process logs contain no fatal, class-loading, verification or JSON parsing error. The temporary app is then removed; existing packages remain installed. This empty-data smoke does not cover populated titles, live widget callbacks, provider parsing or performance.

These results restore functional verification; they do not replace the remaining image/widget and backup-memory measurements, the Statistics PSS investigation, the final milestone audit or the full release gate. Evidence stays ignored under `.audit/environment/`. v1.2.5 remains in progress; v1.3.0 is not started.

A subsequent isolated R8 Statistics investigation on the same HONOR repeats 20 background/foreground cycles with 100,000 synthetic watched activities and three cycles each with 1,000/25,000 activities. Unchanged-filter post-GC Java occupancy stays stable (17.31 to 17.35 MiB at 100,000 activities), but cached rating/UI snapshots retain duplicate episode histories: 200,000 activity objects normally and 400,000 in the filter/reopen scenario. Popping Statistics and its Collection owner releases all observed activity objects and the ProfileViewModel. This identifies avoidable UI retention without establishing an unbounded leak or reconstructing the lost historical +10 MiB PSS comparison. Production sources were unchanged in that investigation; 45 focused JVM tests and standard debug/release builds passed. The normal unsigned APK was byte-identical to the pre-investigation artifact; the diagnostic package was removed. Temporary evidence stays ignored under `.audit/statistics-memory/`.

## Statistics history retention fix — 2026-10-17

Rating shelf results now retain title metadata and totals without episode activity lists; the full calculation input, offline state, date/year totals, rating order and navigation remain intact. The five-line production change has a regression observed failing before the fix and passing afterward. All 487 JVM tests in 82 suites and 42 focused physical Android tests in five suites pass with zero failures, errors or skips. Spotless, lint (zero errors, 51 existing warnings), debug/AndroidTest and optimized release builds pass; independent scoped Ponytail review finds no issues. App 1.2.5/code 9, Room 9 and Backup 3 remain unchanged.

The same isolated R8 HONOR scenarios are repeated: 20 reopen cycles with 100,000 activities, three cycles each with 1,000/25,000 activities, filter/reopen controls and owner teardown. Episode-activity objects fall from 2N–4N to N: UI snapshots hold none, the calculation cache remains complete, stopped Room projections release their rows and popping the owner releases all activities and the ViewModel. At 100,000 activities, post-GC Java usage falls from 17.35 to 13.16 MiB after 20 reopens and from 25.25 to 12.67 MiB in the filter control. The [performance checklist](docs/checklist-1.2.5-performance.md) closes this retention item; approximately 157 MiB of cumulative temporary allocation per large-history reload, aggregation timings and the other performance/release scenarios remain separate. No frame/battery or exact reconstruction of the lost PSS comparison is claimed. Diagnostic evidence remains ignored under `.audit/statistics-memory/fix/`; diagnostic components stay out of the normal APK. v1.2.5 remains in progress and v1.3.0 is not started.

## Statistics calculation and temporary-memory baseline — 2026-10-17

A subsequent bounded measurement uses the same isolated R8 HONOR and 100-series fixtures with
1,000/25,000/100,000 watched activities. Twelve warm calculations measure median watched aggregation
at 0.83/11.87/48.39 ms. Five first-result repository trials measure 27/480/2,081 ms; loading and
materializing the activity rows dominate the large fixture. Separate memory passes measure cumulative
repository allocations of 2.22/37.52/146.89 MiB per load and sampled Java maxima of 11.78/15.52/23.33 MiB
over three consecutive loads. Sampled occupancy is a lower bound on the peak, not cumulative allocation
or retained memory. GC counters and UI producer/request publication timings are recorded separately.

Rating/genre recalculation costs approximately 0.3 ms at the fixed 100-title count, so the conditional
cache optimization is deliberately omitted. No production code, schema, backup, dependency or version
changes are introduced. This closes the scoped timing/temporary-memory baseline in the
[performance checklist](docs/checklist-1.2.5-performance.md), without claiming a before/after speedup,
frame or battery benefit, cold-disk performance or complete Statistics performance coverage.
Raw results, reproducible scripts, diagnostic overlays and limitations remain ignored under
`.audit/statistics-timing/`. Other performance scenarios, final milestone audit and release gate remain
open; v1.3.0 is not started.

## Statistics loading optimization — 2026-10-17

The follow-up separates ordered raw cursor reads (about 901 ms), the same query without ordering
(271 ms), parsing 100,000 resident timestamps (936 ms) and constructing rows from parsed Instants
(6 ms). The first change targets parsing: Room's shared Instant converter reads the common UTC
shapes written by `Instant.toString()` with `java.time` validation and nanosecond precision, retaining
`Instant.parse` for other forms and special/invalid input. No query, ordering, stored format, schema,
backup, cache or version changes are introduced.

Paired isolated R8 trials on the same HONOR reduce median first repository results at
1k/25k/100k activities from 23/500/2,093 ms to 8/125/1,071 ms. At 100k, cumulative allocations
fall from 146.88 to 18.45 MiB per load; the predefined 30% loading-time reduction criterion passes
with approximately 49%. Ordered raw reading still costs about 0.9 seconds. UI producer reloads
at 100k fall from about 2.6 to 1.3 seconds through state publication; frame/first-pixel latency and
the full UI peak remain unmeasured. Compatibility guards cover random round trips, nanoseconds,
legacy forms and invalid input, with a Room-to-statistics month-boundary regression.
The [performance checklist](docs/checklist-1.2.5-performance.md) records this bounded loading fix;
raw evidence remains ignored under `.audit/statistics-load/`. Prior UI retention work is preserved.
All 490 JVM tests and 265 physical Android tests pass with zero failures/errors/skips, alongside
Spotless, lint (zero errors, 51 existing warnings), debug/AndroidTest and standard optimized-release
builds. The normal APK excludes diagnostic code/assets; temporary packages are removed and the
original installations are preserved. No commit, push, PR or release is performed.
Other performance scenarios, final milestone audit and release gate remain open; v1.3.0 is not started.

## Statistics visibility and frame measurement — 2026-10-17

Thirty-three isolated R8 traces on the same HONOR/Android 13 at 60 Hz cover first entry,
cached reopening, background return, media scope and year selection with the previous synthetic
1k/25k/100k histories. At 100k, all three first results are directly correlated to display presentation:
median 1,998 ms from the Statistics click, range 1,984–2,033 ms. A second producer start near the
end of navigation appears in each trace and needs diagnosis. Cached first frames take about
85–89 ms, with navigation settled around 771 ms; scope/year result medians are 109/120 ms.

Jank remains: main-window FrameMetrics maxima are 115 ms on entry, 69 ms for scope and 77 ms
for year changes. The year popup surfaces separately record 10 dropped frames among 294 frames
over 18 openings. No measured main-window frame exceeds 700 ms. Vendor FrameTimeline end
timestamps are invalid in 30 packets; 39 timeline durations are incomplete. Known jank metadata
is retained and independent FrameMetrics has no callback loss, but trace completeness is partial.
Conservative next-frame bounds are labelled where direct content-token correlation is unavailable.

The [performance checklist](docs/checklist-1.2.5-performance.md) records the measurement and
remaining latency/jank work. Raw evidence stays ignored under `.audit/statistics-frames/`.
No product code changes or before/after frame improvement are claimed. Standard `spotlessCheck`
and `assembleRelease` pass again; the normal APK is byte-identical to the previous verified build.
Other performance scenarios, final milestone audit and release gate remain open; v1.3.0 is not started.

## Statistics navigation and filter correction — 2026-10-17

The transient navigation restart is reproduced with a failing ViewModel test: a consumer returning
inside the five-second grace period re-emits `true` and restarts `collectLatest`. Deduplicating the
effective subscription state after the timer preserves in-flight reads. The active statistics producer
also reuses one completed viewing aggregate while source/date/year/time zone remain unchanged;
All/Movies/Series only recomputes genre taste. No extra history list is retained. An exclusive lazy
Material 3 year-chip row replaces the window-backed year menu, preserving the complete year range.

The same 33 isolated R8/ART-speed scenarios on HONOR CMA-LX1 verify one producer start per cold
entry, zero restarts during cached navigation and zero watched-history calculations across 18 scope
changes. At 100k activities, first content presentation falls from median 1,998 to 1,013 ms
(890–1,074 ms after, three direct display correlations); scope response falls from 109 to 46 ms,
maximum 125 to 63 ms. Year response remains approximately 120 ms at 100k, but main-window year
frame P95 falls from 35.72 to 18.99 ms, maximum 77.30 to 34.37 ms. The popup surfaces responsible
for ten earlier dropped frames are absent. Remaining scope/render and foreground-first-frame jank
is recorded explicitly: scope frame P95 only changes 24.53 to 23.75 ms, maximum stays about 69 ms,
and scope deadline misses do not improve. Navigation animation duration is unchanged. This is a
targeted correction, not a claim that every frame now meets its deadline.

Fresh verification passes 493 JVM tests, 20 native Statistics/navigation tests, the new year selector
again in Italian, Spotless, lint (zero errors, 51 existing warnings), debug/AndroidTest/release builds
and the scoped Ponytail review. The normal optimized APK contains no diagnostic components; owned
temporary packages are absent and protected-package inventory matches the initial read. FrameTimeline
remains partial (29 invalid vendor timestamps, 47 incomplete durations); independent FrameMetrics
loses no callbacks. The [performance checklist](docs/checklist-1.2.5-performance.md) records conditions,
comparison windows, conservative bounds and residual limitations. Raw evidence stays ignored under
`.audit/statistics-navigation-fix/`. Other performance work and the v1.2.5 release gate remain open;
v1.3.0 is not started. No commit, push, PR or release is performed.

## Statistics first-filter shader stall correction — 2026-10-17

The remaining All/Movies/Series stalls are traced from Main waiting in `postAndWait` to RenderThread
compiling platform-ripple `CircleOp` shaders for 41–49 ms. A same-APK ON/OFF/OFF/ON isolation with
fresh private synthetic app data reproduces roughly 66 ms maximum frames with the ripple and
24–26 ms without it. The correction keeps Material segmented-button layout, selection and input
semantics, replacing only that selector's ripple with an animated solid press/focus/hover state layer
using theme colors and Material 3 opacities. No global ripple override, warm-up, dependency change
or device setting is introduced.

Four paired first-use runs per version (1k/25k/100k activities plus a second 1k run, six selections
each) verify the optimized release on the same HONOR. Private app data is reset before each run,
so this baseline differs from the earlier cross-fixture warm-shader scenario. In identical 250 ms
post-callback windows, frame P95 falls 53.32 → 18.88 ms, maximum 86.70 → 26.41 ms and frames over
50 ms fall 22 → 0. FrameMetrics deadline misses fall 60/347 → 21/343; the first-three-selection P95
falls 62.02 → 19.83 ms and the warm-selection P95 22.16 → 17.96 ms. Median callback-to-display
latency falls 70.98 → 44.40 ms. No `CircleOp` shader compilation occurs during the corrected filter
changes. Some frames still exceed a 60 Hz budget; the smaller initial texture shader cost and the
known partial vendor FrameTimeline data remain explicit limitations, not a claim of perfect fluidity.

Fresh verification passes 493 JVM tests, 22 native Statistics/navigation tests, three additional
Italian cases, Spotless, lint (zero errors, 51 existing warnings), standard debug/AndroidTest/release
builds and scoped Ponytail review. Press cancellation, visible press/focus feedback, keyboard selection
and both themes are covered. Resting content pixels in the compared screenshot region are unchanged.
The normal APK contains no probes or A/B control; owned packages are absent and protected inventory
matches the first read. Canonical behavior is recorded in [architecture](docs/architecture.md), with
conditions and residual jank in the [performance checklist](docs/checklist-1.2.5-performance.md).
Raw evidence stays ignored under `.audit/statistics-scope-jank/` and `.audit/statistics-scope-final/`.
v1.2.5 remains in progress, v1.3.0 is not started, and no commit, push, PR or release is performed.

## Statistics residual frame-budget reduction — 2026-10-18

The selected Material check now appears immediately, avoiding a first-use texture shader, and the
radar Canvas isolates its recorded drawing commands from the button animation. Native segmented-button
background, shape, border, selection and input stay intact; tint and label placement still animate.
No additional history, bitmap cache, provider call, dependency or persistence change is introduced.

A new same-APK paired baseline uses populated six/three-axis radars with 1k/25k/100k episode activities
and 20 watched movies, fresh private synthetic data and 24 selections per version. On the HONOR at
60 Hz, frames above the fixed 16.666667 ms budget fall 20/341 → 16/319, P95 17.49 → 16.56 ms and
maximum 27.26 → 25.80 ms. FrameMetrics deadline misses fall 14 → 10; drawing the radar falls
288 → 24 times, and first-use shader compilations fall four → zero. The gain is modest and variable:
warm-selection over-budget frames are 8 → 9, so uniform smoothness is not claimed. The fixed budget
is distinct from Android's sometimes longer deadline, and vendor FrameTimeline remains partial.

Conditions, controls and limitations are in the [performance checklist](docs/checklist-1.2.5-performance.md);
raw evidence stays ignored in `.audit/statistics-frame-budget/`. v1.2.5 remains in progress and
v1.3.0 is not started. No commit, push, PR or release is performed.

Fresh verification passes 493 JVM tests, 24 native Statistics/navigation tests and five additional
Italian cases, Spotless, lint (zero errors, 51 existing warnings), standard debug/AndroidTest/release
builds and scoped Ponytail review. Rapid changes preserve exclusive selection and actual radar pixels
in both themes. Normal APKs contain no probes or fixtures; temporary packages are absent and the
protected device inventory is unchanged. Other v1.2.5 measurements and the overall release gate remain open.

## Statistics warm-filter investigation — 2026-10-18

Same-APK release trials with eight warm-up selections before each measurement do not establish an
additional frame-budget improvement. Button recomposition isolation removes unrelated work but changes
over-budget frames 15/315 → 18/320; a raster layer reduces repeated path operations but changes
20/317 → 21/322 and adds about 1.4 MiB of reported GPU memory. Neither is adopted. A single geometry-cache
pair is insufficient evidence and still rebuilds on every data change. Component-duration probes with
1k/100k activities exclude viewing-summary recomposition and history recalculation during warm toggles.
The existing production code and animation remain intact, as requested by the owner. The warm-filter
performance item stays open; this is an investigation result, not a completed optimization or a hardware
limit claim. Conditions and limitations are in the [performance checklist](docs/checklist-1.2.5-performance.md);
temporary evidence stays ignored in `.audit/statistics-warm-filters/`. v1.2.5 remains in progress.

## Public branding and bounded core checks — 2026-10-18

The public name is now `bingee` in both English and Italian: launcher, personal destination,
onboarding, privacy, backup errors and About/update text. Current public documentation follows the
same spelling; technical identifiers, repository URLs and historical records retain their spelling.
The notification small icon now reuses the approved geometric launcher mark instead of the old bitmap.

A targeted Room regression removes movies from the Continue Watching projection before mapping:
100 movies plus one series produce one row instead of 101, with the next episode and all collection
entries preserved, including a same-ID Movie/Series pair. Three continuation consumers share one query
per day. A new Profile test verifies one producer with multiple UI consumers and cancellation five
seconds after the last consumer leaves. Widget logic checks remain distinct from real-launcher
placement/rendering and minified callback coverage. No timing, frame or battery improvement is claimed.
The [performance checklist](docs/checklist-1.2.5-performance.md) retains the remaining physical matrices
and large-library measurements. Evidence stays ignored in `.audit/branding-and-core-validation/`.
The final host gate passes Spotless, all 494 JVM tests (including 13 widget tests), lint with zero
errors and 37 warnings, plus standard debug/AndroidTest/release builds. Physical HONOR
runs pass 40 selected English tests and 15 Italian tests. Standard APK identities and lowercase
launcher labels are verified; temporary verification packages are absent and protected inventory is
unchanged. No commit, push or publication is performed.

v1.2.5 remains in progress; the warm-filter animation and its open performance item are unchanged.

## Physical widgets and large-library query verification — 2026-10-18

Actual HONOR launcher checks cover pin/remove/re-add without process restart, one updater owner,
zero observations after last removal, hidden/visible metadata changes, both minified progress callbacks,
cached Movie/Series detail intents, empty states, fixed Light/Dark, visible EN/IT text, local poster
replacement/fallback and a real 4×2-to-4×3 resize. Injected day rollover affects only the upcoming type;
the phone clock is unchanged. Two P3 findings remain: Italian next-episode numbers are ellipsized in the
large widget at 360dp/font scale 1.0, and a probe-controlled hot EN→IT switch can retain an English
episode-button content description.

Nine 100/1,000/10,000-title fixtures plus repeated large cases measure current SQL against the same
statement without the existing SERIES predicate, on an isolated R8/ART-speed release. All 288 accepted
raw reads preserve series content; 60 separate Room samples validate generated-query materialization.
Three consumers share one query initially and per metadata invalidation. At 10,000 titles, aggregated
raw medians are 407.74 → 190.75 ms for 90% movies, 1,701.18 → 965.10 ms for mixed libraries and
3,753.08 → 3,159.10 ms for 90% completed series. The latter still materializes 9,000 ineligible rows
before returning zero domain items, so this optimization opportunity remains open. These are warm-cache
query/cursor timings, not complete screen, frame or battery measurements; conditions and limits are in
the [performance checklist](docs/checklist-1.2.5-performance.md).

Stale generated release Room output was detected and rejected before accepting measurements. Fresh
KSP generation aligns all three measured DAO statements with current source, and both real widget
callbacks are repeated successfully on that artifact. The normal host gate and normal APK identities
are verified; unchanged-source JVM/lint results remain valid. No production Kotlin/resource/schema or
dependency change, commit or publication. The owned package and widgets are removed; all ten initial
widget bindings and protected packages are preserved. Evidence stays ignored in
`.audit/widget-launcher-db/`. The two widget P3 findings, remaining ineligible-series work and overall
v1.2.5 release gate remain open.

## Widget fixes and caught-up-series query reduction — 2026-10-18

The subsequent correction closes both widget findings above: the next-episode text wraps, and both
widget snapshots/updater observe portable app language. A localized resource context and language-keyed
composition refresh visible text and accessible episode actions together. Actual HONOR launcher checks
at 360dp/font 1.0 verify Light/Dark numbers, hot EN/IT descriptions on both types without process restart,
preference-only language updates, both minified progress callbacks and empty states after completion.

Continue Watching now requires an unwatched, available regular episode before its detailed SQL
projection. Domain eligibility/sorting, full Library progress, incomplete-cache coverage and backup
semantics remain unchanged. Same-APK counterbalanced comparisons across nine fixtures and three large
repetitions preserve every retained row: 288 raw reads and 60 separate Room materializations. At 10,000
titles/270,000 episodes, the 90%-completed-series case drops from 3,174.20 to 224.56 ms raw (92.9%),
and returns zero unnecessary rows. Current Room takes 225.50 ms. Movie-heavy and mixed libraries incur
approximately 3.4% extra raw-query cost; no universal or end-to-end UI speedup is claimed.

The regressions fail before and pass after. All 495 JVM and 63 physical Android repository/Room/backup/
migration tests pass, alongside Spotless, lint (zero errors/38 warnings) and normal debug/AndroidTest/
release builds. Generated Room SQL matches current source; normal APKs contain no diagnostic overlay.
Ponytail review finds no actionable issue. Only owned test packages/widgets are removed; the initial
package inventory and ten protected widget bindings remain intact. No schema/dependency/version change,
commit or publication. Remaining query-selection work and the overall v1.2.5 gate stay open; method,
tradeoffs and limits are in the [performance checklist](docs/checklist-1.2.5-performance.md), with temporary
evidence under ignored `.audit/widget-fixes/`.

## Physical Profile observer lifecycle verification — 2026-10-18

The actual HONOR navigation path now verifies Your bingee, Collection, Movie Details, background and
return, plus quick returns, three longer background cycles and Statistics/dashboard owner reuse.
Passive counters never collect UI state. Each Profile owner has at most one producer per source;
separate navigation owners can overlap during the grace period, while the shared progress query
stays unique. Profile producers stop after 5,047–5,073 ms without consumers; the shared progress
query stops after its own additional grace period (10,091–10,107 ms total) when no other owner exists.
Cached state survives; a synthetic background write is reflected in Collection, Statistics and the
dashboard without process restart or clearing the ready/loading state.

Home and Movie Details retain their existing ViewModel-scoped observations while in the back stack;
Details releases them when popped. This is recorded as a pre-existing conditional P2 opportunity,
without claiming measured CPU/battery harm or implicitly authorizing another lifecycle rewrite.
The physical Profile lifecycle verification item is closed. No production file changed; normal APK
hashes and the applicable 495-JVM/63-Android gate remain unchanged. The owned diagnostic package is
removed and protected packages/ten widget bindings are preserved. Evidence stays ignored under
`.audit/observer-lifecycle/`; the overall milestone/release gate remains open.

# v1.2.6 checklist — Audit and maintenance (milestone open)

- [x] Owner requested the v1.2.6 investigation; application code 10 remains planned, not applied.
- [x] Run the deferred read-only audit and distinguish application dead code, index candidates and optional improvements; no automatic cleanup is authorized.
- [x] Present optimization directions with benefits, costs and stop conditions; the owner selected retaining the current image-cache configuration, with no additional implementation selected.
- [x] Preserve completed v1.2.5 optimizations throughout the approved investigation and documentation changes.
- [x] Complete the isolated image-cache investigation and record the owner's acceptance; reports and tools remain under ignored `.audit/image-cache-126/`. Further measurements await a concrete problem, and proposed thresholds remain unapproved hypotheses.
- [ ] Verify the remaining v1.2.6 milestone closure criteria and selected-scope checks before declaring the whole milestone complete.

No dead-code/over-engineering fix is currently selected; optional findings do not create an
implementation gate. The three index candidates are deferred to a migration required for other
reasons, under the Android verification conditions above, and do not block this milestone.

# v1.3.0 checklist — Details

- [ ] Bump to v1.3.0 / planned code 11.
- [ ] Preserve hero/backdrop/poster/collapsing chrome direction.
- [ ] Design and implement compact personal summary.
- [ ] Movie summary communicates watched/date/rating/membership appropriately.
- [ ] Series summary exposes explicit personal relationship state.
- [ ] Derive “In pari” without unnecessary persistence state.
- [ ] Reuse canonical continuation policy for next episode.
- [ ] Add next-actionable-episode context.
- [ ] Compact rating editing behind explicit intent.
- [ ] Preserve 1–10 rating persistence/chronology.
- [ ] Make Movie date behavior explicit and truthful.
- [ ] Show Series completion date only when meaningful.
- [ ] Move Abandoned/Resume to title-level personal relationship area.
- [ ] Improve season scan hierarchy.
- [ ] Improve episode long-text allocation.
- [ ] Preserve Specials behavior.
- [ ] Preserve stale/cache/error states.
- [ ] Clarify title refresh vs season refresh scope.
- [ ] Run full Details state matrix.
- [ ] Run mandatory GLOBAL audit after each numbered modification.
- [ ] Remove any old Details UI paths made dead by redesign before release.
- [ ] Run final v1.3.0 global audit.
- [ ] Update canonical docs for new Details IA.
- [ ] Pass release gate.

# v1.3.1 checklist — Home

- [ ] Bump to v1.3.1 / planned code 12.
- [ ] Make personal continuation the primary Home task.
- [ ] Reorder or restructure Home sections accordingly.
- [ ] Preserve actionable Continue Watching policy.
- [ ] Preserve calendar value and clarify its role.
- [ ] Deliberately resolve Abandoned-series calendar inclusion policy.
- [ ] Keep Featured secondary and non-personalized.
- [ ] Improve personal empty states.
- [ ] Preserve local-first/cached behavior.
- [ ] Verify Home with empty, populated, stale and no-credential states.
- [ ] Run mandatory GLOBAL audit after each numbered modification.
- [ ] Remove obsolete Home layout/control paths made dead by redesign.
- [ ] Run final v1.3.1 global audit.
- [ ] Update canonical docs.
- [ ] Pass release gate.

# v1.3.2 checklist — Collection

- [ ] Bump to v1.3.2 / planned code 13.
- [ ] Make active Collection scope explicit.
- [ ] Remove impossible media-type/scope combinations.
- [ ] Clarify Collection-local search scope.
- [ ] Decide whether broader “All personal titles” scope is needed; do not implement without owner approval.
- [ ] Align essential Grid/List management capabilities.
- [ ] Make sort labels match actual chronology per scope.
- [ ] Improve filter discoverability.
- [ ] Keep deep tracking/rating ownership in Details.
- [ ] Verify every Collection scope in grid/list and empty/populated states.
- [ ] Run mandatory GLOBAL audit after each numbered modification.
- [ ] Remove obsolete Collection paths/components made dead by redesign.
- [ ] Run final v1.3.2 global audit.
- [ ] Update canonical docs.
- [ ] Pass release gate.

# v1.3.3 checklist — Cross-screen design language

- [ ] Bump to v1.3.3 / planned code 14.
- [ ] Define explicit top-bar/chrome families.
- [ ] Align screens to those families.
- [ ] Preserve justified Details and dashboard exceptions.
- [ ] Standardize Favorite toggle meaning.
- [ ] Standardize primary/secondary/reversible/destructive action hierarchy.
- [ ] Standardize blocking/recoverable/stale/empty/loading expectations.
- [ ] Align section heading/helper-copy conventions.
- [ ] Review release-center name and scope.
- [ ] Rename only with owner approval.
- [ ] Align Settings index/subpage chrome.
- [ ] Run full terminology audit across Collection/Library/Watch Later/Watching/Watched/In pari/Abandoned/Favorite/releases.
- [ ] Keep domain persistence names unchanged unless actual domain semantics change.
- [ ] Run mandatory GLOBAL audit after each numbered modification.
- [ ] Remove obsolete shared/local styling paths made dead by alignment.
- [ ] Run final v1.3.3 global audit.
- [ ] Update canonical design/product docs.
- [ ] Pass release gate.

# v1.3.4 checklist — Responsive & accessibility hardening

- [ ] Bump to v1.3.4 / planned code 15.
- [ ] Execute full width/font/locale/theme matrix.
- [ ] Cover all major screens and key dialogs/states.
- [ ] Fix any remaining fixed-width text starvation.
- [ ] Fix any remaining non-wrapping action failure.
- [ ] Validate hero and episode layouts at 320/360dp.
- [ ] Validate 600dp remaining-content width behavior.
- [ ] Add max-width constraints only where evidence justifies them.
- [ ] Audit global semantics.
- [ ] Remove duplicate decorative announcements.
- [ ] Validate selected/disabled/toggle states.
- [ ] Validate chart/progress semantics.
- [ ] Perform bounded manual TalkBack pass where practical.
- [ ] Validate Light/Dark contrast and system bars.
- [ ] Run final whole-app Impeccable audit.
- [ ] Run final whole-repo Ponytail audit.
- [ ] Remove UI paths/styles/components made dead by the completed v1.3.x redesign cycle.
- [ ] Perform documentation sync for completed UI system.
- [ ] Pass release gate.

---

# D. Rules for audit findings discovered during development

## If the audit finds a P0

- Stop the current roadmap item.
- Fix before continuing.
- Re-run relevant focused and global verification.

## If the audit finds a P1 introduced by the current change

- Fix in the current version before marking the item complete.

## If the audit finds newly introduced dead code or over-engineering

- Remove/simplify it in the current version.
- Do not defer self-created debt.

## If the audit finds pre-existing P1 unrelated to the current change

- Report it immediately.
- Owner decides whether it interrupts the roadmap.

## If the audit finds pre-existing P2/P3

- Record it under a specific future version or `DEFER`.
- Do not silently enlarge scope.

## If the audit suggests a new abstraction

Before implementing, answer:

1. What current duplicated invariant does it own?
2. How many real consumers need it today?
3. Does it reduce conceptual complexity now?
4. Could a pure function or existing model solve the problem instead?
5. Is it being created for a hypothetical future feature?

If #3 is no or #5 is yes, do not add it.

---

# E. Definition of done for the roadmap cycle

The v1.3.4 cycle is complete only when:

- [ ] all v1.2.3–v1.3.4 roadmap items are either completed or explicitly re-scoped by the owner;
- [ ] no known P0/P1 release blocker remains;
- [ ] Room/Backup versions reflect only real persistence changes;
- [ ] global audits show no material newly accumulated dead code;
- [ ] canonical business rules remain centralized or equivalence-tested;
- [ ] Bingee retains a clear private-personal-tracker identity;
- [ ] major screens share a coherent visual language without unjustified uniformity;
- [ ] Details clearly communicates personal relationship state;
- [ ] Home prioritizes current personal activity;
- [ ] Collection clearly communicates its management scope;
- [ ] responsive behavior remains usable at narrow widths and large fonts;
- [ ] accessibility semantics are complete and non-redundant;
- [ ] canonical documentation matches the real codebase;
- [ ] generated audit reports remain outside canonical documentation;
- [ ] full verification is green.
