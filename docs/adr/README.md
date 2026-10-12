# Architecture decision index

This directory contains the durable architecture decisions for Bingee. The newest applicable ADR takes precedence over older exploratory notes.

To add an ADR, copy `0000-template.md`, use the next available four-digit number, and choose one of: Proposed, Accepted, Superseded, or Rejected. Keep accepted ADRs immutable; supersede them with a new ADR instead of rewriting history.

The current application is described by [architecture](../architecture.md): TMDB-only runtime,
Room 9 and Backup v3 with v1/v2/v3 restore. ADRs 0020–0022 are superseded; their Jikan/anime/link
decisions remain historical. Older Room numbers in ADR titles identify the decision's original
milestone and are not the current database version. The export in `app/schemas/` is authoritative.

## Index

| ADR | Topic |
| --- | --- |
| [0001](0001-local-first-architecture.md) | Local-first architecture |
| [0002](0002-initial-android-technology-stack.md) | Initial Android technology stack |
| [0003](0003-provisional-repository-structure.md) | Package-structured repository |
| [0004](0004-apache-2-license.md) | Apache License 2.0 |
| [0005](0005-domain-boundaries-contracts-and-identity.md) | Domain boundaries and provider-qualified identity |
| [0006](0006-result-errors-and-screen-state.md) | Result errors and screen state |
| [0007](0007-centralized-navigation.md) | Centralized Navigation Compose |
| [0008](0008-debug-fakes.md) | Debug fakes |
| [0009](0009-tmdb-credential-configuration.md) | TMDB credential configuration |
| [0010](0010-tmdb-search.md) | TMDB search |
| [0011](0011-room-v1-local-library.md) | Room v1 local Library |
| [0012](0012-cache-first-tmdb-details-room-v2.md) | Cache-first TMDB details and Room v2 |
| [0013](0013-room-v3-seasons-episodes-progress.md) | Room v3 seasons, episodes, and progress |
| [0014](0014-room-v4-ratings-library-organization.md) | Room v4 ratings and Library organization |
| [0015](0015-room-v5-local-release-calendar.md) | Room v5 local release calendar |
| [0016](0016-workmanager-release-notifications-room-v6.md) | WorkManager release notifications and Room v6 |
| [0017](0017-versioned-json-backup-and-transactional-restore.md) | Versioned JSON backup and transactional restore |
| [0018](0018-tv-time-import-evidence-and-boundaries.md) | TV Time import evidence and boundaries |
| [0019](0019-tv-time-import-implementation.md) | TV Time JSON ZIP import implementation |
| [0020](0020-multi-provider-search-ux.md) | Multi-provider Search UX — superseded |
| [0021](0021-anime-entry-relations-and-progress.md) | Anime entry, relations, and progress representation — superseded |
| [0022](0022-assisted-cross-provider-equivalence-and-reversible-media-links.md) | Assisted cross-provider equivalence and reversible media links — superseded |
| [0023](0023-multiplatform-readiness-without-premature-android-migration.md) | Multiplatform readiness without premature Android migration |
| [0024](0024-stable-tmdb-genre-identity-room-v3.md) | Stable TMDB genre identity and Room v3 |
| [0025](0025-favorite-chronology-for-dashboard.md) | Favorite chronology for personal dashboard ordering |
| [0026](0026-restore-genre-statistics-and-portability.md) | Restore canonical genre statistics and portable genre metadata |
| [0027](0027-home-screen-widgets.md) | Home screen widgets with Glance |
| [0028](0028-type-aware-tmdb-identity-room-v5.md) | Type-aware TMDB identity and Room v5 |
| [0029](0029-restore-safe-coverage-cache-language-refresh-rotation-room-v6.md) | Restore-safe progress coverage, cache language, refresh rotation, and Room v6 |
| [0030](0030-portable-empty-season-evidence-room-v7.md) | Portable empty-season evidence and Room v7 |
| [0031](0031-portable-user-preferences-backup-v3-room-v8.md) | Portable user preferences, Backup v3, and Room v8 |
| [0032](0032-recoverable-notification-claims-room-v9.md) | Recoverable notification claims in Room v9 |
