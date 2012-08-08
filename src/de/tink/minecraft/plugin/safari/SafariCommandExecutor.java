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

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class SafariCommandExecutor implements org.bukkit.command.CommandExecutor {
	private SafariPlugin plugin;
	private Configuration pluginConfig;
	private Configuration playerConfig;
	private Configuration groupsConfig;

	private String SAFARI_COMMAND_NO_PERMISSION = "No permission for this command!";
	private String SAFARI_NOT_FOUND = "Safari ?1 not found.";
	
	private String SAFARI_WORLD_ADDED_SUCCESS =  "World \"?1\" is now activated for safaris.";
	private String SAFARI_WORLD_ADD_ALREADY_REGISTERD =  "World \"?1\" is already activated for safaris.";
	private String SAFARI_WORLD_REMOVED_SUCCESS = "World \"?1\" is now deactivated for safaris.";
	private String SAFARI_WORLD_REMOVE_NOT_REGISTERED = "World \"?1\" is not activated for safaris.";
	private String SAFARI_WORLD_REGISTER_NOT_FOUND = "World \"?1\" not found. Check spelling.";
	
	private String SAFARI_PLAYER_LIST_AVAILABLE_SAFARIS = "You may register for the following safaris:";
	private String SAFARI_PLAYER_CURRENT_SAFARI = "You are currently registered for the safari \"?1\".";
	private String SAFARI_PLAYER_KILL_PROGRESS = "You have killed ?1/?2 mobs so far.";
	private String SAFARI_PLAYER_CURRENT_POINTS = "You have gathered ?1 safari-points so far.";
	private String SAFARI_PLAYER_NOT_ENOUGH_POINTS = "You are not eligible for this safari. You need at least ?1 safari-points to register for this safari.";
	private String SAFARI_PLAYER_NONE_REGISTERED = "You are currently not enlisted for a safari.";
	
	private String SAFARI_RECORDS_HEADER = "These are the current safari-records (Player and record-time)";
	private String SAFARI_RECORD_ENTRY = ChatColor.BLUE+"<SAFARI>"+ChatColor.WHITE+": "+ChatColor.RED+"<RECORDTIME>"+ChatColor.WHITE+" by "+ChatColor.RED+"<PLAYER>";
	private String SAFARI_NO_RECORD_AVAILABLE = ChatColor.BLUE+"?1"+ChatColor.WHITE+": No record available yet";
	
	
	private String SAFARI_GROUP_STARTED = "You successfully created a safari-group. Players now can join your group by "+ChatColor.BLUE+"/safari join <PLAYER>."+ChatColor.WHITE+"You can also invite other players:"+ChatColor.BLUE+"/safari invite <Playername>";
	private String SAFARI_GROUP_DISBANDED = "The safari-group was successfully disbanded.";
	private String SAFARI_GROUP_NOT_LEADER = "You are not the current group-leader.";
	private String SAFARI_GROUP_LEAD_NOT_TRANSFERRABLE = "You cannot transfer the group-lead to ?1 (is not in your group)";
	private String SAFARI_GROUP_NOT_FOUND = "Group \"?1\" not found, joining this group is not possible";
	private String SAFARI_GROUP_NEW_LEADER = "<PLAYER> is now the new group-leader.";

	public SafariCommandExecutor(SafariPlugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!command.getName().equalsIgnoreCase("safari")) {
			return false;
		}
		pluginConfig = plugin.getConfig();
		playerConfig = plugin.getPlayerConfig();
		groupsConfig = plugin.getGroupsConfig();
		boolean isGroupLead = false;
		boolean isGroupMember = false;
		String memberInGroup = null;

		// List Safaris
		if (args != null && args.length >= 1 && "list".equals(args[0]) ) {
			Set<String> safaris = pluginConfig.getConfigurationSection("safaris").getKeys(false);
			sender.sendMessage(SAFARI_PLAYER_LIST_AVAILABLE_SAFARIS);
			Integer currentSafariPoints = playerConfig.getInt("registered_players." + sender.getName()+ ".current_safari_points");
			if (currentSafariPoints == null) {
				currentSafariPoints = 0;
			}
			for (String safari : safaris) {
				Integer necessarySafariPoints = pluginConfig.getInt("safaris."+ safari + ".required_points");
				if (necessarySafariPoints == null) {
					necessarySafariPoints = 0;
				}
				if (necessarySafariPoints <= currentSafariPoints) {
					sender.sendMessage(safari+ ": "+ pluginConfig.getString("safaris." + safari+ ".description"));
				}
			}
			return true;
		}

		// get current safari-status for player
		if (args != null && args.length == 1 && "info".equals(args[0]) && sender instanceof Player ) {
			String currentSafari = playerConfig.getString("registered_players."+ sender.getName() + ".safari");
			Integer currentSafariMobsToKill = playerConfig.getInt("registered_players."+sender.getName()+".mobs_to_kill");
			if ( currentSafariMobsToKill == null ) {
				currentSafariMobsToKill = 0;
			}
			Integer currentSafariMobsKilled = playerConfig.getInt("registered_players."+sender.getName()+".mobs_killed");
			if ( currentSafariMobsKilled == null ) {
				currentSafariMobsKilled = 0;
			}
			if ( currentSafari != null) {				
				sender.sendMessage(SAFARI_PLAYER_CURRENT_SAFARI.replace("?1", currentSafari));
				sender.sendMessage(SAFARI_PLAYER_KILL_PROGRESS.replace("?1", currentSafariMobsKilled.toString()).replace("?2", currentSafariMobsToKill.toString()));
			} else if (currentSafari == null) {
				sender.sendMessage(SAFARI_PLAYER_NONE_REGISTERED);
			}
			Integer currentSafariPoints = playerConfig.getInt("registered_players." + sender.getName()+ ".current_safari_points");
			if (currentSafariPoints == null) {
				currentSafariPoints = 0;
			}
			sender.sendMessage(SAFARI_PLAYER_CURRENT_POINTS.replace("?1", currentSafariPoints.toString()));
			return true;
		}

		// Start Safari
		if (args != null && args.length == 2 && "start".equals(args[0])  && sender instanceof Player ) {
			ConfigurationSection safariConfig = pluginConfig.getConfigurationSection("safaris." + args[1]);
			if ( safariConfig == null ) {
				sender.sendMessage(SAFARI_NOT_FOUND.replace("?1",args[1]));
			}
			Integer currentSafariPoints = playerConfig.getInt("registered_players." + sender.getName()+ ".current_safari_points");
			if (currentSafariPoints == null) {
				currentSafariPoints = 0;
			}
			Integer necessarySafariPoints = pluginConfig.getInt("safaris."+ args[1] + ".required_points");
			if (necessarySafariPoints == null) {
				necessarySafariPoints = 0;
			}
			if (safariConfig != null && sender instanceof Player
					&& necessarySafariPoints <= currentSafariPoints) {
				plugin.startSafari((Player) sender, args[1]);
			}
			if (safariConfig != null && sender instanceof Player && necessarySafariPoints > currentSafariPoints) {
				sender.sendMessage(SAFARI_PLAYER_NOT_ENOUGH_POINTS.replace("?1", necessarySafariPoints.toString()));
			}
			return true;
		}

		// Stop Safari
		if (args != null && args.length == 1 && "stop".equals(args[0]) && sender instanceof Player ) {
			plugin.stopSafari((Player) sender);
			return true;
		}
		
		// Show safari-records
		// to current player (if command is sent by a player or to another online-player (if command is sent by console)
		if (args != null && args.length >= 1 && "showrecords".equals(args[0]) ) {
			if ( sender instanceof Player ) {
				sender.sendMessage(SAFARI_RECORDS_HEADER);
			} else if ( !( sender instanceof Player ) && args[1] != null ) {
				Player toPlayer = plugin.getServer().getPlayer(args[1]);
				if ( toPlayer.isOnline() ) {
					toPlayer.sendMessage(SAFARI_RECORDS_HEADER);
				}
			}
			
			Set<String> safaris = pluginConfig.getConfigurationSection("safaris").getKeys(false);
			for ( String safari : safaris ) {
				String currentRecordHolder = pluginConfig.getString("safaris."+safari+".current_recordholder");
				Long currentRecordTime = pluginConfig.getLong("safaris."+safari+".current_recordtime");
				int minutes = (int) ((currentRecordTime / (1000*60)) % 60);
				int hours   = (int) ((currentRecordTime / (1000*60*60)) % 24);
				String durationString = hours+":"+minutes;
				String toSend = "";
				if ( currentRecordHolder != null && currentRecordTime != null ) {
					toSend = SAFARI_RECORD_ENTRY.replace("<SAFARI>",safari).replace("<RECORDTIME>",durationString).replace("<PLAYER>", currentRecordHolder);
				} else {
					toSend = SAFARI_NO_RECORD_AVAILABLE.replace("?1", safari);
				}
				if ( sender instanceof Player ) {
					sender.sendMessage(toSend);
				} else if ( !( sender instanceof Player ) && args[1] != null ) {
					Player toPlayer = plugin.getServer().getPlayer(args[1]);
					if ( toPlayer.isOnline() ) {
						toPlayer.sendMessage(toSend);
					}
				}
			}
			return true;
		}
		
		// Group-Commands Start
		
		// First let´s see if the sender is already a group-leader or a group-member
		ConfigurationSection groupLeaderSection = groupsConfig.getConfigurationSection("groups");
		if ( groupLeaderSection != null ) {
			Set<String> groupLeaders = groupLeaderSection.getKeys(false);
			for ( String groupLeader : groupLeaders ) {
				if ( sender instanceof Player && groupLeader.equals(sender.getName())) {
					isGroupLead = true;
				}
				List<String> groupMembers = groupsConfig.getStringList("groups."+groupLeader+".members");
				if ( groupMembers != null ) {
					for ( String groupMember : groupMembers ) {
						if ( sender instanceof Player && groupMember.equals(sender.getName())) {
							isGroupMember = true;
							memberInGroup = groupLeader;
						}
					}
				}
			}
		}
		
		
		if (args != null && args.length == 1 && "start_group".equals(args[0]) && sender instanceof Player ) {
			plugin.startGroup(sender);
			sender.sendMessage(SAFARI_GROUP_STARTED.replace("<PLAYER>", sender.getName()));
			return true;
		}
		
		
		
		
		if (args != null && args.length == 1 && "listgroupmembers".equals(args[0]) && sender instanceof Player ) {
			return true;
		}		
		
		// Group-Commands End
		


		/*
		 * Admin Commands below! Proceed with caution ;)
		 */

		// Enable Safaris in given world
		if (args != null && args.length == 2 && "add_world".equals(args[0]) ) {
			boolean hasPermissionToExecute = false;
			// Command must be sent by either console or sent by a player with "safari.admin_permission"
			if (!(sender instanceof Player) || (sender instanceof Player && ((Player) sender).hasPermission(pluginConfig.getString("safari.admin_permission")))) {
				hasPermissionToExecute = true;
			}
			if (!hasPermissionToExecute) {
				sender.sendMessage(SAFARI_COMMAND_NO_PERMISSION);
				return false;
			}
			List<String> enabledWorlds = pluginConfig.getStringList("safari.enabled_worlds");
			World worldToEnable = plugin.getServer().getWorld(args[1]);
			if (worldToEnable == null) {
				sender.sendMessage(SAFARI_WORLD_REGISTER_NOT_FOUND.replace("?1", args[1]));
				return false;
			}
			if (!enabledWorlds.contains(worldToEnable.getName())) {
				plugin.addWorld(sender, args[1]);
				sender.sendMessage(SAFARI_WORLD_ADDED_SUCCESS.replace("?1",args[1]));
			} else {
				sender.sendMessage(SAFARI_WORLD_ADD_ALREADY_REGISTERD.replace("?1",args[1]));
			}
			return true;
		}

		// Disable Safaris in given world
		if (args != null && args.length == 2 && "remove_world".equals(args[0]) ) {
			boolean hasPermissionToExecute = false;
			// Command must be sent by either console or sent by a player with "safari.admin_permission"
			if (!(sender instanceof Player)
					|| (sender instanceof Player && ((Player) sender).hasPermission(pluginConfig.getString("safari.admin_permission")))) {
				hasPermissionToExecute = true;
			}
			if (!hasPermissionToExecute) {
				sender.sendMessage(SAFARI_COMMAND_NO_PERMISSION);
				return false;
			}
			List<String> enabledWorlds = pluginConfig.getStringList("safari.enabled_worlds");
			World worldToRemove = plugin.getServer().getWorld(args[1]);
			if (worldToRemove == null) {
				sender.sendMessage(SAFARI_WORLD_REGISTER_NOT_FOUND.replace("?1", args[1]));
				return false;
			}
			if (enabledWorlds.contains(worldToRemove.getName())) {
				plugin.removeWorld(sender, args[1]);
				sender.sendMessage(SAFARI_WORLD_REMOVED_SUCCESS.replace("?1", args[1]));
			} else {
				sender.sendMessage(SAFARI_WORLD_REMOVE_NOT_REGISTERED.replace("?1", args[1]));
			}
			return true;
		}
		return false;
	}

}
