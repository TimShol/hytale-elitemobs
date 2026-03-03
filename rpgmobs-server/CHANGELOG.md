# Changelog

All notable changes to RPGMobs will be documented in this file.

## [3.260303.0] - 2026-03-03

### Added

- **In-game Admin UI** — `/rpgmobs config` opens a full configuration panel. Per-world and per-instance settings with 9 tabs: General, Mob Rules, Stats, Loot, Spawning, Entity Effects, Abilities, Visuals, and Overrides
- **Per-world and per-instance configuration** — each world or dungeon instance can have its own settings. Unset fields inherit from the base config
- **Category-based equipment system** — weapon and armor categories (e.g. Swords, Axes, Heavy Armor) replace the old text-based filters. Fully editable in the Admin UI
- **Loot templates** — create custom drop tables and link them to specific mobs or entire categories. Each drop can be toggled per tier individually
- **Per-mob armor slot restrictions** — control which armor slots a mob can equip, so mobs whose models don't support armor no longer look broken
- **Faction-based summoning** — summoners now call reinforcements from their own faction (goblins summon goblins, trorks summon trorks) instead of only undead
- **Generic entity effects** — new status effects can be added via config alone, no code changes needed
- **Elite anti-aggro** — new option to prevent elites from targeting other elites
- **Per-world tier and loot overrides** — restrict which tiers specific mobs can spawn as, adjust spawn weights, and assign loot templates per mob per world
- **Ability weapon gating** — abilities now check the mob's equipped weapon, so staff-wielding mobs won't use melee-only abilities like Charge Leap
- **Per-mob per-tier ability control** — each linked mob in an ability can have tiers toggled individually

### Changed

- Config files reorganized into `base/` (9 files), `worlds/`, `instances/`, and `core.yml` — existing configs are migrated automatically
- Mob rule weapon filtering now uses categories instead of substring matching
- Ability gating reworked from role-based allow/deny lists to per-mob-rule linking with tier control
- Config changes now take effect on already-spawned elites (mob rules are re-evaluated on reload)

### Fixed

- Pickaxe-wielding mobs incorrectly matching "Axe" weapon rules
- Ability weapon restrictions being silently ignored
- Crash when switching between world instances

## [2.0.2] - 2026-02-18

### Added

- RPGLeveling integration with standalone `rpgleveling.yml` configuration — tier-scaled XP multipliers, XP bonus per active ability, and minion XP reduction. Generated automatically when RPGLeveling is detected, configurable via `Integrations.RPGLeveling.enabled` in `core.yml`
- API: `getWorld()` and `getEntityUuid()` on all RPGMobs events
- API: `isMinion()` on death events and query API
- Death events now fire for summoned minions

### Fixed

- Summoned minions no longer survive when their summoner is killed after re-log or server restart — chain death now properly kills minions instead of applying an unreliable health modifier

## [2.0.1] - 2026-02-18

### Removed

- Consumable override system (templates, config, and feature) — RPGMobs no longer overrides vanilla potion and food interactions, preventing conflicts with other mods

### Fixed

- Asset generation (could be) failing on startup due to `${...}` placeholders being split across multiple lines in all template files, causing unresolved placeholders in the generated JSON

## [2.0.0] - 2026-02-18

### Changed

- Rebranded to RPGMobs.

## [1.1.1] - 2026-02-18

### Changed

- Removed the combat overlay text as this can clash with other mods and is an addition that does not really belong to
  RPGMobs.

## [1.1.0] - 2026-02-17

### Added

- Event-driven ability system with three distinct abilities: Charge Leap, Heal Potion, and Undead Summon
- Heal Potion ability allowing elites to drink a healing potion when health drops below a threshold
- Undead Summon ability allowing skeleton and zombie elites to spawn reinforcement minions during combat
- Summoned minions automatically despawn when their summoner dies
- Ability gating system to restrict abilities by mob family, weapon type, and tier
- Ability cooldowns configurable per tier
- Random health variance so no two elites of the same tier have identical health pools
- Distance-based progression with health and damage bonuses that scale with distance from spawn
- Projectile resistance status effect for higher-tier elites
- Consumable drops (food items and potions) with tier-based availability
- Config version tracking with automatic config file regeneration on major version changes
- Reconciliation system to sync existing elites with updated config after a live reload
- Component migration system for seamless upgrades from 1.0.0 saves
- Debug mode with granular logging controls for server admins
- Per-zone tier distribution for all Hytale environment zones
- 200+ predefined mob rules with per-mob weapon overrides and ability restrictions
- Developer API module for other mods to integrate with RPGMobs (see API changelog)

### Changed

- Reworked internal architecture from monolithic systems to modular, event-driven features
- Split the single leap ability into separate Charge Leap (offensive) and Heal Potion (defensive) abilities
- Each configurable feature is now independently togglable without affecting other systems
- Health and damage scaling now uses the Hytale stat modifier system instead of direct value overrides
- Improved health verification after spawn to correctly handle both health increases and decreases
- Ability triggers are now event-driven instead of polling every tick
- Spawn system no longer triggers unnecessary reconciliation passes
- All non-essential log output is now gated behind the debug flag for cleaner production logs

### Fixed

- Health scaling verification no longer fails for tiers with multipliers below 1.0
- Elites no longer briefly spin during the heal ability animation
- Elites no longer attempt abilities without a valid target
- Reconciliation no longer cascades when multiple elites spawn in quick succession

## [1.0.0] - 2026-01-25

### Added

- 5-tier elite system transforming standard NPCs into progressively stronger enemies
- Per-tier health and damage scaling with configurable multipliers
- Per-tier model scaling for visually distinct elite presence
- Randomized armor and weapon equipment based on tier
- Shield and utility item equipping with tier-based probability
- Gear durability randomization on spawn
- Tiered loot tables with vanilla drop multipliers from 0x to 6x
- Weapon, armor, and off-hand drop chances on elite death
- 30+ custom loot drops including ores, bars, gems, potions, and Life Essence
- Charge Leap ability for melee elites with slam damage and knockback
- Dual nameplate modes: Simple and Ranked Role with tier indicators
- Environment-based progression using Hytale zone tags
- 10 YAML configuration files for full server customization
- Live config reload via `/rpgmobs reload` command
- Automatic asset generation for tier-specific NPC visuals
- Mob rule system for per-NPC elite transformation control
