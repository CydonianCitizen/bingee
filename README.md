# bingee

bingee is an early-stage, open-source Android app for tracking films and TV series. Its core promise is local-first ownership: a user's history must remain usable without an account, a proprietary backend, or continued service availability.

## Project status

The code now targets bingee `1.3.0`/code 11; Details verification remains in progress; the approved horizontal History timeline is implemented with scoped automated and visual verification, while owner acceptance and a new manual TalkBack check remain pending, and the milestone remains open. The agreed v1.2.5 and v1.2.6 scopes remain closed; publication is a separate explicit action. See the [v1.3.0 implementation notes](docs/release-notes-1.3.0.md) for the changed Details/History behavior and remaining checks. It contains a local-first Compose app with Home, Search, the Your bingee personal dashboard, Continue Watching with one-tap episode tracking, two home screen widgets, Notification Center, settings subpages, secure TMDB credential management, Room v9 details and progress, a Room-first release calendar, approximate local notifications, versioned JSON backup/restore (v3 export, v1/v2/v3 import), an additive importer for documented TV Time JSON ZIP profiles, English/Italian localization, and a manual GitHub update checker. V3 carries theme, language, spoiler visibility, and six collection display modes; it rejects exports above the 50 MiB restore ceiling and aligns current episode/season limits with TV Time. TMDB is the only runtime media provider; animated and anime content is handled as ordinary TMDB Movies or TV Series. Published releases are listed in [GitHub releases](https://github.com/CydonianCitizen/Bingee/releases).

Your bingee → Your history shows a continuous horizontal timeline of retained
watched Movies or Episodes, oldest first, with dated points alternating above/below the rail and
expandable Series/day groups. Compact cards centre dates near their points; filters are Movies
and Series, initially Movies. The combined view is hidden and episodes expand within their card.
Automatic bounded loading crosses months and years; regular spacing is not a time scale.
A Movie's manual watched date also
determines its month in Statistics. Specials appear in History but remain excluded from the current
statistics. Unwatch removes an item; repeat viewings and the original viewing time zone are not
reconstructed. The [plan](docs/superpowers/plans/2026-10-20-history-timeline.md) distinguishes the
previous monthly H1–H6 evidence from fresh horizontal-timeline verification; v1.3.0 remains open.


Remote metadata uses a user-supplied TMDB API Read Access Token. It is optional for opening the local shell. Debug fakes are architectural fixtures and are not wired into production navigation.

## Current stack

- Kotlin with Jetpack Compose and Material 3
- Single-activity Navigation Compose
- Hilt dependency injection
- Room for local persistence
- Retrofit and OkHttp for provider access
- Coil for constrained poster loading and caching
- WorkManager for deferrable background refresh
- Jetpack Glance for home screen widgets
- Coroutines and Flow as features are added
- JVM unit tests, with focused Android instrumentation tests where they add value

The checked-in Gradle Wrapper and version catalog are the source of truth for build and dependency versions.

## Prerequisites

- Git
- JDK 21
- Android Studio or Android command-line tools
- Android SDK Platform 36.1

Android Studio normally creates a local, ignored `local.properties` file that points to the Android SDK. Never commit this file.

## Build and verify

Clone the repository, then run these commands from its root:

```bash
./gradlew tasks
./gradlew spotlessCheck
./gradlew test
./gradlew lint
./gradlew assembleDebug
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

Instrumented component tests resolve controls from string/plural resources and existing semantics.
Run their screen suites in Italian on a connected emulator or device with:

```bash
./gradlew connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.package=com.cydoniancitizen.bingee.feature,com.cydoniancitizen.bingee.app,com.cydoniancitizen.bingee.core" "-Pandroid.testInstrumentationRunnerArguments.uiLocale=it"
```

`uiLocale` defaults to `en`. The test locale rule runs before the Compose Activity, so dialogs and
menus use the same language; it restores the previous Android app locale after each test and does
not change saved bingee preferences. Activity navigation tests still exercise the app's own language setting.
Tests that verify wording or grammar retain explicit expectations; fixture titles are not translated.

To apply Kotlin formatting:

```bash
./gradlew spotlessApply
```

The debug APK is generated under `app/build/outputs/apk/debug/`.

### Optimized release smoke

Build the actual optimized variant before testing release behavior:

```bash
./gradlew :app:assembleRelease
```

The APK is generated under `app/build/outputs/apk/release/`. The repository does not configure
release signing. Sign a copy locally for installation; keep certificates, passwords and signed
test artifacts outside Git. A locally signed test copy is not a published release.

Run the installed release through cold launch, TMDB credential validation, Movie/TV Search and
Details, library add/remove, Favorites, movie/episode progress, Your bingee, Statistics and Settings.
Verify notification detail intents on cold and warm launches. Exercise all three launcher shortcuts,
including Watching/Watch Later → Statistics → Back. Place both widgets on a real launcher host;
verify rendering, title navigation and the next-episode action. Change progress and theme while
their Glance sessions are still active and check that both widgets update, then relaunch offline and confirm
saved state. Use synthetic local provider fixtures for automated checks; debug fakes and
`connectedDebugAndroidTest` do not establish minified runtime coverage.

Check app logs for fatal exceptions, class-loading/verification errors and Gson parsing failures.
Retain the tested APK hash, R8 mapping, device/API, observed results and limitations in ignored
`.audit/` evidence. Add keep rules only for a reproduced release failure.

For the widget regression, also probe the optimized DEX with the SDK's `apkanalyzer`:

```bash
apkanalyzer dex code --class com.cydoniancitizen.bingee.feature.widget.MarkNextEpisodeAction --method '<init>()V' app/build/outputs/apk/release/app-release-unsigned.apk
```

In PowerShell, the `.bat` launcher needs `--method '"<init>()V"'` to protect the angle brackets.
The command must resolve a public no-argument constructor. Glance instantiates this callback by
reflection; its constructor carries `@Keep` because R8 otherwise removes it. The DEX probe
supplements the real widget tap and persisted episode-count check; it does not replace them.

An active Glance composition collects the Room/date/theme snapshot; `updateAll` also wakes idle
widgets. Poster loading follows URL changes, so progress/theme updates keep the existing bitmap.

## Run

Open the repository in Android Studio, choose an emulator or connected device, and run the `app` configuration. From the command line, a configured device can install the debug build with:

```bash
./gradlew installDebug
```

On first run without a configured credential, bingee offers TMDB setup or offline continuation. Bottom navigation exposes Home, Search, and Your bingee; Your bingee opens Settings, which indexes the Appearance & Language, Notifications, Data & backup, Privacy, and About subpages and offers a visible Up action back to the dashboard. Search offers explicit Movies and TV Series categories, debounces input, loads additional TMDB pages, and adds or removes results from the local collection. Search and collection rows open title details using provider-qualified identity. Cached textual details, loaded seasons, episodes, watch progress, and personal ratings render from Room and remain usable offline or after credential removal. Episode/movie progress and 1–10 title ratings are editable without network or collection membership. Your bingee reads only Room. It shows actionable Watching, collection shortcuts, Favorites, and a personal statistics preview, and its collection views support local title search, media/watch-state filters, and recently-added, title, progress, or rating sort. Full statistics add a taste radar, a genre ranking across All/Movies/Series, exact viewing analytics, a monthly viewing histogram, and a ratings histogram with selected-result shelves. Home reads cached movie releases, season premieres, and episode air dates only from Room. Continue Watching follows the featured rows; each series card shows the last watched and next episode and marks the next one watched in one tap, with undo. Opening Home also runs one bounded featured-discovery pass against TMDB, at most one movie and one TV `discover` page, kept in memory only and skipped without a stored credential; the cached release calendar itself refreshes only on explicit user action or the periodic worker. Notification Center reads the cached release events; the About page offers a manual GitHub update check.

Two home screen widgets read the same Room data and never call TMDB. The 2×2 widget shows the poster of the series in progress with a button that marks its next episode watched; the 4×2 widget pairs that series with the next two releases from the cached calendar. Both follow the in-app theme and language, including accessible button descriptions. The updater observes data only while widgets exist and redraws for relevant displayed-content or configuration changes; the small widget does not observe the calendar.

The [v1.2.3 changes](docs/release-notes-1.2.3.md) clarify collection membership in Search and Home without changing watch state. Favorite toggles share the same selected meaning, date actions wrap on narrow screens, and Details artwork leaves title identification to the visible heading. Watching and Watch Later launcher shortcuts can open Statistics directly; Back or Up returns to Collection, then Home.

The implemented [v1.2.4 changes](docs/release-notes-1.2.4.md) keep active widgets current, fix changed-title notification/widget navigation, expose recoverable local Home read errors, preserve shared progress observations through transient failures, and size the Statistics radar from its own container. UI tests use localized resources and semantics. Its [historical checklist](BINGEE_DEVELOPMENT_ROADMAP_AND_CHECKLIST.md#v124-checklist) records that delivery; selected hardware-efficiency follow-up and the final closure decision are recorded under v1.2.5 below.

The [v1.2.5 implementation notes](docs/release-notes-1.2.5.md) cover C01–C07 cleanup, Statistics loading and filter fixes, widget localization, cheaper complete-series queries, compact backups and abandoned import-ZIP cleanup. Final verification and all 276 Android tests passed on HONOR / Android 13, with a separate Italian backup-screen check. The [v1.2.5 checklist](BINGEE_DEVELOPMENT_ROADMAP_AND_CHECKLIST.md#v125-checklist) records closure, accepted limits and the owner's decision to defer the next global audit to v1.2.6; completed optimizations are not reopened.

WorkManager maintains a bounded batch of up to 20 followed titles approximately once per day when network is available, rotating through the least recently attempted titles so that ones TMDB keeps rejecting cannot hold back the rest. A separate network-free worker evaluates cached Room events for optional local notifications. Notifications are disabled by default; Settings requests Android notification permission only after the user enables them and supports same-day, one-day, three-day, or seven-day lead times plus movie, season, and episode categories. Android may delay work because of Doze, battery optimization, constraints, or device policy; bingee promises no exact notification time.

Your bingee → Settings → Data & backup emits backup v3, including media history, watch progress, ratings, portable preferences, and ordered genre metadata. Restore accepts v1/v2/v3 and validates the complete file before one Room transaction. Theme, language, spoiler visibility, and the six collection display modes travel with the backup; Android notification permission and enablement and credentials stay device-specific. Legacy v1/v2 files retain their 500,000-episode reader ceiling; v3 exports and TV Time use the current 100,000-episode ceiling.

Your bingee → Settings → Data & backup also exposes an experimental `Import TV Time history` action. It supports only the role-based JSON ZIP profile derived from evidence ID `TVTIME-SAMPLE-001`. The archive is inspected locally with bounded ZIP limits, then matched conservatively through the existing TMDB credential. Ambiguous records require review or skip; confirmation applies additive, idempotent changes only. Ratings, favorites, custom lists, rewatch counters/timelines, CSV, other TV Time variants, TV Time authentication, and TV Time network access are unsupported.

## TMDB configuration and privacy

bingee supports one TMDB credential format: the API Read Access Token available from the API section of a TMDB account. The app trims surrounding whitespace, checks Bearer-token structure locally, and validates the candidate with TMDB's `GET /3/authentication` endpoint. Only a remotely accepted candidate is saved.

The accepted token is encrypted with AES-256-GCM using key material held by Android Keystore. Ciphertext is stored in `noBackupFilesDir`, separate from ordinary Preferences DataStore settings and excluded from cloud backup and device transfer. The token is never included in bingee data exports. Startup trusts a previously validated stored token and does not perform automatic remote validation.

bingee has no account or proprietary backend. Without a usable TMDB credential, the application shell and saved local data remain available while remote metadata features stay disabled. Search reads the credential only inside the protected data/network boundary; query text is not logged or stored. See [privacy notes](docs/privacy.md).

This product uses the TMDB API but is not endorsed or certified by TMDB. The official TMDB attribution logo is shown in Your bingee → Settings/About. Credentials are never part of JSON backup files.

## Architecture

bingee starts as a single Gradle application module and a lightweight modular monolith. Packages are grouped by responsibility:

```text
app/src/main/java/com/cydoniancitizen/bingee/
  app/        application shell
  core/       domain models, results, navigation, and shared UI
  data/       Room persistence, calendar projection, secure credential storage, settings, and isolated TMDB networking
  domain/     repository contracts, refresh coordination, and local notification policy
  feature/    onboarding, settings, search, profile, details, Home calendar, home screen widgets, and feature UI

app/src/debug/java/com/cydoniancitizen/bingee/
  debug/      deterministic repository fixtures
  feature/    deterministic Search, Profile, Details, Home, and Settings state previews
```

Feature UI depends on immutable domain models and repository contracts, never provider DTOs or Room entities. TMDB client, DTOs, mappers, errors, and identities remain isolated. Japanese animation returned by TMDB behaves identically to standard Movies and TV Series. Account sync, recommendations, and automatic merging are absent.

## Versioning

The project follows Semantic Versioning. The current repository version is `1.2.5` with `versionCode` 9, as defined in [app/build.gradle.kts](app/build.gradle.kts). Room schema 9 and Backup format 3 have independent versions. Completion of the agreed milestone scope does not announce a published release; historical milestone documents retain their original version numbers.

Maintenance milestone v1.2.6 is complete within the agreed scope as of 2026-10-19, before the larger v1.3 UI cycle. Its image-cache investigation is complete: the user accepted keeping the current configuration because the tested Android device and 16-title dataset did not demonstrate a useful benefit from reducing the limit. This result does not extend to large libraries or prolonged use. Further measurements are deferred until a concrete problem emerges; proposed thresholds remain hypotheses, not approved requirements. The three database indices remain candidates for validation at the next migration required for other reasons. No additional implementation is selected; the application remains 1.2.5/code 9, with no publication implied. See the [current roadmap](docs/roadmap.md).

## Contributing

Read [CONTRIBUTING.md](CONTRIBUTING.md) before changing the project. Keep pull requests small, scoped, tested, and free of credentials or machine-local files.

Security reports follow [SECURITY.md](SECURITY.md).

## License

bingee is licensed under the [Apache License 2.0](LICENSE).

The app bundles two fonts under the SIL Open Font License 1.1, taken from the [google/fonts](https://github.com/google/fonts) repository: Inter (`app/src/main/res/font/inter.ttf`, from `ofl/inter/Inter[opsz,wght].ttf`) and Oswald (`app/src/main/res/font/oswald.ttf`, from `ofl/oswald/Oswald[wght].ttf`). Their copyright notices and full license texts are in [`app/src/main/assets/licenses/`](app/src/main/assets/licenses/) and ship inside the APK.
