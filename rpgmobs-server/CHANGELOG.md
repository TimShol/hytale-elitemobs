# Changelog

All notable changes to RPGMobs will be documented in this file.

## [3.260219.2] - 2026-03-16

### Added

- **7 new combat abilities** bringing the total from 3 to 10:
  - **Dodge Roll**  -  reactive dodge on damage (all tiers) and preemptive dodge when T4+ intelligence detects the target attacking. Direction-based lateral dash with dust particles and weapon trails. Grants brief invulnerability during the roll
  - **Multi Slash Short**  -  1-2 hit quick strikes with 3 random variations per weapon type. Available to all tiers
  - **Multi Slash Medium**  -  2-4 hit combos with 2 random variations per weapon type. Available T2+
  - **Multi Slash Long**  -  4-6 hit full combos with 1 variation per weapon type. Available T3+
  - **War Cry**  -  staff-channeling AOE buff/debuff with healing aura and dark orbs. The mob temporarily swaps to a staff during the cast. Available T3+
  - **Enrage**  -  berserk fist-fighting transformation. The mob de-equips its weapon and enters a 20-second rapid punch chain (3 random variations), then dies from exhaustion with normal loot/XP drops. Available T2+
  - **Volley**  -  ranged projectile burst for bow/crossbow/gun wielders. Available T3+
- **Flail weapon type** with custom swing sounds and Multi Slash template support
- **8 weapon-specific Multi Slash variants** (Swords, Longswords, Daggers, Battleaxes, Axes, Maces, Clubs, Spears) with unique animations, sounds, trails, and independent per-variant config values
- **48 unique combo designs** across all weapon types and Multi Slash lengths, each with weapon-appropriate animations and visual effects
- **8 weapon-specific Charge Leap sound variants**  -  wind-up, launch, and slam sounds now match the equipped weapon type
- **T4-T5 combat intelligence system**  -  higher-tier elites analyze their target's weapon type, shield, and combat state (attacking, charging, blocking) in real-time
- **Combat personalities** (Aggressive, Tactical, Strategic, Berserker) assigned to T4+ elites at spawn. Each personality has preferred and avoided ability tags that influence trigger priority
- **NPC combat coordination system**  -  groups of elites targeting the same entity dynamically assign combat roles (Engage, Flank, Support, Pressure, Recover) to create varied group combat behavior
- **Personality-based ability filtering**  -  each personality grants a coherent 1-4 ability set instead of all abilities, preventing chaotic combat in group encounters
- **Global ability cooldown**  -  1-3 second random cooldown between abilities prevents ability spam
- **Charged attack detection**  -  dodge roll and player attack detection distinguish between the charge-up phase and the actual attack release. Mobs only react when the attack fires, not during charging
- **Player attack proximity detection**  -  new trigger sources detect when a player swings or releases a charged attack near an elite
- **Debug nameplate overlay**  -  shows real-time personality, coordination role, enabled abilities, and current activity on NPC nameplates when debug mode is enabled
- **Global kill switch**  -  `globalEnabled` in `core.yml` disables RPGMobs across all worlds unconditionally, regardless of per-world overlays
- **Weapon category override on spawn command**  -  `/rpgmobs spawn <role> <tier> [weaponCategory]` now accepts an optional weapon type
- **Heal Leap mob type filtering**  -  configurable deny/allow lists prevent undead mobs from drinking heal potions while allowing exceptions for mage types
- **Admin UI: Multi Slash variant selector**  -  8-button variant picker for viewing and editing per-variant config
- **Admin UI: War Cry staff item picker**  -  item picker for selecting the staff item used during War Cry
- **Admin UI: Heal Leap deny/allow lists**  -  add/delete interface for denied mob prefixes and allowed exceptions
- **Ability tag system**  -  abilities declare classification tags and trigger sources for tag-based dispatch and personality matching

### Changed

- Feature system standardized with `AbstractGatedAbilityFeature` base class  -  shared gate evaluation, chance roll, and component lifecycle with final `apply()`/`reconcile()` methods
- Feature cleanup contract  -  each feature removes its own components via `cleanup()`. SpawnSystem de-elite path simplified
- Config pipeline standardized with declarative `OverlayFieldRegistry`  -  adding a new overlay field now requires only 1 registry entry instead of 7 edits across 5 files
- Self-describing ability config  -  ability features declare their config fields via descriptors, eliminating per-ability switch blocks in the admin UI
- All Multi Slash wind-up timings standardized with minimum counter windows: Short 1.1s, Medium 1.6s, Long 2.1s
- All ability recovery animations changed from Idle to Guard (defensive stance)
- Enrage reworked from a short visual buff to a 20-second berserk fist-fighting mode with exhaustion death
- War Cry reworked from roar-based to staff-channeling design with healing aura and dark orb debuff
- Maximum summoned minions per summoner reduced from 7 to 4
- Dodge roll force increased from 200 to 300
- Reconciliation always re-evaluates mob rules unconditionally  -  adding or changing mob rules immediately affects already-spawned elites
- Weapon swap restoration consolidated into a single generic `finalizeWeaponSwap()` method

### Fixed

- Abilities only triggering once per mob lifetime  -  ability lock component mutations were not persisted via `commandBuffer.replaceComponent()`
- Multi Slash never triggering on any NPC  -  tier component read from entity store before command buffer flush
- NPCs permanently frozen after ability use  -  `DisableAll: true` in movement effects persisted after interaction chain ended
- Weapon not restored after War Cry, Heal Leap, or Undead Summon  -  missing commit cases and finalize methods
- Abilities could double-trigger due to race condition between evaluation and deferred chain start
- Elite-vs-elite aggro  -  elites and their minions no longer target or fight each other. AI markers are now properly cleared
- Dodge roll making mobs permanently invulnerable  -  removed engine-level `Invulnerable: true` from templates
- Dodge roll not cancelling damage during invulnerability  -  eval store vs world store mismatch for cooldown tracking
- Abilities lost after server restart  -  ability components now properly persist across chunk unload/reload
- Enrage triggering on ranged mobs  -  now restricted to melee-only
- Summoned minions incorrectly invulnerable on spawn
- Command-spawned elites persisting in disabled worlds  -  `applyTierFromCommand()` now checks enabled state
- Invalid trail, particle, and sound asset IDs causing server crash on startup  -  full asset audit with verified replacements
- Invalid animation IDs causing mobs to stand motionless during abilities  -  all animations verified against Hytale asset files
- Battleaxe Multi Slash hitbox ranges oversized  -  reduced to appropriate values
- All weapon variants now use weapon-appropriate sounds instead of defaulting to sword sounds
- Duplicate logger prefixes in debug output

## [3.260219.1] - 2026-03-05

### Fixed

- Abilities (Charge Leap, Heal Leap, Undead Summon) not working in worlds that have a saved overlay  -  the weapon category tree was not carried over when merging per-world config overlays, causing the weapon gate to reject all weapons

## [3.260303.0] - 2026-03-03

### Added

- **In-game Admin UI**  -  `/rpgmobs config` opens a full configuration panel. Per-world and per-instance settings with 9 tabs: General, Mob Rules, Stats, Loot, Spawning, Entity Effects, Abilities, Visuals, and Overrides
- **Per-world and per-instance configuration**  -  each world or dungeon instance can have its own settings. Unset fields inherit from the base config
- **Category-based equipment system**  -  weapon and armor categories (e.g. Swords, Axes, Heavy Armor) replace the old text-based filters. Fully editable in the Admin UI
- **Loot templates**  -  create custom drop tables and link them to specific mobs or entire categories. Each drop can be toggled per tier individually
- **Per-mob armor slot restrictions**  -  control which armor slots a mob can equip, so mobs whose models don't support armor no longer look broken
- **Faction-based summoning**  -  summoners now call reinforcements from their own faction (goblins summon goblins, trorks summon trorks) instead of only undead
- **Generic entity effects**  -  new status effects can be added via config alone, no code changes needed
- **Elite anti-aggro**  -  new option to prevent elites from targeting other elites
- **Per-world tier and loot overrides**  -  restrict which tiers specific mobs can spawn as, adjust spawn weights, and assign loot templates per mob per world
- **Ability weapon gating**  -  abilities now check the mob's equipped weapon, so staff-wielding mobs won't use melee-only abilities like Charge Leap
- **Per-mob per-tier ability control**  -  each linked mob in an ability can have tiers toggled individually

### Changed

- Config files reorganized into `base/` (9 files), `worlds/`, `instances/`, and `core.yml`  -  existing configs are migrated automatically
- Mob rule weapon filtering now uses categories instead of substring matching
- Ability gating reworked from role-based allow/deny lists to per-mob-rule linking with tier control
- Config changes now take effect on already-spawned elites (mob rules are re-evaluated on reload)

### Fixed

- Pickaxe-wielding mobs incorrectly matching "Axe" weapon rules
- Ability weapon restrictions being silently ignored
- Crash when switching between world instances

## [2.0.2] - 2026-02-18

### Added

- RPGLeveling integration with standalone `rpgleveling.yml` configuration  -  tier-scaled XP multipliers, XP bonus per active ability, and minion XP reduction. Generated automatically when RPGLeveling is detected, configurable via `Integrations.RPGLeveling.enabled` in `core.yml`
- API: `getWorld()` and `getEntityUuid()` on all RPGMobs events
- API: `isMinion()` on death events and query API
- Death events now fire for summoned minions

### Fixed

- Summoned minions no longer survive when their summoner is killed after re-log or server restart  -  chain death now properly kills minions instead of applying an unreliable health modifier

## [2.0.1] - 2026-02-18

### Removed

- Consumable override system (templates, config, and feature)  -  RPGMobs no longer overrides vanilla potion and food interactions, preventing conflicts with other mods

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
