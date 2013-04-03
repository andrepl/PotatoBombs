[![Build Status](https://travis-ci.org/andrepl/PotatoBombs.png)](https://travis-ci.org/andrepl/PotatoBombs)
PotatoBombs is a bukkit plugin that allows players to craft "PotatoBombs" that explode and apply potion effects when someone tries to pick them up.

Potato-Bombs are crafted by combining a Poisonous Potato, Gunpowder, and a Dye on the crafting grid in any shape.  Each dye color creates a potato bombs with a different potion effect.

All bombs are completely configurable, you can specify your own in the configuration file that gives any effect for any duration, using any dye color.

What's new 0.1.0
===
 * Recipe requires Gunpowder
 * All dyes and effects are now completely configurable.
 * Now uses ItemMeta instead of custom nbt tags.
 * Improved crafting
 * Added an optional explosion effect.
 * Should be compatible with any land-protection plugin.
 * Immunity permissions now default to false for ops.

Default Dyes & Effects
===

These have changed from previous versions.

 * Ink Sack - BLINDNESS
 * Rose Red - WITHER
 * Cactus Green - WEAKNESS
 * Cocoa Beans - HUNGER
 * Lapis Lazuli - SLOW_DIGGING
 * Purple Dye - SLOW
 * Cyan Dye - POISON
 * Orange Dye - CONFUSION

Permissions
===

 * potatobombs.admin - allow use of '/potatobombs reload' to reload config file.
 * potatobombs.drop.* - allow dropping of potatobombs
 * potatobombs.craft.* - allow crafting potatobombs
 * potatobombs.immune.* - grant immunity to potatobombs, (they will be picked up instead of detonating)


Commands
===

 * /potatobombs reload - reload the config file.
