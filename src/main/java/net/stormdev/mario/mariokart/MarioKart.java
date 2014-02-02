package net.stormdev.mario.mariokart;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import net.milkbowl.vault.economy.Economy;
import net.stormdev.mario.commands.AdminCommandExecutor;
import net.stormdev.mario.commands.RaceCommandExecutor;
import net.stormdev.mario.commands.RaceTimeCommandExecutor;
import net.stormdev.mario.config.PluginConfigurator;
import net.stormdev.mario.events.HotbarEventsListener;
import net.stormdev.mario.events.QueueEventsListener;
import net.stormdev.mario.events.RaceEventsListener;
import net.stormdev.mario.events.ServerEventsListener;
import net.stormdev.mario.events.SignEventsListener;
import net.stormdev.mario.events.TrackEventsListener;
import net.stormdev.mario.hotbar.HotBarManager;
import net.stormdev.mario.hotbar.HotBarUpgrade;
import net.stormdev.mario.lesslag.DynamicLagReducer;
import net.stormdev.mario.powerups.PowerupManager;
import net.stormdev.mario.queues.RaceQueue;
import net.stormdev.mario.queues.RaceQueueManager;
import net.stormdev.mario.queues.RaceScheduler;
import net.stormdev.mario.races.Race;
import net.stormdev.mario.races.RaceMethods;
import net.stormdev.mario.shop.Shop;
import net.stormdev.mario.shop.Unlockable;
import net.stormdev.mario.shop.UnlockableManager;
import net.stormdev.mario.signUtils.SignManager;
import net.stormdev.mario.sound.MusicManager;
import net.stormdev.mario.tracks.RaceTimes;
import net.stormdev.mario.tracks.RaceTrackManager;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import com.rosaloves.bitlyj.Bitly;
import com.rosaloves.bitlyj.Url;
import com.useful.uCarsAPI.uCarsAPI;
import com.useful.ucars.Colors;
import com.useful.ucars.ucars;

public class MarioKart extends JavaPlugin {
	public YamlConfiguration lang = new YamlConfiguration();
	public static MarioKart plugin;
	public static FileConfiguration config = new YamlConfiguration();
	public static Colors colors;
	public static CustomLogger logger = null;
	public static ucars ucars = null;
	public AdminCommandExecutor adminCommandExecutor = null;
	public RaceCommandExecutor raceCommandExecutor = null;
	public RaceTimeCommandExecutor raceTimeCommandExecutor = null;
	public List<Listener> listeners = null;
	public RaceTrackManager trackManager = null;
	public RaceScheduler raceScheduler = null;
	public ConcurrentHashMap<String, LinkedHashMap<UUID, RaceQueue>> queues = new ConcurrentHashMap<String, LinkedHashMap<UUID, RaceQueue>>();
	public RaceQueueManager raceQueues = null;
	public static Lang msgs = null;
	public RaceMethods raceMethods = null;
	public Random random = null;
	public static PowerupManager powerupManager = null;
	public RaceTimes raceTimes = null;
	public String packUrl = "";
	public HotBarManager hotBarManager = null;
	public double checkpointRadiusSquared = 10.0;
	public List<String> resourcedPlayers = new ArrayList<String>();
	
	public MusicManager musicManager = null;
	
	public static boolean dynamicLagReduce = true;

	Map<String, Unlockable> unlocks = null;

	public UnlockableManager upgradeManager = null;
	public SignManager signManager = null;

	public BukkitTask lagReducer = null;

	public static Boolean vault = false;
	public static Economy economy = null;

	private void setupCmds(){
		adminCommandExecutor = new AdminCommandExecutor(this);
		raceCommandExecutor = new RaceCommandExecutor(this);
		raceTimeCommandExecutor = new RaceTimeCommandExecutor(this);
		
		getCommand("marioRaceAdmin").setExecutor(adminCommandExecutor);
		getCommand("race").setExecutor(raceCommandExecutor);
		getCommand("racetimes").setExecutor(raceTimeCommandExecutor);
		
	}
	
	private void setupListeners(){
		listeners = new ArrayList<Listener>();
		
		listeners.add(new HotbarEventsListener(this));
		listeners.add(new QueueEventsListener(this));
		listeners.add(new RaceEventsListener(this));
		listeners.add(new ServerEventsListener(this));
		listeners.add(new SignEventsListener(this));
		listeners.add(new TrackEventsListener(this));
	}
	
	
	@Override
	public void onEnable() {
		System.gc();
		if (listeners != null || logger != null
				|| msgs != null || powerupManager != null || economy != null) {
			getLogger().log(Level.WARNING,
					"Previous plugin instance found, performing clearup...");
			listeners = null;
			logger = null;
			msgs = null;
			powerupManager = null;
			vault = null;
			economy = null;
		}
		random = new Random();
		plugin = this;
		File queueSignFile = new File(getDataFolder().getAbsolutePath()
				+ File.separator + "Data" + File.separator + "queueSigns.signData");
		File langFile = new File(getDataFolder().getAbsolutePath()
				+ File.separator + "lang.yml");
		if (langFile.exists() == false || langFile.length() < 1) {
			try {
				langFile.createNewFile();
				// newC.save(configFile);
			} catch (IOException e) {
			}

		}
		try {
			lang.load(langFile);
		} catch (Exception e1) {
			getLogger().log(Level.WARNING,
					"Error creating/loading lang file! Regenerating..");
		}
		msgs = new Lang(this);
		if (new File(getDataFolder().getAbsolutePath() + File.separator
				+ "config.yml").exists() == false
				|| new File(getDataFolder().getAbsolutePath() + File.separator
						+ "config.yml").length() < 1) {
			getDataFolder().mkdirs();
			File configFile = new File(getDataFolder().getAbsolutePath()
					+ File.separator + "config.yml");
			try {
				configFile.createNewFile();
			} catch (IOException e) {
			}
			copy(getResource("marioKartConfigHeader.yml"), configFile);
		}
		config = getConfig();
		logger = new CustomLogger(getServer().getConsoleSender(), getLogger());
		try {
			// Setup the Lang file
			if (!lang.contains("error.memoryLockdown")) {
				lang.set("error.memoryLockdown",
						"Operation failed due to lack of System Memory!");
			}
			if (!lang.contains("general.disabled")) {
				lang.set("general.disabled",
						"Error: Disabled");
			}
			if (!lang.contains("general.cmd.leave.success")) {
				lang.set("general.cmd.leave.success",
						"Successfully left %name%!");
			}
			if (!lang.contains("general.cmd.page")) {
				lang.set("general.cmd.page", "Page [%page%/%total%]:");
			}
			if (!lang.contains("general.cmd.full")) {
				lang.set("general.cmd.full",
						"There are no race tracks available!");
			}
			if (!lang.contains("general.cmd.overflow")) {
				lang.set("general.cmd.overflow",
						"Queues/Tracks are full, joining new low-priority queue!");
			}
			if (!lang.contains("general.cmd.playersOnly")) {
				lang.set("general.cmd.playersOnly",
						"This command is for players only!");
			}
			if (!lang.contains("general.cmd.leave.fail")) {
				lang.set("general.cmd.leave.fail", "You aren't in a game/que!");
			}
			if (!lang.contains("general.cmd.setlaps.success")) {
				lang.set("general.cmd.setlaps.success",
						"Successfully set laps for track %name%!");
			}
			if (!lang.contains("general.cmd.delete.success")) {
				lang.set("general.cmd.delete.success",
						"Successfully deleted track %name%!");
			}
			if (!lang.contains("general.cmd.delete.exists")) {
				lang.set("general.cmd.delete.exists",
						"That track doesn't exist!");
			}
			if (!lang.contains("general.cmd.racetimes")) {
				lang.set("general.cmd.racetimes",
						"Top %n% times for track %track%:");
			}
			if (!lang.contains("general.shop.notEnoughMoney")) {
				lang.set("general.shop.notEnoughMoney",
						"You don't have enough %currency% for that item!");
			}
			if (!lang.contains("general.shop.maxUpgrades")) {
				lang.set("general.shop.maxUpgrades",
						"You are not allowed to own more than 64 of an upgrade!");
			}
			if (!lang.contains("general.shop.success")) {
				lang.set(
						"general.shop.success",
						"Successfully bought %name% for %price% %currency%! You now have %balance% %currency%!");
			}
			if (!lang.contains("general.shop.sellSuccess")) {
				lang.set("general.shop.sellSuccess",
						"Successfully removed %amount% of %name% from your upgrades list!");
			}
			if (!lang.contains("general.shop.error")) {
				lang.set("general.shop.error",
						"An error occured. Please contact a member of staff. (No economy found)");
			}
			if (!lang.contains("setup.create.exists")) {
				lang.set("setup.create.exists",
						"This track already exists! Please do /urace delete %name% before proceeding!");
			}
			if (!lang.contains("setup.create.start")) {
				lang.set("setup.create.start", "Wand: %id% (%name%)");
			}
			if (!lang.contains("setup.create.lobby")) {
				lang.set("setup.create.lobby",
						"Stand in the lobby and right click anywhere with the wand");
			}
			if (!lang.contains("setup.create.exit")) {
				lang.set("setup.create.exit",
						"Stand at the track exit and right click anywhere with the wand");
			}
			if (!lang.contains("setup.create.grid")) {
				lang.set(
						"setup.create.grid",
						"Stand where you want a car to start the race and right click anywhere (Without the wand). Repeat for all the starting positions. When done, right click anywhere with the wand");
			}
			if (!lang.contains("setup.create.checkpoints")) {
				lang.set(
						"setup.create.checkpoints",
						"Stand at each checkpoint along the track (Checkpoint 10x10 radius) and right click anywhere (Without the wand). Repeat for all checkpoints. When done, right click anywhere with the wand");
			}
			if (!lang.contains("setup.create.notEnoughCheckpoints")) {
				lang.set("setup.create.notEnoughCheckpoints",
						"You must have at least 3 checkpoints! You only have: %num%");
			}
			if (!lang.contains("setup.create.line1")) {
				lang.set(
						"setup.create.line1",
						"Stand at one end of the start/finish line and right click anywhere with the wand");
			}
			if (!lang.contains("setup.create.line2")) {
				lang.set(
						"setup.create.line2",
						"Stand at the other end of the start/finish line and right click anywhere with the wand");
			}
			if (!lang.contains("setup.create.done")) {
				lang.set("setup.create.done",
						"Successfully created Race Track %name%!");
			}
			if (!lang.contains("setup.fail.queueSign")) {
				lang.set("setup.fail.queueSign",
						"That track doesn't exist!");
			}
			if (!lang.contains("setup.create.queueSign")) {
				lang.set("setup.create.queueSign",
						"Successfully registered queue sign!");
			}
			if (!lang.contains("race.que.existing")) {
				lang.set("race.que.existing",
						"You are already in a game/que! Please leave it before joining this one!");
			}
			if (!lang.contains("race.que.other")) {
				lang.set("race.que.other",
						"Unavailable! Current queue race type: %type%");
			}
			if (!lang.contains("race.que.full")) {
				lang.set("race.que.full", "Race que full!");
			}
			if (!lang.contains("race.que.success")) {
				lang.set("race.que.success", "In Race Que!");
			}
			if (!lang.contains("race.que.joined")) {
				lang.set("race.que.joined", " joined the race que!");
			}
			if (!lang.contains("race.que.left")) {
				lang.set("race.que.left", " left the race que!");
			}
			if (!lang.contains("race.que.players")) {
				lang.set(
						"race.que.players",
						"Acquired minimum players for race! Waiting %time% seconds for additional players to join...");
			}
			if (!lang.contains("race.que.preparing")) {
				lang.set("race.que.preparing", "Preparing race...");
			}
			if (!lang.contains("race.que.starting")) {
				lang.set("race.que.starting", "Race starting in...");
			}
			if (!lang.contains("resource.download")) {
				lang.set("resource.download", "Downloading resources...");
			}
			if (!lang.contains("resource.downloadHelp")) {
				lang.set("resource.downloadHelp",
						"If the resources aren't downloaded automatically. Download it at: %url%");
			}
			if (!lang.contains("resource.clear")) {
				lang.set("resource.clear",
						"Switching back to default minecraft textures...");
			}
			if (!lang.contains("race.que.go")) {
				lang.set("race.que.go", "Go!");
			}
			if (!lang.contains("race.end.timeLimit")) {
				lang.set("race.end.timeLimit", "Time Limit exceeded!");
			}
			if (!lang.contains("race.end.won")) {
				lang.set("race.end.won", " won the race!");
			}
			if (!lang.contains("race.end.rewards")) {
				lang.set("race.end.rewards",
						"&6+&a%amount%&6 %currency% for %position%! You now have %balance% %currency%!");
			}
			if (!lang.contains("race.end.time")) {
				lang.set("race.end.time", "Your time was %time% seconds!");
			}
			if (!lang.contains("race.mid.miss")) {
				lang.set("race.mid.miss",
						"You missed a section of the track! Please go back and do it!");
			}
			if (!lang.contains("race.mid.backwards")) {
				lang.set("race.mid.backwards", "You are going the wrong way!");
			}
			if (!lang.contains("race.mid.lap")) {
				lang.set("race.mid.lap", "Lap [%lap%/%total%]");
			}
			if (!lang.contains("race.end.soon")) {
				lang.set("race.end.soon",
						"You have 1 minute before the race ends!");
			}
			if (!lang.contains("race.end.position")) {
				lang.set("race.end.position", "You finished %position%!");
			}
			if (!lang.contains("race.upgrades.use")) {
				lang.set("race.upgrades.use", "&c[-]&6 Consumed Upgrade");
			}
			if (!lang.contains("mario.hit")) {
				lang.set("mario.hit", "You were hit by a %name%!");
			}
			// Setup the config
			PluginConfigurator.load(config); //Loads and converts configs
		} catch (Exception e) {
		}
		saveConfig();
		try {
			lang.save(langFile);
		} catch (IOException e1) {
			getLogger().info("Error parsing lang file!");
		}
		uCarsAPI.getAPI().hookPlugin(this);
		// Load the colour scheme
		colors = new Colors(config.getString("colorScheme.success"),
				config.getString("colorScheme.error"),
				config.getString("colorScheme.info"),
				config.getString("colorScheme.title"),
				config.getString("colorScheme.title"));
		logger.info("Config loaded!");
		this.checkpointRadiusSquared = Math.pow(
				config.getDouble("general.checkpointRadius"), 2);
		logger.info("Searching for uCars...");
		Plugin[] plugins = getServer().getPluginManager().getPlugins();
		Boolean installed = false;
		for (Plugin p : plugins) {
			if (p.getName().equals("uCars")) {
				installed = true;
				ucars = (com.useful.ucars.ucars) p;
			}
		}
		if (!installed) {
			logger.info("Unable to find uCars!");
			getServer().getPluginManager().disablePlugin(this);
		}
		ucars.hookPlugin(this);
		logger.info("uCars found and hooked!");
		logger.info("Searching for ProtocolLib...");
		
		setupCmds(); //Setup the commands
		setupListeners(); //Setup listeners
 		
		this.musicManager = new MusicManager(this);
		this.trackManager = new RaceTrackManager(this, new File(getDataFolder()
				+ File.separator + "Data" + File.separator
				+ "tracks.uracetracks"));
		this.raceQueues = new RaceQueueManager();
		this.raceMethods = new RaceMethods();
		this.raceScheduler = new RaceScheduler(
				config.getInt("general.raceLimit"));
		// Setup marioKart
		powerupManager = new PowerupManager(this);
		this.raceTimes = new RaceTimes(new File(getDataFolder()
				+ File.separator + "Data" + File.separator
				+ "raceTimes.uracetimes"),
				config.getBoolean("general.race.timed.log"));
		if (config.getBoolean("general.race.rewards.enable")) {
			try {
				vault = this.vaultInstalled();
				if (!setupEconomy()) {
					plugin.getLogger()
							.warning(
									"Attempted to enable rewards but Vault/Economy NOT found. Please install vault to use this feature!");
					plugin.getLogger().warning("Disabling reward system...");
					config.set("general.race.rewards.enable", false);
				}
			} catch (Exception e) {
				plugin.getLogger()
						.warning(
								"Attempted to enable rewards and shop but Vault/Economy NOT found. Please install vault to use these features!");
				plugin.getLogger().warning("Disabling reward system...");
				plugin.getLogger().warning("Disabling shop system...");
				MarioKart.config.set("general.race.rewards.enable", false);
				MarioKart.config.set("general.upgrades.enable", false);
			}
		}
		String rl = MarioKart.config.getString("mariokart.resourcePack");

		Boolean valid = true;
		try {
			new URL(rl);
		} catch (MalformedURLException e2) {
			valid = false;
		}
		if (valid && MarioKart.config.getBoolean("bitlyUrlShortner")) {
			// Shorten url
			// Generic access token: 3676e306c866a24e3586a109b9ddf36f3d177556
			Url url = Bitly
					.as("storm345", "R_b0fae26d68750227470cd06b23be70b7").call(
							Bitly.shorten(rl));
			this.packUrl = url.getShortUrl();
		} else {
			this.packUrl = rl;
		}
		this.upgradeManager = new UnlockableManager(new File(getDataFolder()
				.getAbsolutePath()
				+ File.separator
				+ "Data"
				+ File.separator
				+ "upgradesData.mkdata"),
				config.getBoolean("general.upgrades.useSQL"));
		this.hotBarManager = new HotBarManager(config.getBoolean("general.upgrades.enable"));
		this.lagReducer = getServer().getScheduler().runTaskTimer(this,
				new DynamicLagReducer(), 100L, 1L);
		
		this.signManager = new SignManager(queueSignFile);
		dynamicLagReduce = config.getBoolean("general.optimiseAtRuntime");
		
		if(!dynamicLagReduce){
			logger.info(ChatColor.RED+"[WARNING] The plugin's self optimisation has been disabled,"
					+ " this is risky as if one config value isn't set optimally - MarioKart has a chance"
					+ " of crashing your server! I recommend you turn it back on!");
			try {
				Thread.sleep(1000); //Show it to then for 1s
			} catch (InterruptedException e) {}
		}
		
		System.gc();
		logger.info("MarioKart v" + plugin.getDescription().getVersion()
				+ " has been enabled!");
	}

	@Override
	public void onDisable() {
		if (ucars != null) {
			ucars.unHookPlugin(this);
		}
		HashMap<UUID, Race> races = new HashMap<UUID, Race>(
				this.raceScheduler.getRaces());
		for (UUID id : races.keySet()) {
			races.get(id).end(); // End the race
		}
		raceQueues.clear();
		Player[] players = getServer().getOnlinePlayers().clone();
		for (Player player : players) {
			if (player.hasMetadata("car.stayIn")) {
				player.removeMetadata("car.stayIn", plugin);
			}
		}
		this.lagReducer.cancel();
		getServer().getScheduler().cancelTasks(this);
		System.gc();
		try {
			Shop.getShop().destroy();
		} catch (Exception e) {
			// Shop is invalid anyway
		}
		this.upgradeManager.unloadSQL();
		logger.info("MarioKart has been disabled!");
		System.gc();
	}

	private void copy(InputStream in, File file) {
		try {
			OutputStream out = new FileOutputStream(file);
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
				// System.out.write(buf, 0, len);
			}
			out.close();
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public boolean vaultInstalled(){
		Plugin[] plugins = getServer().getPluginManager().getPlugins();
		for (Plugin p : plugins) {
			if (p.getName().equals("Vault")) {
				return true;
			}
		}
		return false;
	}
	
	public boolean setupEconomy() {
		if(!vault){
			return false;
		}
		RegisteredServiceProvider<Economy> economyProvider = getServer()
				.getServicesManager().getRegistration(
						net.milkbowl.vault.economy.Economy.class);
		if (economyProvider != null) {
			economy = economyProvider.getProvider();
		}
		return (economy != null);
	}
}
