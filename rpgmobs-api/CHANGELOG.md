# Changelog

All notable changes to RPGMobs API will be documented in this file.

## [1.3.0] - 2026-03-30

### Added

- **Spawn API**  -  new `IRPGMobsSpawnAPI` interface for programmatically creating RPGMobs elites from other mods
  - `RPGMobsAPI.spawn()` accessor  -  static entry point, same pattern as `query()`
  - `spawnElite(World, String, int, Vector3d, Vector3f, String)`  -  creates an NPC and promotes it to an elite in one call
  - `applyEliteTier(World, Ref, int, String)`  -  promotes an already-spawned NPC to an elite
  - `SpawnResult` sealed interface with `Success(entityRef, tier, roleName)` and `Failure(reason, message)`
  - 8 typed failure reasons: `NOT_INITIALIZED`, `CONFIG_NOT_LOADED`, `NPC_SPAWN_FAILED`, `NO_MOB_RULE`, `MOB_RULE_DISABLED`, `RPGMOBS_DISABLED_IN_WORLD`, `EVENT_CANCELLED`, `TIER_APPLY_FAILED`
  - All methods must be called on the world thread  -  use `world.execute()` from other threads

## [1.2.0] - 2026-03-28

### Added

- `IRPGMobsQueryAPI.getMatchedMobRuleKey(Ref, Store)`  -  returns the mob rule key that matched this elite
- `IRPGMobsQueryAPI.getActiveAbilityId(Ref, Store)`  -  returns the currently executing ability ID, or null if idle
- `IRPGMobsQueryAPI.getCombatStyle(Ref, Store)`  -  returns the combat style name (Disciplined, Berserker, Tactical, Chaotic)
- `RPGMobsReconcileEvent` now carries `worldName` and `entityCount` fields

### Changed

- `RPGMobsScalingAppliedEvent` converted from a record to a proper class extending `RPGMobsEvent`  -  now includes `World`, `entityRef`, `tierIndex`, and `roleName` consistent with all other events
- Removed stale `war_cry` reference from `RPGMobsAbilityStartedEvent` Javadoc

## [1.1.0] - 2026-03-16

### Added

- Professional Javadoc on all public classes, interfaces, and methods across the entire API surface
- Usage examples in class-level Javadoc for `RPGMobsAPI`, `IRPGMobsEventListener`, and `IRPGMobsQueryAPI`
- `@since`, `@param`, and `@return` tags on all public API methods
- Javadoc on all 12 event classes documenting event semantics, mutability, and cancellation behavior

### Changed

- `RPGMobsAggroEvent.targetRef()` renamed to `getTargetRef()` for consistency with the getter naming convention used by all other event classes

### Removed

- `AbilityType` enum  -  was incomplete (only 3 of 11 abilities) and never referenced by the server plugin. Ability identification should use the string-based ability IDs from `RPGMobsAbilityStartedEvent.getAbilityId()` instead
- `IRPGMobsQueryAPI.getMigrationVersion(Ref<EntityStore>)`  -  internal migration detail that should not be part of the public API
- `IRPGMobsQueryAPI.needsMigration(Ref<EntityStore>)`  -  internal migration detail that should not be part of the public API
- `IRPGMobsQueryAPI.getSupportedTriggerTypes()`  -  returned a hardcoded set that was never kept in sync with actual trigger sources
- `IRPGMobsQueryAPI.isTriggerTypeSupported(String)`  -  convenience wrapper around the removed `getSupportedTriggerTypes()`

## [1.0.1] - 2026-02-18

### Added

- `RPGMobsEvent.getWorld()`  -  returns the `World` in which the event occurred, available on all event types
- `RPGMobsEvent.getEntityUuid()`  -  returns the entity's `UUID`, eagerly resolved at event construction time
- `RPGMobsDeathEvent.isMinion()`  -  distinguishes summoned minion deaths from regular RPG mob deaths
- `IRPGMobsQueryAPI.isMinion(Ref<EntityStore>)`  -  checks whether an entity is a summoned minion
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
