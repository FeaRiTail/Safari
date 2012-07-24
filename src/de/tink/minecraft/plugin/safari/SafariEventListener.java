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

import java.util.List;
import java.util.Set;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

public class SafariEventListener implements Listener {
	SafariPlugin plugin;
	
	private String SAFARI_FINISHED = "Congratulations, you have successfully completed this safari!";
	private String SAFARI_KILL_COUNTS = "This kill is counting for your current safari!";
	private String SAFARI_DROPS_MESSAGES = "Your reward for the completed safari:";
	
	@EventHandler
	public void onMobKill(EntityDeathEvent deathEvent) {
		LivingEntity killedMob = deathEvent.getEntity();
		EntityType killedMobType = deathEvent.getEntityType();
		Player player = killedMob.getKiller();
		if ( player == null ) {
			return;
		}
		Configuration playerConfig = plugin.getPlayerConfig();
		Configuration safariConfig = plugin.getConfig();
		ConfigurationSection registeredPlayerSection = null;
		boolean playerIsInSafari = false;
		boolean killedByPlayer = false;
		boolean killIsInSafariTimeframe = false;
		
		if ( player != null ) {
			killedByPlayer = true;
			registeredPlayerSection = playerConfig.getConfigurationSection("registered_players."+player.getName());
		}
		if ( registeredPlayerSection != null ) {
			playerIsInSafari = true;
		}
		String currentSafari = playerConfig.getString("registered_players."+player.getName()+".safari");

		// check Safari Config for Night/Day Config
		killIsInSafariTimeframe = false;
		Long currentHourLong = (player.getWorld().getFullTime())/1000;
		Integer currentHour = (Integer) currentHourLong.intValue();
		List<String> safariHours = safariConfig.getStringList("safaris."+currentSafari+".valid_hours");
		if ( safariHours == null || ( safariHours != null && safariHours.size() == 0 ) ) {
			killIsInSafariTimeframe = true;
		} else {
			for ( String safariHour : safariHours ) {
				Integer safariHourInt = Integer.parseInt(safariHour);
				if ( safariHourInt == currentHour ) {
					killIsInSafariTimeframe = true;
				}
			}
		}	
		
		
		/*
		 *  Skip/ignore the kill if
		 *  a) the killing player is not registered for a safari
		 *  or
		 *  b) the mob was not killed by a player
		 *  or
		 *  c) the Safari is bound to a given Timeframe (e.g.: day, night, dusk, dawn) 
		 */
		if ( !killedByPlayer || !playerIsInSafari || !killIsInSafariTimeframe ) {
			return;
		}
		
		
		Integer currentSafariMobsToKill = playerConfig.getInt("registered_players."+player.getName()+".mobs_to_kill");
		Integer currentSafariMobsKilled = playerConfig.getInt("registered_players."+player.getName()+".mobs_killed");
		if ( currentSafariMobsKilled == null ) {
			currentSafariMobsKilled = 0; 
		}
		String mobKey = "safaris."+currentSafari+".types_of_mobs_to_kill";
		List<String> relevantMobs = safariConfig.getStringList(mobKey);
		boolean isRelevantMob = false;
		for (String mobToKill : relevantMobs ) {
			if ( killedMobType.getName().toLowerCase().equals(mobToKill.toLowerCase())) {
				isRelevantMob = true;
			}
		}
		
		// add 1 to mobs_killed
		if ( isRelevantMob ) {
			currentSafariMobsKilled++;
			playerConfig.set("registered_players."+player.getName()+".mobs_killed",currentSafariMobsKilled);
			player.sendMessage(SAFARI_KILL_COUNTS);
			plugin.savePlayerConfig();
			if ( currentSafariMobsKilled == currentSafariMobsToKill ) {
				player.sendMessage(SAFARI_FINISHED);
				player.sendMessage(SAFARI_DROPS_MESSAGES);
				String basePath = "safaris."+currentSafari;
				// should we add drops?
				ConfigurationSection addDropsSection = safariConfig.getConfigurationSection(basePath + ".addDrops");
				if ( addDropsSection != null ){
					Set<String> addDrops = addDropsSection.getKeys(false);
					List<ItemStack> drops = deathEvent.getDrops();
					for(String drop : addDrops) {
						String amount = plugin.getConfig().getString(basePath + ".addDrops." + drop);
						int itemAmount = parseInt(amount);
						
						if(itemAmount > 0) {
							ItemStack newDrop = new ItemStack(Integer.parseInt(drop), itemAmount);
							drops.add(newDrop);
						}
					}
				}
				plugin.fulfillSafari(player);
			}
		}
	}
	
	/*
	 * Used to determine/calculate the drop(s) for the accomplished Safari
	 * thanks to metakiwi: http://dev.bukkit.org/profiles/metakiwi/
	 * for this nice piece of code which evolved from his
	 * "LessFood" Plugin: http://dev.bukkit.org/server-mods/lessfood/
	 * 
	 */
	
	private int parseInt(String number) {
		if(number == null) return 0;
		String[] splitNumber = number.split(" "); 
		float chance=100;
		int min = -1;
		int max = -1;
		int ret;
	    int bonus = 0;
	    
		if(splitNumber.length > 1) {
			for(String partNumber : splitNumber) {
				if(partNumber.indexOf("+") == 0) {
					partNumber = partNumber.replace("+", "");
					bonus = Integer.parseInt(partNumber);
				} else if(partNumber.indexOf("%") >= 0) {
					partNumber = partNumber.replace("%", "");
					chance = Float.parseFloat(partNumber);
				} else {
					if(min > -1) {
						max = Integer.parseInt(partNumber);
					} else {
						min = max  = Integer.parseInt(partNumber);
					}
				}
			}
		} else {
			min = max = Integer.parseInt(number);
		}

		ret = (int) Math.round(Math.random() * (max - min) + min);
		if(Math.random() * 100 > 100 - chance) {
			return ret + bonus;
		} else {
			return bonus;
		}
	}
	
	SafariEventListener(SafariPlugin plugin) {
		this.plugin = plugin;
	}

}
