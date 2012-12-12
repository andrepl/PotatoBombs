package com.norcode.bukkit.potatobombs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.server.NBTTagCompound;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.inventory.CraftShapelessRecipe;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import org.bukkit.material.MaterialData;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PotatoBombs extends JavaPlugin implements Listener {
	
	public static Random random = new Random();
	ShapelessRecipe poisonousPotatoRecipe;
	private HashMap<PotionEffectType, CraftItemStack> potatoBombs;
	private HashMap<Integer, PotionEffectType> dyeMap;
	@Override
	public void onEnable() {
		
		FileConfiguration config = getConfig();
		config.options().copyDefaults(true);
		saveConfig();
	
	        
		getServer().getPluginManager().registerEvents(this, this);
		poisonousPotatoRecipe = new ShapelessRecipe(new ItemStack(Material.POISONOUS_POTATO));
		poisonousPotatoRecipe.addIngredient(Material.POTATO_ITEM);
		poisonousPotatoRecipe.addIngredient(Material.FERMENTED_SPIDER_EYE);
		if (getConfig().getBoolean("allow-crafting.poisonous-potatoes")) {
			getServer().addRecipe(poisonousPotatoRecipe);
		}
		
		ShapelessRecipe potatoBombRecipe = new ShapelessRecipe(new ItemStack(Material.POISONOUS_POTATO));
		potatoBombRecipe.addIngredient(Material.POISONOUS_POTATO);
		potatoBombRecipe.addIngredient(Material.INK_SACK, 0);
		getServer().addRecipe(potatoBombRecipe);
		
		dyeMap = new HashMap<Integer, PotionEffectType> (9);
		dyeMap.put(1,  PotionEffectType.BLINDNESS);
		dyeMap.put(2,  PotionEffectType.CONFUSION);
		dyeMap.put(3,  PotionEffectType.HARM);
		dyeMap.put(4,  PotionEffectType.HUNGER);
		dyeMap.put(5,  PotionEffectType.POISON);
		dyeMap.put(6,  PotionEffectType.SLOW);
		dyeMap.put(7,  PotionEffectType.SLOW_DIGGING);
		dyeMap.put(8,  PotionEffectType.WEAKNESS);
		dyeMap.put(9,  PotionEffectType.WITHER);
		List<PotionEffectType> effects = new ArrayList<PotionEffectType>(9);
		List<Integer> keys = new ArrayList<Integer>(dyeMap.keySet());
		Collections.sort(keys);
		for (Integer i: keys) {
			effects.add(dyeMap.get(i));
			potatoBombRecipe = new ShapelessRecipe(new ItemStack(Material.POISONOUS_POTATO));
			potatoBombRecipe.addIngredient(Material.POISONOUS_POTATO);
			potatoBombRecipe.addIngredient(Material.INK_SACK, i);
			getServer().addRecipe(potatoBombRecipe);
			
		}
		
		potatoBombs = new HashMap<PotionEffectType, CraftItemStack>(9);
		CraftItemStack s;
		NBTTagCompound disp;
		
		for (PotionEffectType e: effects) {
			s = new CraftItemStack(Material.POISONOUS_POTATO,1);
			s.getHandle().tag = new NBTTagCompound();
			s.getHandle().getTag().setInt("potatoBombEffect", e.getId());
			disp = new NBTTagCompound();
			disp.setString("Name", toTitleCase(e.getName()) + " Potato-bomb");
			s.getHandle().getTag().setCompound("display", disp);
			potatoBombs.put(e, s);
		}
		
	}
	

	public static String toTitleCase(String input) {
	    StringBuilder titleCase = new StringBuilder();
	    boolean nextTitleCase = true;

	    for (char c : input.replace("_", " ").toLowerCase().toCharArray()) {
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
	
	
	@EventHandler(ignoreCancelled=true)
	void onCraftItem(CraftItemEvent event) {
		CraftItemStack stack = (CraftItemStack)event.getInventory().getResult();
		if (stack.getType().equals(Material.POISONOUS_POTATO)) {
			if (event.isShiftClick()) {
				event.setCancelled(true);
			}
		}
	}
	
	@EventHandler(ignoreCancelled=true)
	void onInventoryClick(InventoryClickEvent event) {
		final CraftingInventory inv;

		final Player player = (Player)event.getWhoClicked();
		try {
			inv = (CraftingInventory)event.getInventory();
		} catch (ClassCastException ex) {
			
			return;
		}
		
		if (event instanceof CraftItemEvent) {
		
			return;
			
		}
		
		
		getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
			@Override
			public void run() {
				
				Recipe r = inv.getRecipe();
				if (r != null) {
					if (r instanceof ShapelessRecipe) {
						getLogger().info("Recipe is shapeless");
						getLogger().info(((ShapelessRecipe)r).getIngredientList().toString());
						List<ItemStack> inGrid = new ArrayList<ItemStack>();
						
						for (ItemStack s: inv.getMatrix()) {
							if (s != null && !s.getType().equals(Material.AIR)) {
								inGrid.add(s);
							}
						}
						if (inGrid.size() != 2) return;
						
						ItemStack potato = null;
						ItemStack dye = null;
						for (ItemStack s: inGrid) {
							if (s == null || s.getType().equals(Material.AIR)) continue;
							if (s.getType().equals(Material.POISONOUS_POTATO)) {
								if (potato == null) {
									potato = s;
								} else {
									return;
								}
							} else if (s.getType().equals(Material.INK_SACK)){
								if (dye == null) {
									dye = s;
								} else {
									return;
								}
							}
						}
						
						if (dye != null && potato != null) {
							int dyeId = dye.getData().getData();
							PotionEffectType pt = dyeMap.get(dyeId);
							String permNode = "potatobombs.craft." + pt.getName().toLowerCase().replaceAll("_","");
							boolean has = hasPermission(player,permNode);
							if (has) {
								getLogger().info("Has The Node!");
							} else {
								getLogger().info("No Permissions");
							}
							getLogger().info("Checking permission Node: " + permNode);
							if (potatoBombs.containsKey(pt) && has) {
								CraftItemStack ns = potatoBombs.get(pt).clone();
								inv.setMaxStackSize(64);
								inv.setResult(ns);
							}
						}
						player.updateInventory();
					}
				}
			}
		}, 0);
	}

	
	@EventHandler
	void onPrepareItemCraftEvent(PrepareItemCraftEvent event) {
		Recipe r = event.getRecipe();
		if (r == null) {
			getLogger().info("onPrepare: Recipe is null");
		} else {
			getLogger().info("onPrepare: Recipe is " + r.toString());
			CraftShapelessRecipe sr = (CraftShapelessRecipe)r;
			getLogger().info("onPrepare: " + sr.getIngredientList());
			getLogger().info("onPrepare: " + sr.getResult());

		}
	}

	
	public void showHelp(CommandSender sender) {
		sender.sendMessage("/potatobombs reload - reload config file");
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		
		if (hasPermission((Player)sender, "potatobombs.admin")) {
			if (command.getName().equals("potatobombs")) {
				if (args.length == 0) {
					showHelp(sender);
				} else {
					switch (args[1].toLowerCase()) {
					case "reload":
						reloadConfig();
						break;
					default:
						sender.sendMessage("unknown subcommand.");
						break;
					}
				}
				return true;
			}
		}
		return false;
	}


   public String getMsg(String key, String... params) {
		String t = getConfig().getConfigurationSection("messages").getString(key);
		String k;
		String v;
		for (int i=0;i<params.length-1;i+=2) {
			k = params[i];
			v = params[i+1];
			
			t = t.replaceAll("\\{\\s*" + Pattern.quote(k) + "\\s*\\}", Matcher.quoteReplacement(v));
			
		}
		return t;
	}
   
	@EventHandler
    public void onPlayerDropPotato(PlayerDropItemEvent event) {
		if (event.getItemDrop().getItemStack().getType().equals(Material.POISONOUS_POTATO)) {
			CraftItemStack dropping = (CraftItemStack)event.getItemDrop().getItemStack();
			if (dropping.getHandle().tag != null) {
				if (dropping.getHandle().getTag().hasKey("potatoBombEffect")) {
					PotionEffectType effect = PotionEffectType.getById(dropping.getHandle().getTag().getInt("potatoBombEffect"));
					String permNode = "potatobombs.drop." + effect.getName().toLowerCase().replaceAll("_","");
					getLogger().info("Checking perm node: " + permNode);
					if (!hasPermission(event.getPlayer(), permNode)) event.setCancelled(true);
				}
			}
			
		}
	}
	
	@EventHandler
	public void onPickupPotato(PlayerPickupItemEvent event) {
		CraftItemStack stack = (CraftItemStack)event.getItem().getItemStack();
		if (stack.getType().equals(Material.POISONOUS_POTATO)) {
			if (stack.getHandle().tag != null) {
				
				int effectId = stack.getHandle().getTag().getInt("potatoBombEffect");
				PotionEffectType et = PotionEffectType.getById(effectId);
				String permNode = "potatobombs.immune." + et.getName().toLowerCase().replaceAll("_","");
				getLogger().info("Checking perm node: " + permNode);
				if (!hasPermission(event.getPlayer(), permNode)) {
					int effectDuration = getConfig().getInt("effect-durations." + et.getName().toLowerCase().replaceAll("_",""),100);
					PotionEffect eff = new PotionEffect(et, effectDuration * stack.getAmount(), 1);
					event.getPlayer().sendMessage(getMsg("stepped-on", "effect", et.getName()));
					event.setCancelled(true);
					event.getItem().remove();
					applyEffect(eff, event.getPlayer());
				}
			}
		}
	}

	
	private boolean hasPermission(Player p, String node) {
		if (p.hasPermission(node)) return true;
		int i = node.lastIndexOf('.');
		if (i == -1) return false;
		String wc = node.substring(0,i) + ".*";
		return p.hasPermission(wc);
	}
	
	private void applyEffect(PotionEffect effect, Player player) {
		for (PotionEffect existing: player.getActivePotionEffects()) {
			if (existing.getType().equals(effect.getType())) {
				effect = new PotionEffect(effect.getType(), effect.getDuration() + existing.getDuration(), effect.getAmplifier());
				player.removePotionEffect(effect.getType());
				break;
			}
			
		}
		player.addPotionEffect(effect);
		
	}
}
