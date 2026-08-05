# What Was Done Today - 2026-08-05

- Extended the shared LFG backend contract so category payloads can include Discord-synced activities instead of only top-level category rows.
- Added a new `LfgActivity` model and updated the RuneLite LFG create flow to choose activities from the backend catalog rather than free-text entry.
- Added backend support for distinguishing `RUNELITE` and `DISCORD` callers through request headers so Discord-originated groups can be represented in the same Supabase LFG system.
- Added a protected Supabase function for syncing the Discord activity catalog and updated documentation to describe the shared Discord/RuneLite LFG setup.
- Updated the RuneLite LFG sidebar card renderer so member names display one per line instead of a single comma-separated row.
- Updated the RuneLite LFG list logic so started/ongoing groups stay visible and joinable instead of disappearing as soon as their scheduled start time passes.
