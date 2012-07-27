package de.tink.minecraft.plugin.safari;

/*
Copyright (C) 2012 Thomas Starl

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>
*/


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class SafariPlugin extends JavaPlugin {
	private SafariCommandExecutor safariCommmandExecutor;
	private SafariEventListener safariEventListener;
	private static String playerConfigFileName = "playerData.yml";
	private FileConfiguration playerConfig = null;
	private File playerConfigFile = null;
	
	private String SAFARI_START_SUCCESS = "Enlisting for safari \"?1\" was saved successfully. Happy hunting!";
	private String SAFARI_END_SUCCESS = "Your safari was completed successfully.";
	private String SAFARI_INFO_POINTS = "You have ?1 safaripoints.";
	private String SAFARI_PLAYER_CURRENTLY_ENLISTED = "You are currently enlisted for afari \"?1\". Cancel it or complete it before starting a new one.";

	@Override
	public void onDisable() {
		getLogger().info("Disabling: "+this.toString());
	}

	@Override
	public void onEnable() {
		getConfig().options().copyDefaults(true);
        saveConfig();
        getPlayerConfig();
        savePlayerConfig();
        getLogger().info(this.toString() + " has been enabled.");
		safariCommmandExecutor = new SafariCommandExecutor(this);
		safariEventListener = new SafariEventListener(this);
		getCommand("safari").setExecutor(safariCommmandExecutor);
		getServer().getPluginManager().registerEvents(safariEventListener, this);
    }
	
	public void startSafari(Player player, String safariName) {
		Configuration playerConfig = this.getPlayerConfig();
		String currentSafari = playerConfig.getString("registered_players."+player.getName()+".safari");
		if ( currentSafari == null ) {
			playerConfig.set("registered_players."+player.getName()+".safari", safariName);
			playerConfig.set("registered_players."+player.getName()+".mobs_killed",0);
			savePlayerConfig();
			reloadPlayerConfig();
			player.sendMessage(SAFARI_START_SUCCESS.replace("?1", safariName));
		} else  {
				player.sendMessage(SAFARI_PLAYER_CURRENTLY_ENLISTED.replace("?1", currentSafari));
		}
	}

	public void stopSafari(Player player) {
		playerConfig.set("registered_players."+player.getName()+".safari",null);
		playerConfig.set("registered_players."+player.getName()+".mobs_killed",0);
		savePlayerConfig();
		reloadPlayerConfig();
		player.sendMessage(SAFARI_END_SUCCESS);
	}
	
	// to be called upon fulfilling the Safari-Goal
	public void fulfillSafari(Player player) {
		reloadPlayerConfig();
		
		// increase points
		Integer currentSafariPoints = playerConfig.getInt("registered_players."+player.getName()+".current_safari_points");
		if ( currentSafariPoints == null ) {
			currentSafariPoints = 0;
		}
		currentSafariPoints++;
		playerConfig.set("registered_players."+player.getName()+".current_safari_points",currentSafariPoints);
		savePlayerConfig();
		reloadPlayerConfig();
		player.sendMessage(SAFARI_INFO_POINTS.replace("?1",currentSafariPoints.toString()));
		
		// unregister Safari
		stopSafari(player);
	}
	
	public void addWorld(CommandSender sender, String world) {
		Configuration config = this.getConfig();
		String configKey = "safari.enabled_worlds";
		List<String> enabledWorlds = config.getStringList(configKey);
		enabledWorlds.add(world);
		config.set(configKey, enabledWorlds);
		saveConfig();
		reloadConfig();		
	}
	
	public void removeWorld(CommandSender sender, String world) {
		Configuration config = this.getConfig();
		String configKey = "safari.enabled_worlds";
		List<String> enabledWorlds = config.getStringList(configKey);
		enabledWorlds.remove(world);
		config.set(configKey, enabledWorlds);
		saveConfig();
		reloadConfig();		
	}
	
	public void reloadPlayerConfig() {
        if (playerConfigFile == null) {
        	playerConfigFile = new File(getDataFolder(), playerConfigFileName);
        }
        playerConfig = YamlConfiguration.loadConfiguration(playerConfigFile);
        // Look for defaults in the jar
        InputStream playerConfigStream = this.getResource(playerConfigFileName);
        if (playerConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(playerConfigStream);
            playerConfig.setDefaults(defConfig);
        }
    }
	
	public FileConfiguration getPlayerConfig() {
        if (playerConfig == null) {
            this.reloadPlayerConfig();
        }
        return playerConfig;
    }
	
	
	public void savePlayerConfig() {
        if (playerConfig == null || playerConfigFile == null) {
        return;
        }
        try {
        	getPlayerConfig().save(playerConfigFile);
        } catch (IOException ex) {
            this.getLogger().log(Level.SEVERE, "Could not save config to " + playerConfigFile, ex);
        }
    }

}
