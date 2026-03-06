# RPGMobs

### Adds tiered elite variants to every _configured_ NPC — with <span style="color:#3598db">scaling</span>, <span style="color:#2dc26b">abilities</span>, <span style="color:#f1c40f">loot</span> and <span style="color:#e67e23">equipment</span> to your world!

![RPGMobs Ability](https://media.forgecdn.net/attachments/1495/784/eliteleapability-gif.gif)

Currently enhancing <span style="color:#2dc26b">165 NPC types</span> — and more to come!

## Documentation / Guide

You can find all the documentation, configuration guides, and developer references on my docs site.

[![Docs](https://img.shields.io/badge/Documentation_/_Guide-3da15b?style=for-the-badge&logo=bookstack&logoColor=white)](https://docs.rpgmobs.frotty27.com/)

[![GitHub](https://img.shields.io/badge/GitHub-RPGMobs-3da15b?style=for-the-badge&logo=github&logoColor=white)](https://github.com/TimShol/hytale-rpgmobs)

## Support
I'm not able to setup a Ko-Fi or donation platform as I have to be registered as a business where I live.
If you wish to support me and this project, you can simply download and play this mod (already makes me happy that people are interested in seeing this grow) and if you want to go the extra mile, you can vote/recommend this mod in the Modding Contest. I'll be adding a link later here if you wish to do so.
**Thank you** for all the support and patience!

## Required Dependencies

*   **[NameplateBuilder](https://www.curseforge.com/hytale/mods/nameplatebuilder)** — Handles nameplate rendering for elite mobs. Must be installed alongside RPGMobs.

***

## Combat

*   5 power tiers (Common to Legendary) with independent health and damage scaling
*   Random damage and health variance for less predictable encounters
*   Ability gating per mob rule with per-tier toggles and weapon category restrictions
*   Per-mob armor slot restrictions — mobs whose models don't support full armor won't equip it
*   Entity effects system (projectile resistance and more)
*   Elite-vs-elite aggro prevention toggle

### Abilities

Elites use an event-driven ability system — abilities trigger contextually based on combat state, not on a timer. There are currently 3 abilities:

*   **Charge Leap** — Gap-closing attack. Triggers when a target is within range.
*   **Heal Leap** — Defensive retreat with self-heal. Triggers when health drops below a threshold.
*   **Undead Summon** — Spawns faction-appropriate reinforcements (goblins summon goblins, trorks summon trorks, undead summon undead). Triggers periodically during sustained combat. Configurable summon pool exclusion prevents certain roles from being summoned.

Each ability is linked to specific mob rules with per-tier enablement toggles and weapon category restrictions — for example, staff-wielding mobs cannot use Charge Leap. By default, most mob categories have abilities enabled for Tier 4 and 5.

More abilities and new AI behaviours are actively in development, including conditional combos, environmental awareness, and tier-exclusive mechanics that will make each encounter feel distinct.

<div class="spoiler"><p><img src="https://media.forgecdn.net/attachments/description/1444529/description_158dce7c-1520-4c2a-a6c0-2053fdce20a8.png" alt="Tiers"></p></div>

## Loot & Gear

*   Category-based weapon and armor organization with hierarchical category trees
*   Rarity-weighted equipment assigned by weapon and armor categories per mob rule
*   Tiered loot tables with configurable drop multipliers (0x to 6x)
*   Per-tier drop enablement — control exactly which tiers can drop each item
*   Loot templates with linked mob rules for targeted drop tables (e.g., Goblin Boss loot)
*   Chance to drop equipped items on death with configurable durability range
*   Consumable drops including food, potions, gems, and materials
*   30+ custom loot entries (ores, bars, gems, life essence, …)

<div class="spoiler"><p><img src="https://media.forgecdn.net/attachments/description/1444529/description_4fc00da8-81bb-4793-8763-7a1f4725bc11.png" alt="Loot"></p></div>

## Visual Identity

*   Tiered nameplates with rank indicators and family prefixes via [NameplateBuilder](https://github.com/TimShol/hytale-nameplate-builder)
*   Model scaling per tier for distinct visual presence
*   Entity effects system

## Per-World Overlays

RPGMobs uses an overlay system with per-world files in `worlds/` and per-instance-template files in `instances/`. Each overlay can override spawning, stats, loot, abilities, visuals, and elite behavior independently.

*   Per-world and per-instance-template overrides via the overlay system
*   Per-world mob rules and loot templates with category tree organization
*   Tier overrides and loot overrides for fine-grained per-mob control
*   Instance worlds (`instance-{Template}-{UUID}`) are automatically matched by template name (case-insensitive)
*   Two built-in presets (Full and Empty) plus custom preset save/restore
*   Manage overlays visually through the Admin UI or edit YAML files directly

## Progression

There are 3 progression styles currently:

*   **Environment (zone-based):** Tier distribution is determined by the Hytale zone the mob spawns in. Each zone (Zone 1 through Zone 4) has configurable tier weights, letting you create smooth difficulty curves across the world map. For example, Zone 1 might heavily favor Tier 1 and 2, while Zone 4 spawns mostly Tier 4 and 5 elites.
*   **Distance from Spawn:** Tier selection and stat bonuses scale based on how far the mob spawns from the world origin. The further out players explore, the stronger the elites become. Health and damage bonuses increase gradually with distance, creating a natural difficulty gradient without relying on zone tags.
*   **Random (None):** Any tier can spawn anywhere with equal probability. Useful for arena-style servers or testing.

## Configuration

<span style="color:#2dc26b"><strong>Almost everything is configurable!</strong></span> The recommended way to configure RPGMobs is through the in-game Admin UI using `/rpgmobs config`.

*   **9 sub-tabs** per world/instance: General, Mob Rules, Stats, Loot, Spawning, Entity Effects, Abilities, Visuals, Overrides
*   **Sidebar navigation** with sections for Global Core, Global Debug, Per-World overlays, and Per-Instance overlays
*   **Tree explorers** for mob rules, loot templates, abilities, and entity effects with search filtering
*   **NPC picker** and **Item picker** popups for adding mob rules and loot drops
*   **Global Config** tabs for managing weapon categories, armor categories, rarity rules, and tier equipment quality
*   **Save & Discard** with unsaved change indicators across all tabs

<!-- TODO: Replace with actual UI-General screenshot URL -->
<div class="spoiler"><p><img src="PLACEHOLDER_UI_GENERAL" alt="Admin UI General"></p></div>

### Editing via Config Files

Optionally, you can also configure RPGMobs by editing the YAML files directly. RPGMobs generates 9 base YAML files plus per-world and per-instance overlay files under:

```
%APPDATA%\Hytale\UserData\Saves\(your save name)\mods\RPGMobs
```

<div class="spoiler"><p><img src="https://media.forgecdn.net/attachments/description/1444529/description_7ec2c5a1-5926-4ed7-8e44-f96ad5861c85.png" alt="Config Directory"></p></div>

*   Layered config: `base/` (9 YAML files) + `worlds/` (per-world overlays) + `instances/` (per-instance overlays)
*   Live reload via `/rpgmobs reload` with automatic reconciliation of existing elites
*   Every feature is independently toggleable
*   Config format version migration — automatically regenerates config when the architecture changes, preserves custom changes on regular updates
*   Missing keys are regenerated automatically — no need to start fresh after a content update

## Asset Generation

*   The generated `Server` folder inside the mod directory is the runtime asset pack — no need to edit it manually

## Permissions

*   **`rpgmobs.reload`** — Allows the player to reload RPGMobs configuration at runtime
*   **`rpgmobs.config`** — Allows the player to open the in-game Admin UI

***

## Installation

*   Download the RPGMobs `.jar` from CurseForge
*   Place it in your server's `mods` folder
*   Start the server to generate default configuration
*   Use `/rpgmobs config` to open the in-game Admin UI (recommended), or edit the YAML files under `%APPDATA%\Hytale\UserData\Saves\(your save name)\mods\RPGMobs`
*   Run `/rpgmobs reload` to apply YAML changes without restarting

## Uninstalling

If you remove the mod, leftover elite NPCs can remain with modified stats and equipment. Do **not** try to kill them — the game will crash since it can no longer find the mod's code.

Instead, use `/npc clean --confirm` and repeat until all remaining elite NPCs are removed.

## Compatibility

RPGMobs (should) work alongside other Hytale mods. Nameplate rendering is handled by [NameplateBuilder](https://www.curseforge.com/hytale/mods/nameplatebuilder).

Custom weapons and armor from other mods can be added to the appropriate weapon or armor categories in `gear.yml` (or use the Admin UI under Global Config to manage categories visually).

✅ **Strongly recommended to play alongside:**

**[Perfect Parries](https://www.curseforge.com/hytale/mods/perfect-parries)** — Adds more depth and skill-based combat, especially against elite enemies.

**[RPGLeveling](https://www.curseforge.com/hytale/mods/rpg-leveling-and-stats)** — Adds experience, levels, zones, (even more) difficulty scaling and stat-based progression. RPGMobs auto-generates a `rpgleveling.yml` config with tier-scaled XP multipliers, ability-based XP bonuses, and minion XP reduction. XP settings are overlayable per-world.

***

## Development Status

<span style="color:#2dc26b"><strong>RPGMobs is actively in development.</strong></span>

*   Expect frequent updates and improvements
*   Feedback and suggestions are very welcome
*   Balancing and content will continue to evolve

Solo developer project — this mod is developed and maintained by one person (me), alongside a full-time job.

***

## FAQ

<span style="color:#843fa1"><strong>Which Hytale version is supported?</strong></span>

<span style="color:#2dc26b"><em>Hytale <strong>Update 3</strong>.</em></span>

<span style="color:#843fa1"><strong>Does this work on existing worlds?</strong></span>

<span style="color:#2dc26b"><em>Yes. RPGMobs works on existing worlds and affects both current and newly spawned mobs.</em></span>

<span style="color:#843fa1"><strong>Is there an in-game configuration UI?</strong></span>

<span style="color:#2dc26b"><em>Yes. Use <code>/rpgmobs config</code> to open the Admin UI. It requires the <code>rpgmobs.config</code> permission. The Admin UI provides visual editors for every setting — base config, per-world overlays, per-instance overlays, weapon/armor categories, and more.</em></span>

<span style="color:#843fa1"><strong>Can I configure what I don't like?</strong></span>

<span style="color:#2dc26b"><em>Yes. Almost everything is configurable — either through the in-game Admin UI (<code>/rpgmobs config</code>) or by editing YAML files directly.</em></span>

<span style="color:#843fa1"><strong>Can I reload the config?</strong></span>

<span style="color:#2dc26b"><em>Yes — use <code>/rpgmobs reload</code>. Existing elites are reconciled on their next tick.</em></span>

<span style="color:#843fa1"><strong>Is the source code available?</strong></span>

<span style="color:#2dc26b"><em>Yes. The full source is available on <a href="https://github.com/TimShol/hytale-rpgmobs" target="_blank" rel="nofollow">GitHub</a>. The mod is MIT licensed.</em></span>

***

## For Developers

RPGMobs ships a separate `rpgmobs-api` artifact for mod developers. Add it as a compile-time dependency to listen to events, query RPG mob state, or modify loot and damage at runtime.

*   Event-driven API with 12 event types (spawn, death, damage, abilities, aggro, loot)
*   Read-only Query API for inspecting any RPG mob's tier, scaling, combat state, and more
*   Cancellable events for spawn blocking, damage modification, and loot customization
*   Lightweight API artifact — no dependency on the full plugin

See the [API documentation](https://docs.rpgmobs.frotty27.com/api/overview) for setup instructions and integration examples.

### Damage System Overview

RPGMobs is compatible with other damage-modifying mods. Multipliers stack, so balance may need tuning — but as long as you know how to read and edit config files, you're in the clear.

<div class="spoiler"><p><img src="https://media.forgecdn.net/attachments/description/1444529/description_2c56d0ab-f22e-4ae9-a7d0-b65e6134caab.png" alt="Damage Pipeline"></p></div>
