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

The previously agreed v1.3.0 / versionCode 11 scope was verified and closed on 2026-10-22: a compact
personal Details summary, the accepted horizontal Film/Series History timeline with alternating
cards, and the approved Home calendar refinements. Home places Continue Watching after the
calendar and before TMDB discovery. Shared tracking rules, Room 9 and Backup v3 are unchanged.
The [closure record](BINGEE_DEVELOPMENT_ROADMAP_AND_CHECKLIST.md#v130-closure-record--2026-10-22)
preserves tested scope, deferred work and publication boundaries. The earlier v1.2.6 maintenance
scope is also closed; completed optimization decisions remain in force.

The owner subsequently reopened v1.3.0 for M6–M8. M6 Episode Details/date editing is verified
within its documented scope, using the parent Details state and existing timestamp with Room 9
and Backup v3 unchanged. M7 UI refinements are verified in their documented scope: Home footer,
aligned dashboard title, short Search title, centred Search buttons, a History Collection tile and
four initial ranked genres with expansion. The renewed global audit remains M8; manual
TalkBack for the new page/layout is not implied by the earlier accepted checks.

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
