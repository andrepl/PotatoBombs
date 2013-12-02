package com.norcode.bukkit.potatobombs.v1_7_R1;

import com.norcode.bukkit.potatobombs.api.IPotatoBomb;
import net.minecraft.server.v1_7_R1.EntityPotion;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.craftbukkit.v1_7_R1.CraftServer;
import org.bukkit.craftbukkit.v1_7_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_7_R1.entity.CraftThrownPotion;
import org.bukkit.craftbukkit.v1_7_R1.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FakeThrownPotion extends CraftThrownPotion {
	private IPotatoBomb bomb = null;
	private int amount = 0;
	public FakeThrownPotion(Server server, Location location, IPotatoBomb bomb, int amount) {
		super((CraftServer) server, new EntityPotion(((CraftWorld) location.getWorld()).getHandle(),
				location.getX(), location.getY(), location.getZ(),
				CraftItemStack.asNMSCopy(new ItemStack(Material.POTION, 1))));
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