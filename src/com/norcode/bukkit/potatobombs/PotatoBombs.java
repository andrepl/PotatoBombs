package com.norcode.bukkit.potatobombs;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import net.h31ix.updater.Updater;
import net.h31ix.updater.Updater.UpdateType;
import net.minecraft.server.v1_5_R3.EntityPotion;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.v1_5_R3.CraftServer;
import org.bukkit.craftbukkit.v1_5_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_5_R3.entity.CraftThrownPotion;
import org.bukkit.craftbukkit.v1_5_R3.inventory.CraftItemStack;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;


import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PotatoBombs extends JavaPlugin implements Listener {
    private Updater updater;
    public static Random random = new Random();

    @Override
    public void onEnable() {
        FileConfiguration config = getConfig();
        config.options().copyDefaults(true);
        saveConfig();
        loadConfig();
        doUpdater();
        getServer().getPluginManager().registerEvents(this, this);
    }

    public void debug(String s) {
        if (getConfig().getBoolean("debug", false)) {
            getLogger().info(s);
        }
    }
    public void loadConfig() {
        // unregister all bombs
        for (PotionEffectType type: new ArrayList<PotionEffectType>(PotatoBomb.getRegisteredTypes())) {
            debug("Unregistering bomb-type: " + type);
            PotatoBomb.unregister(this, type);
        }
        
        ConfigurationSection cfg;
        for (String key: getConfig().getConfigurationSection("types").getKeys(false)) {
            cfg = getConfig().getConfigurationSection("types").getConfigurationSection(key);
            debug("registering bomb-type: " + key);
            PotatoBomb.register(this, PotionEffectType.getByName(key.toUpperCase()), (byte) cfg.getInt("dye"), cfg.getInt("amplifier", 1), cfg.getInt("duration", 100), cfg.getBoolean("craftable", true), cfg.getBoolean("enabled", true));            
        }
        // Remove Poisonous Potato Recipe
        Iterator<Recipe> it = getServer().recipeIterator();
        while (it.hasNext()) {
            Recipe recipe = it.next();
            if (recipe.getResult().isSimilar(new ItemStack(Material.POISONOUS_POTATO))) {
                getLogger().info("Unregistering Recipe: " + recipe);
                it.remove();
            }
        }
        // re-register poisonous potato recipe if configured.
        if (getConfig().getBoolean("craft-poisonous-potatoes", false)) {
            getLogger().info("Registering Poisonous Potato Recipe");
            ShapedRecipe recipe = new ShapedRecipe(new ItemStack(Material.POISONOUS_POTATO));
            recipe.shape("XXX","XYX", "XXX").setIngredient('X', Material.FERMENTED_SPIDER_EYE).setIngredient('Y', Material.POTATO_ITEM);
            getServer().addRecipe(recipe);
        }
    }

    @Override
    public void onDisable() {
        for (PotionEffectType type: new ArrayList<PotionEffectType>(PotatoBomb.getRegisteredTypes())) {
            PotatoBomb.unregister(this, type);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
            String label, String[] args) {
        if (args.length == 1 && args[0].toLowerCase().equals("reload")) {
            reloadConfig();
            loadConfig();
            return true;
        }
        return false;
    }
    
    public String getMsg(String key, Object... args) {
        String tpl = getConfig().getString("messages." + key);
        if (tpl == null) {
            tpl = "[" + key + "] ";
            for (int i=0;i<args.length;i++) {
                tpl += "{"+i+"}, ";
            }
        }
        return MessageFormat.format(tpl,args);
    }
    
    @EventHandler(ignoreCancelled=true, priority=EventPriority.HIGHEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack stack = event.getItemDrop().getItemStack();
        if (stack.getType().equals(Material.POISONOUS_POTATO)) {
            if (stack.hasItemMeta() && stack.getItemMeta().hasLore()) {
                String type = stack.getItemMeta().getLore().get(0);
                PotionEffectType effect = PotionEffectType.getByName(type);
                PotatoBomb bomb = PotatoBomb.get(effect);
                if (bomb != null) {
                    if (!event.getPlayer().hasPermission(bomb.getDropPermission())) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler(ignoreCancelled=true, priority=EventPriority.MONITOR)
    public void onCraftItem(CraftItemEvent event) {
        if (event.getRecipe() != null && event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            ItemStack result = event.getRecipe().getResult();
            if (result.getType().equals(Material.POISONOUS_POTATO) && result.hasItemMeta() && result.getItemMeta().hasLore()) {
                PotatoBomb bomb = PotatoBomb.get(PotionEffectType.getByName(result.getItemMeta().getLore().get(0)));
                if (bomb != null && (!bomb.isEnabled() || !player.hasPermission(bomb.getCraftPermission()))) {
                    event.setCancelled(true);
                }
            }
        }
    }

    public void doUpdater() {
        String autoUpdate = getConfig().getString("auto-update", "notify-only").toLowerCase();
        if (autoUpdate.equals("true")) {
            updater = new Updater(this, "potatobombs", this.getFile(), UpdateType.DEFAULT, true);
        } else if (autoUpdate.equals("false")) {
            getLogger().info("Auto-updater is disabled.  Skipping check.");
            updater = null;
        } else {
            updater = new Updater(this, "potatobombs", this.getFile(), UpdateType.NO_DOWNLOAD, true);
        }
    }

    @EventHandler(ignoreCancelled=true, priority=EventPriority.HIGHEST)
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        ItemStack stack = event.getItem().getItemStack();
        if (stack.getType().equals(Material.POISONOUS_POTATO) && stack.hasItemMeta() && stack.getItemMeta().hasLore()) {
            String type = stack.getItemMeta().getLore().get(0);
            PotionEffectType effect = PotionEffectType.getByName(type);
            PotatoBomb bomb = PotatoBomb.get(effect);
            if (bomb != null && bomb.isEnabled() && !event.getPlayer().hasPermission(bomb.getImmunePermission())) {
                // First we spawn a 'fake' thrownpotion entity representing the potato's effect.
                ThrownPotion potion = new FakeThrownPotion((CraftServer) getServer(), event.getItem().getLocation(), bomb, stack.getAmount());
                HashMap<LivingEntity, Double> target = new HashMap<LivingEntity, Double>();
                target.put(event.getPlayer(), 1.0);
                // Call a PotionSplashEvent to see if any plugins prevent it.
                PotionSplashEvent pse = new PotionSplashEvent(potion, target);
                getServer().getPluginManager().callEvent(pse);
                // Remove the potion before it splashes.
                potion.remove();
                event.setCancelled(true); // cancel the pickup anyway, the bomb is enabled, it just doesn't work.
                event.getItem().remove();
                if (!pse.isCancelled() && pse.getIntensity((LivingEntity) event.getPlayer()) >= 1.0) {
                    int duration = 0;
                    for (PotionEffect eff: event.getPlayer().getActivePotionEffects()) {
                        if (eff.getType().equals(bomb.getPotionEffectType())) {
                            duration = eff.getDuration();
                            break;
                        }
                    }
                    if (event.getPlayer().hasPotionEffect(bomb.getPotionEffectType())) {
                        event.getPlayer().removePotionEffect(bomb.getPotionEffectType());
                    }
                    ;
                    event.getPlayer().addPotionEffect(bomb.getEffect(stack.getAmount(), duration));
                    if (getConfig().getBoolean("explosion-effect", false)) {
                        event.getPlayer().getWorld().createExplosion(event.getItem().getLocation(), 0.0f, false);
                    }
                    if (stack.getAmount() == 1) {
                        event.getPlayer().sendMessage(getMsg("stepped-on-one", bomb.getEffectName()));
                    } else {
                        event.getPlayer().sendMessage(getMsg("stepped-on-many", stack.getAmount(), bomb.getEffectName()));
                    }
                }
            }
        }
    }

    @EventHandler(ignoreCancelled=true, priority=EventPriority.HIGH)
    public void onConsumePotato(PlayerItemConsumeEvent event) {
        ItemStack stack = event.getItem();
        if (stack.getType().equals(Material.POISONOUS_POTATO) && stack.hasItemMeta() && stack.getItemMeta().hasLore()) {
            String type = stack.getItemMeta().getLore().get(0);
            PotionEffectType effect = PotionEffectType.getByName(type);
            PotatoBomb bomb = PotatoBomb.get(effect);
            if (bomb != null && bomb.isEnabled() && !event.getPlayer().hasPermission(bomb.getImmunePermission())) {
                int duration = 0;
                for (PotionEffect eff: event.getPlayer().getActivePotionEffects()) {
                    if (eff.getType().equals(bomb.getPotionEffectType())) {
                        duration = eff.getDuration();
                        break;
                    }
                }
                if (event.getPlayer().hasPotionEffect(bomb.getPotionEffectType())) {
                    event.getPlayer().removePotionEffect(bomb.getPotionEffectType());
                }
                event.getPlayer().addPotionEffect(bomb.getEffect(1, duration));
                event.getPlayer().sendMessage(getMsg("ate", bomb.getEffectName()));
            }
        }
    }

    private static class FakeThrownPotion extends CraftThrownPotion {
        private PotatoBomb bomb = null;
        private int amount = 0;
        public FakeThrownPotion(CraftServer server, Location location, PotatoBomb bomb, int amount) {
            super(server, new EntityPotion(((CraftWorld) location.getWorld()).getHandle(), location.getX(), location.getY(), location.getZ(), CraftItemStack.asNMSCopy(new ItemStack(Material.POTION, 1))));
            this.bomb = bomb;
            this.amount = amount;
        }

        @Override
        public Collection<PotionEffect> getEffects() {
            List<PotionEffect> effects = new ArrayList<PotionEffect>();
            effects.add(bomb.getEffect(amount, 0));
            return effects;
        }
    }

    @EventHandler(ignoreCancelled=true)
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (event.getPlayer().hasPermission("potatobombs.admin") && updater != null) {
            final String playerName = event.getPlayer().getName();
            getServer().getScheduler().runTaskLaterAsynchronously(this, new Runnable() {
                public void run() {
                    Player player = getServer().getPlayer(playerName);
                    if (player != null && player.isOnline()) {
                        getLogger().info("Updater Result: " + updater.getResult());
                        switch (updater.getResult()) {
                        case UPDATE_AVAILABLE:
                            player.sendMessage("A new version of PotatoBombs is available at http://dev.bukkit.org/server-mods/potatobombs/");
                            break;
                        case SUCCESS:
                            player.sendMessage("A new version of PotatoBombs has been downloaded and will take effect when the server restarts.");
                            break;
                        default:
                            // nothing
                        }
                    }
                }
            }, 20);
        }
    }

}
