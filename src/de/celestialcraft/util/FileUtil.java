package de.celestialcraft.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import de.celestialcraft.AgentEstate.AgentEstate;

public class FileUtil {
	
	private AgentEstate plugin;
	
	private FileConfiguration customConfig = null;
	private File customConfigFile = null;
	
	public FileUtil(AgentEstate instance){
		this.plugin=instance;
	}
	
	public void updateStats(String player, boolean sell) {
		
		int amount = this.getCustomConfig().getInt(player, 0);
		if (sell) {
			
			this.getCustomConfig().set(player, (amount - 1));
		} else {

			this.getCustomConfig().set(player, (amount + 1));
		}
		this.saveCustomConfig();
	}

	public void reloadCustomConfig() {
		if (customConfigFile == null) {
			customConfigFile = new File(plugin.getDataFolder(), "user.yml");
		}
		customConfig = YamlConfiguration.loadConfiguration(customConfigFile);

		// Look for defaults in the jar
		InputStream defConfigStream = plugin.getResource("user.yml");
		if (defConfigStream != null) {
			YamlConfiguration defConfig = YamlConfiguration
					.loadConfiguration(defConfigStream);
			customConfig.setDefaults(defConfig);
		}
	}

	public FileConfiguration getCustomConfig() {
		if (customConfig == null) {
			reloadCustomConfig();
		}
		return customConfig;
	}

	public void saveCustomConfig() {
		if (customConfig == null || customConfigFile == null) {
			return;
		}
		try {
			customConfig.save(customConfigFile);
		} catch (IOException ex) {
			Logger.getLogger(JavaPlugin.class.getName()).log(Level.SEVERE,
					"Could not save config to " + customConfigFile, ex);
		}
	}

}
