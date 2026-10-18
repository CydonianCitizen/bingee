# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

Inferred from the product brief: one person tracking a private, personal media history on an Android device.

## Product Purpose

Inferred from the product brief: bingee lets a person search films and series, save them locally, track regular episodes and films as watched, keep favorites, follow releases, and export or restore their data without an account or proprietary backend.

## Positioning

The user's viewing history remains useful offline and portable because local persistence is the product boundary, not a sign-in service.

## Operating Context

The primary workflow is offline-capable Android use with optional TMDB metadata refresh. Your bingee is the personal dashboard, and its collection views are the destination for the saved library. The destination keeps the internal `profile` route; only its user-facing vocabulary is Your bingee / Il tuo bingee.

## Capabilities and Constraints

TMDB is the only runtime media provider. Animation and anime returned by TMDB use the ordinary Movie/TV Series paths; no Jikan integration or separate anime model exists. Serial progress excludes specials and Season 0. Watching is derived from current regular episode availability and progress; caught-up is not persisted. No social, recommendation, account, or cloud-sync features are in scope for this milestone.

The agreed v1.2.5 / versionCode 9 scope is complete: proven dead-code cleanup, selected measured optimizations, final verification, the full Android suite and canonical documentation alignment. The [canonical roadmap](BINGEE_DEVELOPMENT_ROADMAP_AND_CHECKLIST.md#v125-checklist) records accepted limits and the owner's deferral of the next global audit, potential over-engineering/dead-code fixes and optimization study to v1.2.6. Completed optimizations are retained; further optimization directions require a benefits/costs decision. This milestone closure does not publish a release. Room schema 9, Backup v3 export with v1/v2/v3 restore, and the experimental documented TV Time JSON ZIP import define the current data boundaries. Portable preferences live in Room; permission and notification enablement remain device-specific.

## Brand Commitments

The public brand is written `bingee`, with a lowercase initial, in English and Italian, including launcher labels and the personal dashboard. Technical package/class names, repository URLs and persistence identifiers retain their existing spelling.

bingee name and existing Material 3 ColorScheme, light/dark themes, edge-to-edge shell, and existing poster/image loading remain authoritative. Product language distinguishes private personal history from social identity.

## Evidence on Hand

Repository code, Room schema, the Your bingee dashboard and its collection views, Details, Search, Settings, Statistics, and shared poster components. No user accounts or backend are present.

## Product Principles

- User data stays local and exportable.
- Derived serial state follows current regular episode availability.
- Existing navigation and canonical repository logic are reused.
- UI stays accessible, scannable, and calm.

## Accessibility & Inclusion

Inferred from the project instructions: Material touch targets, content descriptions, string resources, font scaling, loading/error/empty states, and light/dark parity are required.
