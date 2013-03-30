package com.norcode.bukkit.potatobombs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.server.v1_5_R2.NBTTagCompound;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.v1_5_R2.inventory.CraftItemStack;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapelessRecipe;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.PlayerCache;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.object.PlayerCache.TownBlockStatus;
import com.palmergames.bukkit.towny.utils.PlayerCacheUtil;
import com.palmergames.bukkit.towny.war.flagwar.TownyWarConfig;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;

public class PotatoBombs extends JavaPlugin implements Listener {
	
	public static Random random = new Random();
	ShapelessRecipe poisonousPotatoRecipe;
	private HashMap<PotionEffectType, CraftItemStack> potatoBombs;
	private HashMap<Integer, PotionEffectType> dyeMap;
	private Towny towny = null;
	private WorldGuardPlugin wg = null;
	public boolean hasTowny() {
		return towny != null;
	}
	
	public Towny getTowny() {
		return towny;
	}
	
	public boolean hasWorldGuard() {
		return wg != null;
	}
	
	public WorldGuardPlugin getWorldGuard() {
		return wg;
	}
	
	@Override
	public void onEnable() {
		
		FileConfiguration config = getConfig();
		config.options().copyDefaults(true);
		saveConfig();
		
		towny = (Towny)getServer().getPluginManager().getPlugin("Towny");
		wg = (WorldGuardPlugin)getServer().getPluginManager().getPlugin("WorldGuard");
	        
		getServer().getPluginManager().registerEvents(this, this);
		poisonousPotatoRecipe = new ShapelessRecipe(new ItemStack(Material.POISONOUS_POTATO));
		poisonousPotatoRecipe.addIngredient(Material.POTATO_ITEM);
		poisonousPotatoRecipe.addIngredient(Material.FERMENTED_SPIDER_EYE);
		if (getConfig().getBoolean("allow-crafting.poisonous-potatoes")) {
			getServer().addRecipe(poisonousPotatoRecipe);
		}
//		
		ShapelessRecipe potatoBombRecipe; // = new ShapelessRecipe(new ItemStack(Material.POISONOUS_POTATO));
//		potatoBombRecipe.addIngredient(Material.POISONOUS_POTATO);
//		potatoBombRecipe.addIngredient(Material.INK_SACK, 0);
//		getServer().addRecipe(potatoBombRecipe);
//		
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
			potatoBombRecipe = new ShapelessRecipe(new ItemStack(Material.POISONOUS_POTATO, 1, (short)0));
			potatoBombRecipe.addIngredient(Material.POISONOUS_POTATO);
			potatoBombRecipe.addIngredient(Material.INK_SACK, i);
			getServer().addRecipe(potatoBombRecipe);
			
		}
		
		potatoBombs = new HashMap<PotionEffectType, CraftItemStack>(9);
		CraftItemStack s;
		NBTTagCompound disp;
		
		for (PotionEffectType e: effects) {
			net.minecraft.server.v1_5_R2.ItemStack nms = CraftItemStack.asNMSCopy(new ItemStack(Material.POISONOUS_POTATO));
			nms.tag = new NBTTagCompound();
			nms.tag.setInt("potatoBombEffect", e.getId());
			
			disp = new NBTTagCompound();
			disp.setString("Name", toTitleCase(e.getName()) + " Potato-bomb");
			nms.tag.setCompound("display", disp);
			potatoBombs.put(e, CraftItemStack.asCraftMirror(nms));
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
	
	
	void onCraftItem(CraftItemEvent event) {
		CraftItemStack stack = (CraftItemStack)event.getInventory().getResult();
		if (stack != null && stack.getType().equals(Material.POISONOUS_POTATO)) {
			if (event.isShiftClick()) {
				// shift click is super wierd and buggy
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
			onCraftItem((CraftItemEvent)event);
			
		} 
		
		getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
			@Override
			public void run() {
				
				Recipe r = inv.getRecipe();
				if (r != null) {
					if (r instanceof ShapelessRecipe) {
						
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
							getLogger().info("Should be a potatobomb. check perms and junk.");
							int dyeId = dye.getData().getData();
							PotionEffectType pt = dyeMap.get(dyeId);
							String permNode = "potatobombs.craft." + pt.getName().toLowerCase().replaceAll("_","");
							getLogger().info("Permnode: " + permNode);
							if (potatoBombs.containsKey(pt) && hasPermission(player,permNode)) {
								getLogger().info("Crafting PBOMBS!");
								CraftItemStack ns = potatoBombs.get(pt).clone();
								inv.setResult(ns);
							}
						}
						player.updateInventory();
					}
				}
			}
		}, 0);
	}

	
	public void showHelp(CommandSender sender) {
		sender.sendMessage("/potatobombs reload - reload config file");
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		
		if (sender.hasPermission("potatobombs.admin")) {
			if (command.getName().equals("potatobombs")) {
				if (args.length == 0) {
					showHelp(sender);
				} else {
					switch (args[0].toLowerCase()) {
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
			net.minecraft.server.v1_5_R2.ItemStack nms = CraftItemStack.asNMSCopy(dropping); 
			if (nms.tag != null) {
				if (nms.getTag().hasKey("potatoBombEffect")) {
					if (hasWorldGuard()) {
						if (!getWorldGuard().canBuild(event.getPlayer(), event.getPlayer().getLocation().getBlock())) {
							event.setCancelled(true);
							return;
						}
					}
					if (hasTowny()) {
						TownBlock tb = getTowny().getTownyUniverse().getTownBlock(event.getPlayer().getLocation());
						if (tb.getPermissions().pvp) {
							String worldName = event.getPlayer().getLocation().getWorld().getName();
							WorldCoord worldCoord = new WorldCoord(worldName, Coord.parseCoord(event.getPlayer()));
						
							boolean bItemUse = false;
							boolean wildOverride = false;
							try {
								bItemUse = PlayerCacheUtil.getCachePermission(event.getPlayer(), event.getPlayer().getLocation(), 0, (byte) 0, TownyPermission.ActionType.DESTROY);
								wildOverride = TownyUniverse.getPermissionSource().hasWildOverride(worldCoord.getTownyWorld(), event.getPlayer(), 0, (byte)0, TownyPermission.ActionType.DESTROY);
							} catch (NotRegisteredException ex) {
								event.setCancelled(true);
								return;
							}

							PlayerCache cache = getTowny().getCache(event.getPlayer());
							//cache.updateCoord(worldCoord);
							try {
								TownBlockStatus status = cache.getStatus();
								if (status == TownBlockStatus.UNCLAIMED_ZONE && wildOverride) {
									event.setCancelled(false);
									return;
								}
								
								// Allow item_use if we have an override
								if (((status == TownBlockStatus.TOWN_RESIDENT) && (TownyUniverse.getPermissionSource().hasOwnTownOverride(event.getPlayer(), 0, (byte)0, TownyPermission.ActionType.ITEM_USE))) || (((status == TownBlockStatus.OUTSIDER) || (status == TownBlockStatus.TOWN_ALLY) || (status == TownBlockStatus.ENEMY)) && (TownyUniverse.getPermissionSource().hasAllTownOverride(event.getPlayer(), 0,(byte)0, TownyPermission.ActionType.DESTROY)))) {
									event.setCancelled(false);
									return;
								}
								
								if (status == TownBlockStatus.WARZONE) {
									if (!TownyWarConfig.isAllowingItemUseInWarZone()) {
										event.setCancelled(true);
										TownyMessaging.sendErrorMsg(event.getPlayer(), TownySettings.getLangString("msg_err_warzone_cannot_use_item"));
									}
									return;
								}
							} catch (NullPointerException ex) {
							}
						}
					}
					PotionEffectType effect = PotionEffectType.getById(nms.getTag().getInt("potatoBombEffect"));
					String permNode = "potatobombs.drop." + effect.getName().toLowerCase().replaceAll("_","");
					if (!hasPermission(event.getPlayer(), permNode)) event.setCancelled(true);
				}
			}
			
		}
	}
	
	@EventHandler
	public void onPickupPotato(PlayerPickupItemEvent event) {
		CraftItemStack stack = (CraftItemStack)event.getItem().getItemStack();
		if (stack.getType().equals(Material.POISONOUS_POTATO)) {
			net.minecraft.server.v1_5_R2.ItemStack nms = CraftItemStack.asNMSCopy(stack); 
			if (nms.tag != null) {
				
				int effectId = nms.getTag().getInt("potatoBombEffect");
				PotionEffectType et = PotionEffectType.getById(effectId);
				String permNode = "potatobombs.immune." + et.getName().toLowerCase().replaceAll("_","");
				if (!hasPermission(event.getPlayer(), permNode)) {
					int effectDuration = getConfig().getInt("effect-durations." + et.getName().toLowerCase().replaceAll("_",""),0);
					PotionEffect eff = new PotionEffect(et, effectDuration * stack.getAmount(), 1);
					event.getPlayer().sendMessage(getMsg("stepped-on", "effect", toTitleCase(et.getName())));
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
