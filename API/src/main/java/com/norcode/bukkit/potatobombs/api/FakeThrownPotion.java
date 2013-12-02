package com.norcode.bukkit.potatobombs.api;

import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.inventory.AnvilInventory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public abstract class FakeThrownPotion implements ThrownPotion {

	private static Class<? extends ThrownPotion> implementation;

	public static void initialize(Server server) throws ClassNotFoundException {
		String packageName = server.getClass().getPackage().getName();
		// Get full package string of CraftServer.
		// org.bukkit.craftbukkit.versionstring (or for pre-refactor, just org.bukkit.craftbukkit
		String version = packageName.substring(packageName.lastIndexOf('.') + 1);
		final Class<?> clazz = Class.forName("com.norcode.bukkit.potatobombs." + version + ".FakeThrownPotion");
		if (ThrownPotion.class.isAssignableFrom(clazz)) { // Make sure it actually implements
			implementation = (Class<? extends FakeThrownPotion>) clazz;
		}
	}

	public static ThrownPotion create(Server server, Location location, IPotatoBomb bomb, int amount) {
		try {
			return implementation.getConstructor(Server.class, Location.class, IPotatoBomb.class, Integer.TYPE)
					.newInstance(server, location, bomb, amount);
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
		return null;
	}

}