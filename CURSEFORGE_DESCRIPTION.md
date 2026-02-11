# ⚔️ EliteMobs

**EliteMobs** makes combat more <span style="color:#ba372a">dangerous</span>, <span style="color:#f1c40f">rewarding</span>, and <span style="color:#3598db">dynamic</span> by introducing tiered mob variants with special abilities, enhanced stats, visual feedback, and improved loot.

![EliteMobs Ability](https://media.forgecdn.net/attachments/1495/784/eliteleapability-gif.gif)

Highly configurable, designed for both casual servers and hardcore experiences, and compatible with other mods.

Currently enhancing <span style="color:#2dc26b">162 NPC types</span> — and more to come!

***

# 📖 Documentation

You can find all the documentation, configuration guides, and developer references on the docs site:

[![Docs](https://img.shields.io/badge/Documentation-2dc26b?style=for-the-badge&logo=bookstack&logoColor=white)](https://docs.elitemobs.frotty27.com/)

***

# ✨ Features

## 🧠 Elite Abilities

* Mobs gain **special abilities** such as leaps, resistances, and conditional effects
* Mobs scale by **tier**, making higher-tier elites significantly more dangerous
* Fully configurable behavior and scaling

![Tiers](https://media.forgecdn.net/attachments/1498/352/tiers-png.png)

## 💥 Enhanced Combat Feedback

* Includes a **combat text overlay**
* Damage numbers appear **red** and **slightly larger** for better combat clarity

## 🎁 Enhanced Loot

* Mobs drop **configurable loot** per tier
* Mobs spawn with armor and weapons based on their tier
* Each equipped item has a chance to drop on death
* All fully configurable per mob and per tier

![Loot](https://media.forgecdn.net/attachments/1498/353/loot-png.png)

## ⚙️ Configuration

<span style="color:#2dc26b">**Almost everything is configurable!**</span> Check out all available configuration files under:

```
%APPDATA%\Hytale\UserData\Saves\<save name>\mods\EliteMobs
```

![Config](https://media.forgecdn.net/attachments/1498/351/configdirectory-png.png)

(The "Server" folder is the Asset Pack generated at runtime from the `.yml` files — no need to edit it manually.)

## 🔄 Runtime Configuration Reloading

EliteMobs supports **runtime config reloading**:

```
/elitemobs reload
```

This reloads configuration files without restarting the server.

⚠️ Some changes — especially those affecting already-spawned mobs or generated assets — may not apply immediately. If anything feels off, a **server restart is always safe and recommended**.

***

# ❓ FAQ

<span style="color:#843fa1">**Which Hytale version is supported?**</span>
<span style="color:#2dc26b">*Hytale **Update 2**.*</span>

<span style="color:#843fa1">**Does this work on existing worlds?**</span>
<span style="color:#2dc26b">*Yes. EliteMobs works on existing worlds and affects both current and newly spawned mobs.*</span>

<span style="color:#843fa1">**Can I configure what I don't like?**</span>
<span style="color:#2dc26b">*Yes. Feel free to change whatever you want.*</span>

<span style="color:#843fa1">**Can I reload the config?**</span>
<span style="color:#2dc26b">*Yes — use `/elitemobs reload`. Restart if changes don't fully apply.*</span>

<span style="color:#843fa1">**Is the source code available?**</span>
<span style="color:#2dc26b">*Not on GitHub yet, but you can check the bytecode. The mod is MIT licensed — **credit me and this mod** if you use or modify the code.*</span>

***

# 🛠️ Installation

1. Download the **EliteMobs `.jar` file**
2. Place it in your server's **mods folder**
3. Start the server — configuration files are generated automatically

***

## 🗑️ Uninstalling

If you decide to remove this mod, leftover NPCs will still have modified stats, equipment, and nameplates. That's just how Hytale's Entity Component System works.

⚠️ Do **NOT** try to kill them — the game will crash since it can no longer find the mod's code.

Instead, use the following command to clean up affected NPCs around you:

* **`/npc clean --confirm`**

Repeat until all modified NPCs are gone.

***

## 🔐 Permissions (Server Owners)

* **`elitemobs.reload`** — Allows the player to reload EliteMobs configuration at runtime

***

## 🤝 Compatibility

EliteMobs is designed to be compatible with **almost any mod**.

For mods that modify shared components like Nameplates, a compatibility mod is needed to merge conflicting behaviors. Currently available:

**RPGLevelingAndStats** ([https://www.curseforge.com/hytale/mods/rpg-leveling-and-stats](https://www.curseforge.com/hytale/mods/rpg-leveling-and-stats))
* [https://www.curseforge.com/hytale/mods/elitemobs-rpgleveling-compat](https://www.curseforge.com/hytale/mods/elitemobs-rpgleveling-compat)

For mods that scale damage, see the "For Developers" section below to understand how multipliers interact. They're compatible, but can break balance!

✅ **Strongly recommended to play alongside:**
**Perfect Parries** — [https://www.curseforge.com/hytale/mods/perfect-parries](https://www.curseforge.com/hytale/mods/perfect-parries)

Perfect Parries complements EliteMobs by adding more depth and skill-based combat, especially against elite enemies.

***

# 🧪 Development Status & Feedback

🚧 **EliteMobs is actively in development.**

* Expect frequent updates and improvements
* Feedback and suggestions are very welcome
* Balancing and content will continue to evolve

👤 **Solo developer project** — This mod is developed and maintained by one person (me), alongside a full-time job.

🎨 I'm also looking for a **small logo** — if you'd like to help, feel free to reach out!

***

# 🗺️ Roadmap

## 🔮 Advanced Abilities
* More complex ability logic
* Conditional triggers and combos
* Better visual and audio feedback

## 🧪 Advanced Loot Systems
* More loot variety
* Better scaling per tier
* Rare elite-exclusive drops

## ⏱️ Combat Flow Improvements
* Reduced potion & food drinking timers to better match increased combat difficulty

## 📊 Smarter Difficulty Scaling
* Granular control over how difficulty scales over time (spawn rates, health, damage, loot, …)

***

# 🧑‍💻 For Developers

## ⚔️ Damage System Overview

EliteMobs is compatible with other damage-modifying mods. Multipliers stack, so balance may need tuning — but as long as you know how to read and edit config files, you're in the clear.

Here's a high-level diagram of the Hytale Damage Pipeline:

![image](https://media.forgecdn.net/attachments/description/1444529/description_2c56d0ab-f22e-4ae9-a7d0-b65e6134caab.png)

## ⚙️ Asset Generation

* Assets are generated automatically from templates
* Tiered assets are created per configuration
* Reduces duplication and manual asset maintenance

## 📄 Configuration System

* Java defaults are the source of truth
* YAML overrides are applied at runtime
* Missing keys are regenerated automatically
* Supports deep nesting and per-tier values

This allows large-scale configurability without repetitive boilerplate.
