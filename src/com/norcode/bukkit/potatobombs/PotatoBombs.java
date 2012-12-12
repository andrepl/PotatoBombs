package com.norcode.bukkit.potatobombs;

import java.util.ArrayList;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PotatoBombs extends JavaPlugin implements Listener {
	public static Random random = new Random();
	public ArrayList<PotionEffect> effects;
	
	@Override
	public void onEnable() {
		
		FileConfiguration config = getConfig();
		config.options().copyDefaults(true);
		saveConfig();
		loadEffects();
		getServer().getPluginManager().registerEvents(this, this);
		
	}
	
	private void loadEffects() {
		effects = new ArrayList<PotionEffect>();
		String name = null;
		int duration = 100;
		int amplifier = 1;
		String[] parts;
		for (String e: getConfig().getStringList("effects")) {
			parts = e.split(":");
			if (parts.length >= 1) name = parts[0];
			if (parts.length >= 2) duration = Integer.parseInt(parts[1]);
			if (parts.length >= 3) amplifier = Integer.parseInt(parts[2]);
			effects.add(new PotionEffect(PotionEffectType.getByName(name), duration, amplifier));
		}
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
	
	@EventHandler
	public void onDropPotato(PlayerDropItemEvent event) {
		
		ItemStack stack = event.getItemDrop().getItemStack();
		
		if (stack.getType().equals(Material.POISONOUS_POTATO) && stack.getAmount() == 1) {
			if (!event.getPlayer().hasPermission("potatobombs.drop")) return;
			int effectPos = random.nextInt(effects.size());
			
			stack.addUnsafeEnchantment(Enchantment.SILK_TOUCH, effectPos+1);
			
			PotionEffect eff = effects.get(effectPos);
			event.getPlayer().sendMessage(getMsg("dropped", "effect", eff.getType().getName()));
		}
	}

	public PotionEffect getEffect(ItemStack stack) {
		PotionEffect e = effects.get(stack.getEnchantmentLevel(Enchantment.SILK_TOUCH)-1);
		int duration = e.getDuration() * stack.getAmount();
		if (duration > getConfig().getInt("max-duration")) {
			duration = getConfig().getInt("max-duration");
		}
		return new PotionEffect(e.getType(), duration, e.getAmplifier());
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
	public void onPickupPotato(PlayerPickupItemEvent event) {
		ItemStack stack = event.getItem().getItemStack();
		if (stack.getType().equals(Material.POISONOUS_POTATO)) {
			if (stack.getEnchantmentLevel(Enchantment.SILK_TOUCH) != 0) {
				PotionEffect effect = getEffect(stack);
				event.getPlayer().sendMessage(getMsg("stepped-on", "effect", effect.getType().getName()));
				event.setCancelled(true);
				event.getItem().remove();
				applyEffect(effect, event.getPlayer());
			}
		}
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
