package com.norcode.bukkit.potatobombs;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.yaml.snakeyaml.constructor.BaseConstructor;

import com.norcode.bukkit.potatobombs.PotatoBombs;


public class PotatoBomb {
    private static Permission wildcardCraftPermission =  null;
    private static Permission wildcardDropPermission = null;
    private static Permission wildcardImmunePermission = null;
    PotatoBombs plugin;
    byte data = 0;
    PotionEffectType potionEffectType = null;
    int amplifier = 0;
    int duration = 0;
    boolean craftable = false;
    boolean enabled = false;

    private ShapelessRecipe recipe = null;

    private static HashMap<PotionEffectType, PotatoBomb> registered = new HashMap<PotionEffectType, PotatoBomb>();

    private PotatoBomb(PotatoBombs plugin, PotionEffectType effect, byte dyeData, int amplifier, int duration, boolean craftable, boolean enabled) {
        this.plugin = plugin;
        this.potionEffectType = effect;
        this.data = dyeData;
        this.amplifier = amplifier;
        this.duration = duration;
        this.craftable = craftable;
        this.enabled = enabled;
    }

    public String getEffectName() {
        return toTitleCase(this.potionEffectType.getName());
    }

    public static String toTitleCase(String s) {
        String input = s.toLowerCase();
        StringBuilder titleCase = new StringBuilder();
        boolean nextTitleCase = true;
        for (char c : input.toCharArray()) {
            if (Character.isSpaceChar(c)) {
                nextTitleCase = true;
            } else if (nextTitleCase) {
                c = Character.toTitleCase(c);
                nextTitleCase = false;
            }
            titleCase.append(c);
        }
        return titleCase.toString();
    }

    public ShapelessRecipe getRecipe() {
        if (this.recipe == null) {
            ItemStack bomb = new ItemStack(Material.POISONOUS_POTATO);
            ItemMeta meta = bomb.getItemMeta();
            List<String> lore = new ArrayList<String>();
            lore.add(getEffectName());
            meta.setLore(lore);
            meta.setDisplayName(getEffectName() + " Potato-bomb");
            bomb.setItemMeta(meta);
            this.recipe = new ShapelessRecipe(bomb);
            recipe.addIngredient(Material.POISONOUS_POTATO);
            recipe.addIngredient(Material.SULPHUR);
            recipe.addIngredient(Material.INK_SACK, data);
        }
        return recipe;
    }

    public static void unregister(PotatoBombs plugin, PotionEffectType type) {
        Iterator<Recipe> recipeIt = plugin.getServer().recipeIterator();
        while (recipeIt.hasNext()) {
            if (recipeIt.next().equals(registered.get(type).getRecipe())) {
                recipeIt.remove();
                break;
            }
        }
        
        registered.remove(type);
        wildcardCraftPermission.getChildren().remove("potatobombs.craft." + type.getName().toLowerCase());
        wildcardDropPermission.getChildren().remove("potatobombs.drop." + type.getName().toLowerCase());
        wildcardImmunePermission.getChildren().remove("potatobombs.immune." + type.getName().toLowerCase());
        plugin.getServer().getPluginManager().removePermission("potatobombs.craft." + type.getName().toLowerCase());
        plugin.getServer().getPluginManager().removePermission("potatobombs.drop." + type.getName().toLowerCase());
        plugin.getServer().getPluginManager().removePermission("potatobombs.immune." + type.getName().toLowerCase());
        wildcardImmunePermission.recalculatePermissibles();
        wildcardCraftPermission.recalculatePermissibles();
        wildcardDropPermission.recalculatePermissibles();
    }

    public static void register(PotatoBombs plugin, PotionEffectType effect, byte dyeData, int amplifier, int duration, boolean craftable, boolean enabled) {
        if (registered.containsKey(effect)) {
            unregister(plugin, effect);
        }
        PotatoBomb bomb = new PotatoBomb(plugin, effect, dyeData, amplifier, duration, craftable, enabled);
        if (wildcardCraftPermission == null) {
            // initialize wildcard permissions if necessary
            wildcardCraftPermission = new Permission("potatobombs.craft.*", PermissionDefault.TRUE);
            plugin.getServer().getPluginManager().addPermission(wildcardCraftPermission);
            wildcardDropPermission = new Permission("potatobombs.drop.*", PermissionDefault.TRUE);
            plugin.getServer().getPluginManager().addPermission(wildcardDropPermission);
            wildcardImmunePermission = new Permission("potatobombs.immune.*", PermissionDefault.FALSE);
            plugin.getServer().getPluginManager().addPermission(wildcardImmunePermission);
        }
        Permission craftPermission = new Permission(bomb.getCraftPermission(), PermissionDefault.TRUE);
        Permission dropPermission = new Permission(bomb.getDropPermission(), PermissionDefault.TRUE);
        Permission immunePermission = new Permission(bomb.getImmunePermission(), PermissionDefault.FALSE);
        plugin.getServer().getPluginManager().addPermission(craftPermission);
        plugin.getServer().getPluginManager().addPermission(dropPermission);
        plugin.getServer().getPluginManager().addPermission(immunePermission);
        craftPermission.addParent(wildcardCraftPermission, true);
        dropPermission.addParent(wildcardDropPermission, true);
        immunePermission.addParent(wildcardImmunePermission, false);
        wildcardCraftPermission.recalculatePermissibles();
        wildcardDropPermission.recalculatePermissibles();
        wildcardImmunePermission.recalculatePermissibles();
        registered.put(bomb.potionEffectType, bomb);
        if (bomb.craftable) {
            plugin.getLogger().info("Registering Recipe: " + bomb.getRecipe() +  " -> " + bomb.getRecipe().getIngredientList());
            plugin.getServer().addRecipe(bomb.getRecipe());
        }
    }

    public static PotatoBomb get(PotionEffectType type) {
        return registered.get(type);
    }

    public byte getData() {
        return data;
    }

    public void setData(byte data) {
        this.data = data;
    }

    public PotionEffectType getPotionEffectType() {
        return potionEffectType;
    }

    public void setPotionEffectType(PotionEffectType potionEffectType) {
        this.potionEffectType = potionEffectType;
    }

    public int getAmplifier() {
        return amplifier;
    }

    public void setAmplifier(int amplifier) {
        this.amplifier = amplifier;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public boolean isCraftable() {
        return craftable;
    }

    public void setCraftable(boolean craftable) {
        this.craftable = craftable;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public static Collection<PotionEffectType> getRegisteredTypes() {
        return registered.keySet();
    }

    public String getDropPermission() {
        return "potatobombs.drop." + potionEffectType.getName().toLowerCase();
    }

    public String getCraftPermission() {
        return "potatobombs.craft." + potionEffectType.getName().toLowerCase();
    }

    public String getImmunePermission() {
        return "potatobombs.immune." + potionEffectType.getName().toLowerCase();
    }

    public PotionEffect getEffect(int amount, int existingDuration) {
        return new PotionEffect(potionEffectType, existingDuration + (getDuration()*amount), getAmplifier());
    }
}
