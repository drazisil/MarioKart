package net.stormdev.mario.server;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.stormdev.mario.mariokart.MarioKart;
import net.stormdev.mario.races.MarioKartRaceEndEvent;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.server.ServerListPingEvent;

public class ServerListener implements Listener {
	private FullServerManager fsm;
	public ServerListener(){
		this.fsm = FullServerManager.get();
	}
	
	@EventHandler
	void entityDamage(EntityDamageByEntityEvent event){ //Not part of MK
		event.setDamage(0);
		event.setCancelled(true);
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	void respawn(PlayerRespawnEvent event){
		if(fsm.getStage().equals(ServerStage.WAITING) || fsm.getStage().equals(ServerStage.STARTING)){
			event.setRespawnLocation(fsm.lobbyLoc);
		}
	}
	
	@EventHandler
	void onPing(ServerListPingEvent event){
		event.setMotd("Stage: "+fsm.getMOTD());
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	void disconnect(PlayerQuitEvent event){
		event.setQuitMessage(null);
		Player player = event.getPlayer();
		if(player.getVehicle() != null){
			player.getVehicle().eject();
			player.getVehicle().remove();
		}
		if(fsm.voter != null){
			fsm.voter.removePlayerFromBoard(player);
		}
	}
	
	
	@EventHandler(priority = EventPriority.MONITOR)
	void playerJoin(PlayerJoinEvent event){
		event.setJoinMessage(null);
		final Player player = event.getPlayer();
		if(!fsm.getStage().getAllowJoin()){
			player.sendMessage(ChatColor.RED+"Unable to join server at this time!");
			Bukkit.getScheduler().runTaskLater(MarioKart.plugin, new Runnable(){

				@Override
				public void run() {
					fsm.sendToLobby(player);
					return;
				}}, 5*20l);
			return;
		}
		
		player.sendMessage(ChatColor.BOLD+""+ChatColor.GOLD+"------------------------------");
		player.sendMessage(ChatColor.DARK_RED+"Welcome to MarioKart, "+ChatColor.WHITE+player.getName()+ChatColor.DARK_RED+"!");
		player.sendMessage(ChatColor.BOLD+""+ChatColor.GOLD+"------------------------------");
		
		//Enable resource pack for them:
		String rl = MarioKart.plugin.packUrl;                           //Send them the download url, etc for if they haven't get server RPs enabled
		player.sendMessage(MarioKart.colors.getInfo()
				+ MarioKart.msgs.get("resource.download"));
		String msg = MarioKart.msgs.get("resource.downloadHelp");
		msg = msg.replaceAll(Pattern.quote("%url%"),
				Matcher.quoteReplacement(ChatColor.RESET + ""));
		player.sendMessage(MarioKart.colors.getInfo() + msg);
		player.sendMessage(rl); //new line
		
		if(!MarioKart.plugin.resourcedPlayers.contains(player.getName()) //Send them the RP for if they have got server RPs enabled
				&& MarioKart.plugin.fullPackUrl != null
				&& MarioKart.plugin.fullPackUrl.length() > 0){
			Bukkit.getScheduler().runTaskLater(MarioKart.plugin, new Runnable(){

				@Override
				public void run() {
					player.setTexturePack(MarioKart.plugin.fullPackUrl);
					MarioKart.plugin.resourcedPlayers.add(player.getName());
					return;
				}}, 20l);
		}
		
		final Location spawnLoc = fsm.lobbyLoc;
		if(player.getVehicle() != null){
			player.getVehicle().eject();
			player.getVehicle().remove();
			Bukkit.getScheduler().runTaskLater(MarioKart.plugin, new Runnable(){

				@Override
				public void run() {
					player.teleport(spawnLoc);
					return;
				}}, 2l);
		}
		else {
			player.teleport(spawnLoc);
		}
		
		if(fsm.getStage().equals(ServerStage.WAITING)){
			fsm.voter.addPlayerToBoard(player);
			Bukkit.getScheduler().runTaskLater(MarioKart.plugin, new Runnable(){

				@Override
				public void run() {
					player.sendMessage(ChatColor.BOLD+""+ChatColor.DARK_RED+"------------------------------");
					player.sendMessage(fsm.voter.getHelpString());
					player.sendMessage(fsm.voter.getAvailTracksString());
					player.sendMessage(ChatColor.BOLD+""+ChatColor.DARK_RED+"------------------------------");
					return;
				}}, 2l);
		}
		else if(fsm.getStage().equals(ServerStage.STARTING)){
			player.sendMessage(ChatColor.BOLD+""+ChatColor.DARK_RED+"------------------------------");
			player.sendMessage(ChatColor.GOLD+"Game starting in under 10 seconds...");
			player.sendMessage(ChatColor.BOLD+""+ChatColor.DARK_RED+"------------------------------");
		}
	}
	
	@EventHandler
	public void raceEnding(MarioKartRaceEndEvent event){
		//Reset game
		fsm.changeServerStage(ServerStage.RESTARTING);
		//wait...
		Bukkit.getScheduler().runTaskLater(MarioKart.plugin, new Runnable(){

			@Override
			public void run() {
				Player[] online = Bukkit.getOnlinePlayers();
				for(Player p:online){
					fsm.sendToLobby(p);
				}
				Bukkit.getScheduler().runTaskLater(MarioKart.plugin, new Runnable(){

					@Override
					public void run() {
						fsm.changeServerStage(ServerStage.WAITING);
						return;
					}}, 10*20l);
				return;
			}}, 10*20l);
	}
}