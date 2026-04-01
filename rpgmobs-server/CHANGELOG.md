# Changelog

All notable changes to RPGMobs will be documented in this file.

## [4.260326.4] - 2026-04-01

### Changed (Admin UI (QOL) Improvements)

- **Spawn Tier Restrictions: Delete All / Delete Filtered buttons**  -  search NPCs and bulk-delete tier restrictions, consistent with Loot and Mob Rules tabs
- **Tier Detail Parameters table**  -  Combat AI tier parameters (guard cooldown, strafe interval, shield block duration, guard recovery, retreat HP threshold) are now displayed as a table with all 5 tiers visible simultaneously, replacing the old tab-selector-with-single-value layout
- **Equipment tab**: weapon and armor category rows now stretch full page width and are left-aligned
- **Rarity & Tiers tab**: Per-Tier Equipment and Tier Rarity Weights tables now have full-width row backgrounds with proper left-aligned data
- **Spawning tab**: description text moved above progression style buttons, "Search NPCs:" label with Delete All/Delete Filtered, Add Category/Mob Rule buttons moved below the NPC list, consistent spacing and subtitle styling throughout
- **Combat AI tab**: Combat Styles and Weapon Combat detail panels no longer double-indented from the left wall
- **Loot tab**: restored missing Delete All button for Search Templates and Add Category/Add Template buttons after exiting drop search mode
- **Loot tab**: Drop Rules Delete Filtered/Delete All buttons moved below the drop list for consistency
- **Loot tab**: Search All Drops Delete Filtered button properly aligned with the data table
- **Mob Rules tab**: NPC selection popup now shows "[+] Add Filtered" button to bulk-add all filtered NPCs to the current category
- **Custom Presets section**  -  per-world preset area split into "Apply Preset" and "Custom Presets" with clearer descriptions

### Fixed

- Vanilla loot drops not spawning when extra rolls set to 0  -  the drop table now always rolls at least once as a base replacement for culled weapon/armor items
- Category extract (">" peek button) not working for Spawn Tier Restrictions  -  the extract handler was stripping the category key prefix before map lookup, causing a silent no-op
- Loot template state (expanded template, filters, category) not preserved after save/discard
- Equipment tab state (weapon/armor category navigation) not preserved after save/discard
- Health scaling changes not applying on first save after server restart  -  the post-restart resync was consuming the reconcile cycle without checking for config changes, requiring a second save to take effect
- Reduced same-faction friendly fire damage between elite NPCs  -  DisableDamageGroups now consistently includes both "Self" and the NPC's own faction for all factions. Note: NPCs may still aggro on each other after accidental hits due to engine-level retaliation behavior. Full fix requires generating Attitude Role assets with Friendly group definitions (planned)

### Optimization

- **Mob rule matching performance overhaul**  - Estimated up to 75% reduction in total RPGMobs server overhead

### Changed

- NameplateBuilder dependency now requires version >= 4.260326.2 (breaking API changes)

## [4.260326.3] - 2026-03-30

### Fixed

- Crash when equipping NPCs with zero-capacity hotbar  -  NPCs from other mods (e.g. dialog NPCs) could match a mob rule but have no inventory slots, causing "Slot is outside capacity" crash

## [4.260326.2] - 2026-03-30

### Fixed

- Crash from cross-world entity refs in PlayerAttackTracker  -  dodge roll and parry evaluation iterated refs from all worlds, causing "Incorrect store for entity reference" on multi-world servers

## [4.260326.1] - 2026-03-30

### Fixed

- Death message showing "Template" instead of the NPC name after CAE role change  -  added NameTranslationKey to generated role variants
- Per-weapon basic attack sounds using sword sound for all weapons  -  generic swing interaction was hardcoded to SFX_Sword_T1_Swing

## [4.260326.0] - 2026-03-28

### Added

- **6 new combat abilities** bringing the total from 3 to 9:
  - **Dodge Roll**  -  elites dodge sideways when you swing at them. Higher tiers dodge more often, and charged attacks trigger a boosted dodge chance. The dodge happens instantly and won't wait for other ability cooldowns
  - **Multi Slash Short**  -  quick 1-2 hit strikes, 3 random variations per weapon. All tiers
  - **Multi Slash Medium**  -  aggressive 2-4 hit combos, 2 variations per weapon. T2+
  - **Multi Slash Long**  -  devastating 4-6 hit full combos, 1 variation per weapon. T3+
  - **Enrage**  -  the elite throws away its weapon and goes berserk, punching rapidly for 10 seconds before dying from exhaustion. Drops normal loot. Only living humanoids (Outlanders, Goblins, Trorks). T2+
  - **Volley**  -  ranged elites fire a burst of projectiles. T3+
- **54 unique weapon combos**  -  every melee weapon type (Swords, Longswords, Daggers, Battleaxes, Axes, Maces, Clubs, ClubsFlail, Spears) has its own set of combo animations with matching sounds and visual trails
- **Smart combat AI**  -  elites now use Hytale's CombatActionEvaluator with 4 distinct fighting styles:
  - **Disciplined**  -  steady, measured attacks with long shield blocks
  - **Berserker**  -  fast, aggressive, barely retreats
  - **Tactical**  -  circles around you, retreats early, flanks at T5
  - **Chaotic**  -  unpredictable timing, swarms then scatters
- **Reactive parry**  -  elites with shields can block your attacks. Higher tiers parry more often (T2: 15% up to T5: 70%)
- **Per-tier movement speed**  -  T1 elites rush in fast, T5 elites approach with a slow, menacing walk
- **Global ability cooldown**  -  elites wait 1-3 seconds between abilities so they don't chain them back to back. Dodge roll and parry bypass this so elites can still defend themselves
- **Pickaxes as weapons**  -  pickaxe-wielding NPCs now attack with proper Pickaxe animations
- **Charge Leap weapon sounds**  -  the charge-up, launch, and slam now match the weapon the elite is holding
- **(Debug mode) Weapon category on spawn command**  -  `/rpgmobs spawn Skeleton 5 --weapon longsword` spawns a T5 Skeleton with a longsword
- **Debug nameplate**  -  enable debug mode to see each elite's enabled abilities, current activity, and cooldowns above their head
- **Master kill switch**  -  one toggle in `core.yml` to disable RPGMobs across all worlds instantly
- **In-game Admin UI**  -  `/rpgmobs config` opens a full configuration panel with per-world and per-instance settings
- **Per-world and per-instance configuration**  -  each world or dungeon instance can have its own settings. Unset fields inherit from the base config
- **Category-based equipment system**  -  weapon and armor categories (e.g. Swords, Axes, Heavy Armor) replace the old text-based filters. Fully editable in the Admin UI
- **Loot templates**  -  create custom drop tables and link them to specific mobs or entire categories. Each drop can be toggled per tier individually
- **Per-mob armor slot restrictions**  -  control which armor slots a mob can equip, so mobs whose models don't support armor no longer look broken
- **Role-based summoning**  -  summoners now call reinforcements matching their own role instead of only undead
- **Generic entity effects**  -  new status effects can be added via config alone, no code changes needed
- **Elite anti-aggro**  -  new option to prevent elites from targeting other elites
- **Per-world tier and loot overrides**  -  restrict which tiers specific mobs can spawn as, adjust spawn weights, and assign loot templates per mob per world
- **Ability weapon gating**  -  abilities now check the mob's equipped weapon, so staff-wielding mobs won't use melee-only abilities like Charge Leap
- **Per-mob per-tier ability control**  -  each linked mob in an ability can have tiers toggled individually

### Admin UI

- **Combat AI tab**  -  new tab with 3 sections to customize how elites fight:
  - **Combat Styles**  -  adjust attack speed, retreat behavior, and group tactics for each combat personality
  - **Tier Behavior**  -  toggle which tactics each tier unlocks (shield blocking, stepping back, retreating, watching allies, flanking)
  - **Weapon Combat**  -  configure attack chains, animation sets, sounds (with preview), and visual effects per weapon type. Supports modded weapons and sounds from other asset packs
- **Global Mob Rules**  -  new sidebar section. All mob rules are now edited in one place with per-mob combat style selection
- **Per-world mob rules**  -  simplified ON/OFF toggles per world. Toggle individual rules or entire categories at once. Shows "X of Y rules active" summary
- **Asset picker popup**  -  browse all loaded sounds, animations, trails, and particles by pack name. Click a sound to hear it
- **Save & Reload is fast now**  -  no more disconnects when saving combat AI changes

### Changed

- Config files reorganized into `base/` (9 files), `worlds/`, `instances/`, and `core.yml`  -  existing configs are migrated automatically
- Mob rules are now global  -  edit them once, enable/disable per world
- Mob rule weapon filtering now uses categories instead of substring matching
- Ability gating reworked from role-based allow/deny lists to per-mob-rule linking with tier control
- Heal Leap restricted to living humanoids (Outlanders, Goblins, Trorks)  -  undead mobs no longer drink healing potions
- Config changes now take effect on already-spawned elites (mob rules are re-evaluated on reload)
- NPC and item lists are now detected automatically from loaded asset packs  -  modded NPCs and items show up without rebuilding
- Hytale Update 4 compatibility

### Fixed

- Model scaling not applying on spawn  -  elites all appeared the same size until a config reload triggered reconciliation. The tick-based scaling path was gated behind the reconcile check, so newly spawned mobs never hit the initial scaling code
- Elites spawned via `/rpgmobs spawn` persisting in disabled worlds  -  they now correctly de-elite when the world has RPGMobs disabled
- Pickaxe-wielding mobs incorrectly matching "Axe" weapon rules
- Ability weapon restrictions being silently ignored
- Crash when switching between world instances

## [3.260219.1] - 2026-03-05

### Fixed

- Abilities (Charge Leap, Heal Leap, Undead Summon) not working in worlds that have a saved overlay  -  the weapon category tree was not carried over when merging per-world config overlays, causing the weapon gate to reject all weapons

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
