package com.norcode.bukkit.potatobombs.api;

import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collection;

public interface IPotatoBomb {
	public String getEffectName();
	public ShapelessRecipe getRecipe();
	public byte getData();
	public void setData(byte data);

	public PotionEffectType getPotionEffectType();
	public void setPotionEffectType(PotionEffectType potionEffectType);
	public int getAmplifier();
	public void setAmplifier(int amplifier);
	public int getDuration();
	public void setDuration(int duration);
	public boolean isCraftable();
	public void setCraftable(boolean craftable);
	public boolean isEnabled();
	public void setEnabled(boolean enabled);
	public String getDropPermission();
	public String getCraftPermission();
	public String getImmunePermission();
	public PotionEffect getEffect(int amount, int existingDuration);
}
