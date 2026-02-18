# Changelog

All notable changes to RPGMobs API will be documented in this file.

## [1.1.0] - 2026-02-18

### Added

- `RPGMobsEvent.getWorld()` — returns the `World` in which the event occurred, available on all event types
- `RPGMobsEvent.getEntityUuid()` — returns the entity's `UUID`, eagerly resolved at event construction time
- `RPGMobsDeathEvent.isMinion()` — distinguishes summoned minion deaths from regular RPG mob deaths
- `IRPGMobsQueryAPI.isMinion(Ref<EntityStore>)` — checks whether an entity is a summoned minion
- Death events now fire for summoned minions (with `isMinion()` returning `true`)

## [1.0.0] - 2026-02-18

### Added

- Initial release of RPGMobs API
- `RPGMobsAPI` static entry point for mod developers
- `RPGMobsAPI.registerListener()` and `unregisterListener()` for event subscriptions
- `RPGMobsAPI.query()` for read-only inspection of RPG mob state
- `IRPGMobsEventListener` interface with default no-op handlers for all 12 event types
- `RPGMobsSpawnedEvent` for reacting to or cancelling elite spawns
- `RPGMobsDeathEvent` with killer reference and position
- `RPGMobsDropsEvent` with mutable drop list for adding, removing, or replacing loot
- `RPGMobsDamageDealtEvent` with adjustable damage multiplier
- `RPGMobsDamageReceivedEvent` for monitoring incoming damage
- `RPGMobsAbilityStartedEvent` for reacting to or cancelling ability activation
- `RPGMobsAbilityCompletedEvent` and `RPGMobsAbilityInterruptedEvent` for ability lifecycle tracking
- `RPGMobsAggroEvent` and `RPGMobsDeaggroEvent` for combat state changes
- `RPGMobsScalingAppliedEvent` with full scaling parameters (health, damage, model)
- `RPGMobsReconcileEvent` for synchronizing state after config reloads
- `ICancellable` interface for events that support cancellation (spawn, drops, damage dealt, ability started)
- `IRPGMobsQueryAPI` with methods for tier, scaling, progression, combat state, and summon tracking
- `AbilityType` enum for built-in ability identification (Heal Leap, Charge Leap, Undead Summon)
- `RPGMobsNotInitializedException` for safe early-access detection
