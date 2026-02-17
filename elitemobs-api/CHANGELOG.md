# Changelog

All notable changes to EliteMobs API will be documented in this file.

## [1.0.0] - 2026-02-17

### Added

- Initial release of EliteMobs API
- `EliteMobsAPI` static entry point for mod developers
- `EliteMobsAPI.registerListener()` and `unregisterListener()` for event subscriptions
- `EliteMobsAPI.query()` for read-only inspection of elite mob state
- `IEliteMobsEventListener` interface with default no-op handlers for all 12 event types
- `EliteMobSpawnedEvent` for reacting to or cancelling elite spawns
- `EliteMobDeathEvent` with killer reference and position
- `EliteMobDropsEvent` with mutable drop list for adding, removing, or replacing loot
- `EliteMobDamageDealtEvent` with adjustable damage multiplier
- `EliteMobDamageReceivedEvent` for monitoring incoming damage
- `EliteMobAbilityStartedEvent` for reacting to or cancelling ability activation
- `EliteMobAbilityCompletedEvent` and `EliteMobAbilityInterruptedEvent` for ability lifecycle tracking
- `EliteMobAggroEvent` and `EliteMobDeaggroEvent` for combat state changes
- `EliteMobScalingAppliedEvent` with full scaling parameters (health, damage, model)
- `EliteMobReconcileEvent` for synchronizing state after config reloads
- `ICancellable` interface for events that support cancellation (spawn, drops, damage dealt, ability started)
- `IEliteMobsQueryAPI` with methods for tier, scaling, progression, combat state, and summon tracking
- `AbilityType` enum for built-in ability identification (Heal Leap, Charge Leap, Undead Summon)
- `EliteMobsNotInitializedException` for safe early-access detection
