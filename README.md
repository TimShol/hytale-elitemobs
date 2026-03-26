# RPGMobs

## Tiered elites that change combat, loot, and progression

RPGMobs transforms standard Hytale NPCs into tiered elites with scaling stats, combat abilities, tiered loot, and
distinct visuals. Fully configurable via YAML or the in-game Admin UI, with runtime reloads, per-world overlays,
an event-driven modding API, and support for both casual and hardcore servers.

## Downloads

<table>
<tr>
<td align="center" width="50%">
<img src="icons/RPGMobs-Icon-128.png" alt="Plugin" width="128"/>
<br/><br/>
<strong>Plugin</strong>
<br/><br/>
<a href="https://www.curseforge.com/hytale/mods/rpgmobs">
<img src="https://img.shields.io/badge/Download-F16436?style=for-the-badge&logo=curseforge&logoColor=white" alt="Download RPGMobs"/>
</a>
</td>
<td align="center" width="50%">
<img src="icons/RPGMobs-Icon-128.png" alt="API" width="128"/>
<br/><br/>
<strong>API</strong>
<br/><br/>
<a href="https://www.curseforge.com/hytale/mods/rpgmobs-api">
<img src="https://img.shields.io/badge/Download-F16436?style=for-the-badge&logo=curseforge&logoColor=white" alt="Download RPGMobs API"/>
</a>
</td>
</tr>
</table>

## Documentation

Full configuration guides, developer API reference, and troubleshooting:

[![Docs](https://img.shields.io/badge/Documentation-2dc26b?style=for-the-badge&logo=bookstack&logoColor=white)](https://docs.RPGMobs.frotty27.com/)

## Feature Highlights

### Combat

- 5 power tiers (Common to Legendary) with independent health and damage scaling
- Combat abilities: Charge Leap, Heal Potion, Undead Summon
- Faction-based summoning  -  goblins summon goblins, trorks summon trorks, undead summon undead
- Random damage and health variance for less predictable encounters
- Ability gating per mob rule with per-tier toggles and weapon category restrictions
- Per-mob armor slot restrictions for mobs whose models don't support full armor
- Elite-vs-elite aggro prevention toggle
- In-game Admin UI for live configuration (`/rpgmobs config`)

### Loot & Gear

- Category-based weapon and armor organization with hierarchical category trees
- Rarity-weighted equipment assigned by weapon and armor categories per mob rule
- Tiered loot tables with configurable drop multipliers (up to 6x for Tier 5)
- Per-tier drop enablement  -  control exactly which tiers can drop each item
- Loot templates with linked mob rules for targeted drop tables (e.g., Goblin Boss loot)
- Chance to drop equipped items on death with configurable durability range
- Consumable drops including food, potions, gems, and materials

### Identity

- Tiered nameplates with rank indicators and family prefixes via [NameplateBuilder](https://github.com/TimShol/hytale-nameplate-builder)
- Model scaling per tier for distinct visual presence
- Entity effects system (projectile resistance and more)

### Progression

- Three progression styles: Environment (zone-based), Distance from Spawn, or Random
- Per-zone tier distribution with configurable weights
- Distance-based stat bonuses for smooth difficulty curves

### Per-World Overlays

- Per-world and per-instance-template overrides via the overlay system (`worlds/` and `instances/` directories)
- Override spawning, stats, loot, abilities, visuals, and elite behavior per world or instance
- Per-world mob rules and loot templates with category tree organization
- Tier overrides and loot overrides for fine-grained per-mob control
- Instance worlds (`instance-{Template}-{UUID}`) are automatically matched by template name (case-insensitive)
- Two built-in presets (Full and Empty) plus custom preset save/restore
- Manage overlays visually through the Admin UI or edit YAML files directly

### Admin UI

RPGMobs includes a full in-game configuration panel accessible via `/rpgmobs config`. Every setting can be managed visually without editing YAML files.

- **Sidebar navigation** with sections for Global Core, Global Debug, Per-World overlays, and Per-Instance overlays
- **9 sub-tabs** per world/instance: General, Mob Rules, Stats, Loot, Spawning, Entity Effects, Abilities, Visuals, Overrides
- **Tree explorers** for mob rules, loot templates, abilities, and entity effects with search filtering
- **NPC picker** and **Item picker** popups for adding mob rules and loot drops
- **Global Config** tabs for managing weapon categories, armor categories, rarity rules, and tier equipment quality
- **Save & Discard** with unsaved change indicators (yellow/green markers) across all tabs

![Admin UI Overview](docs/images/admin-ui-main.png)

![Admin UI Mob Rules](docs/images/ui-mob-rules.png)

### Integrations

- Built-in RPGLeveling support with tier-scaled XP, ability-based XP bonuses, and minion XP reduction
- XP settings are overlayable per-world  -  each world or instance can have its own XP multipliers
- Standalone `rpgleveling.yml` config auto-generated when RPGLeveling is detected

### For Developers

- Event-driven API with 12 event types (spawn, death, damage, abilities, aggro, loot)
- Read-only Query API for inspecting any RPG mob's state
- Cancellable events for spawn blocking, damage modification, and loot customization
- Separate API artifact for compile-time dependency

## Quick Start

```text
1. Download RPGMobs and place the JAR in your server's mods folder
2. Start the server to generate default configuration
3. Use /rpgmobs config for the in-game Admin UI, or edit the YAML files directly
4. Run /rpgmobs reload to apply YAML changes without restarting
```

## Installation

1. Download the RPGMobs `.jar` from CurseForge.
2. Place it in your server `mods` folder.
3. Start the server to generate configuration files.
4. Configuration files are created under:

```
%APPDATA%\Hytale\UserData\Saves\<save name>\mods\RPGMobs
```

## Configuration

RPGMobs uses a layered configuration system with a base config directory, per-world overlays, and per-instance overlays.

| Path | Purpose |
|:---|:---|
| `core.yml` | Global settings: enabled by default, debug mode, config format version |
| `base/core.yml` | System config: reconciliation, integrations (RPGLeveling) |
| `base/stats.yml` | Health and damage multipliers per tier |
| `base/spawning.yml` | Progression style, spawn chances, zone distributions |
| `base/gear.yml` | Equipment categories, rarity rules, armor materials |
| `base/loot.yml` | Drop rates, loot templates, extra drops |
| `base/abilities.yml` | Ability toggles, cooldowns, linked mob rules, per-tier scaling |
| `base/visuals.yml` | Nameplates, model scaling, family prefixes |
| `base/effects.yml` | Entity effects (projectile resistance, etc.) |
| `base/mobrules.yml` | NPC rules, weapon categories, armor slots |
| `worlds/` | Per-world overlay YAML files |
| `instances/` | Per-instance-template overlay YAML files |

## Runtime Reload

```text
/rpgmobs reload
```

Reloads all YAML configuration from disk. Spawn logic updates immediately. Existing elites are reconciled on their
next tick  -  mob rules are re-evaluated and equipment is re-applied as needed.

## Commands

| Command | Description |
|:---|:---|
| `/rpgmobs reload` | Reloads all YAML configuration files from disk |
| `/rpgmobs config` | Opens the in-game Admin UI for visual configuration |
| `/rpgmobs spawn <role> <tier>` | Spawns an elite NPC (debug mode only) |
| `/npc clean --confirm` | Removes all Elite entities from the world |

## Permissions

| Permission | What it allows |
|:---|:---|
| `rpgmobs.reload` | Use the `/rpgmobs reload` command |
| `rpgmobs.config` | Use the `/rpgmobs config` command to open the Admin UI |
| `rpgmobs.spawn` | Use the `/rpgmobs spawn` command (debug mode only) |

## API Overview

RPGMobs ships a separate `rpgmobs-api` artifact for mod developers. Add it as a compile-time dependency to listen to
events, query RPG mob state, or modify loot and damage.

See the [API documentation](https://docs.RPGMobs.frotty27.com/api/overview) for integration details.

## Uninstalling

If you remove the mod, leftover elite NPCs can remain with modified stats and equipment. Use:

```text
/npc clean --confirm
```

Repeat until all remaining elite NPCs are removed. Do not kill them directly as it can crash the game.

## Compatibility

RPGMobs works alongside other Hytale mods. Nameplate rendering is handled by
[NameplateBuilder](https://github.com/TimShol/hytale-nameplate-builder), which provides the tiered nameplate
display used by RPGMobs. Mod developers looking to extend or interact with RPGMobs should use the
`rpgmobs-api` artifact  -  see the [API documentation](https://docs.RPGMobs.frotty27.com/api/overview) for
integration details.

[![GitHub](https://img.shields.io/badge/GitHub-NameplateBuilder-7C3AED?style=for-the-badge&logo=github&logoColor=white)](https://github.com/TimShol/hytale-nameplate-builder)

## License

This project is licensed under the [MIT License](LICENSE).
