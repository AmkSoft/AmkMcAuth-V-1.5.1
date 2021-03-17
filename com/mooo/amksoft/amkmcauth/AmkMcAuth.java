package com.mooo.amksoft.amkmcauth;

import com.google.common.io.PatternFilenameFilter;
import com.mooo.amksoft.amkmcauth.commands.CmdChngPwd;
import com.mooo.amksoft.amkmcauth.commands.CmdLogin;
import com.mooo.amksoft.amkmcauth.commands.CmdLogout;
import com.mooo.amksoft.amkmcauth.commands.CmdRecover;
import com.mooo.amksoft.amkmcauth.commands.CmdRegister;
import com.mooo.amksoft.amkmcauth.commands.CmdSetEmail;
import com.mooo.amksoft.amkmcauth.commands.CmdAmkAuth;
import com.mooo.amksoft.amkmcauth.tools.MySQL;
import com.mooo.amksoft.amkmcauth.tools.QueueClass;

//import org.apache.commons.lang.RandomStringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
//import org.bukkit.Material;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
//import java.util.Random;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.logging.Filter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

// To publish JavaCode on bukkit.org:
// Use these tags: [syntax=java]code here[/syntax]

public class AmkMcAuth extends JavaPlugin {
	
    public static File dataFolder;
    public Config c;
    public Logger log;
    //public LogFilter MyFilter;
    private BukkitTask reminderTask = null;

    private static int MaxQueueDepth = 500; // This should be enough to handle size 10.000 player base??
    public static QueueClass MyQueue = new QueueClass(MaxQueueDepth);
    		
    private static AmkMcAuth instance; 
    public AmkMcAuth() {
    	instance = this;
    }
	public static Plugin getInstance() {
		// TODO Auto-generated method stub
		return instance;
	}
    
    private BukkitTask getCurrentReminderTask() {
        return this.reminderTask;
    }

    /**
     * Creates a PlayerConfigSave task.
     *
     * @param p Plugin to register task under
     * @return Task created
     */
    
    private BukkitTask createSaveTimer(Plugin p) {
        this.reminderTask = AmkAUtils.createSaveTimer(p);
        return this.getCurrentReminderTask();
    }

    /**
     * Creates a PlayerConfigSave task.
     *
     * @param p Plugin to register task under
     * @return Task created
     */
    
    private BukkitTask createChkVersionTimer(Plugin p) {
        this.reminderTask = AmkAUtils.createChkVersionTimer(p);
        return this.getCurrentReminderTask();
    }

    //private void createSaveTimer(AmkMcAuth amkMcAuth) {
    //    AmkAUtils.createSaveTimer(this);		
	//}

    /**
     * Registers a command in the server. If the command isn't defined in plugin.yml
     * the NPE is caught, and a warning line is sent to the console.
     *
     * @param ce      CommandExecutor to be registered
     * @param command Command name as specified in plugin.yml
     * @param jp      Plugin to register under
     */
    private void registerCommand(CommandExecutor ce, String command, JavaPlugin jp) {
        try {
            jp.getCommand(command).setExecutor(ce);
        } catch (NullPointerException e) {
            jp.getLogger().warning(String.format(AmkAUtils.colorize(Language.COULD_NOT_REGISTER_COMMAND.toString()), command, e.getMessage()));
        }
    }

    private void update() {
        final File userdataFolder = new File(dataFolder, "userdata");
        if (!userdataFolder.exists() || !userdataFolder.isDirectory()) return;
        this.getLogger().info("Checking+Updating old player profilefiles to new format (may take a long time)");
        for (String fileName : userdataFolder.list(new PatternFilenameFilter("(?i)^.+\\.yml$"))) {
            String playerName = fileName.substring(0, fileName.length() - 4); // ".yml" = 4
            try {
                //noinspection ResultOfMethodCallIgnored
                UUID.fromString(playerName);
                continue; // FileName(PlayerName) sounds like a UUID, no need to rename!!
            } catch (IllegalArgumentException ignored) {}

            //boolean Online=true;
            //UUID u;            
        	//if(Bukkit.getOnlineMode()!= Online) 
    		//{
        	//    // Server runs 'OffLine' AmkMcAuth calculates the UUID for this player...
        	//    u = UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes(Charsets.UTF_8));    		
    		//}
        	//else
        		//{            		
        		//try {
        		//	u = AmkAUtils.getUUID(playerName); // Get the official MOJANG UUID!
        		//} catch (Exception ex) {
        		//	// Oops, its not a MOJANG UUID, (or server is running in 'OffLine' mode)!!  
        		//	//u = this.getServer().getOfflinePlayer(playerName).getUniqueId(); // Depricated
            	//    u = UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes(Charsets.UTF_8));    		
        		//}
        	//}

            UUID u;
            try {
            	u = AmkAUtils.getUUID(playerName);
            } catch (Exception ex) {
            	//ex.printStackTrace();
            	u = null;        		
            }

            if (u == null) {
            	this.getLogger().warning(AmkAUtils.colorize(Language.ERROR.toString()));
            	continue;
            }

            // Original FileRename Code does not work, this one (below) works
     		File origFile = new File(userdataFolder.toString() + File.separator + fileName);		
       		File destFile = new File(userdataFolder.toString() + File.separator + u + ".yml");
            if (!origFile.exists()){
                this.getLogger().info("Debug: Orig-File " + origFile.toString() + " does NOT exist??");
            }
            if (destFile.exists()){
                this.getLogger().info("Debug: Dest-File " + destFile.toString() + " allready exists??");
            }
            if (origFile.renameTo(destFile)) {
                this.getLogger().info(String.format(AmkAUtils.colorize(Language.CONVERTED_USERDATA.toString()), fileName, u + ".yml"));
            } else {
                 this.getLogger().warning(String.format(AmkAUtils.colorize(Language.COULD_NOT_CONVERT_USERDATA.toString()), fileName, u + ".yml"));
            }
                
        }
    }
    
    //@SuppressWarnings("unused")
    private void updateToMySQL() {
		final File userdataFolder = new File(AmkMcAuth.dataFolder, "userdata");
		if (!userdataFolder.exists() || !userdataFolder.isDirectory()) return;

		boolean ImportError=false;
		
		long  PlayerJoin;
		long  PlayerQuit;
		long  PlayerLogin;
		String  PlayerName;
		String  PassWord;
		String  PassWordHash;
		String  IpAddress;
		String  EmAddress;
		boolean LoggedIn;
		boolean VipPlayer;
		long  Expire;

		int NoValidPlayerCount=0;
		
        this.getLogger().info("Checking+Inserting player profilefiles into MySQL (may take a long time)");
		for (String fileName : userdataFolder.list(new PatternFilenameFilter("(?i)^.+\\.yml$"))) {
			PlayerJoin=0;
			PlayerQuit=0;
			PlayerLogin=0;
			PlayerName="";
			PassWord="";
			PassWordHash="";
			IpAddress="";
			EmAddress="";
			LoggedIn=false;
			VipPlayer=false;
			Expire=0;
			
			Scanner in;

			ImportError=false;
					
			try {
				in = new Scanner(new File(userdataFolder + File.separator + fileName));
				//while (in.hasNextLine()) { // iterates each line in the file
				while (in.hasNext()) { // 1 more character?: iterates each line in the file
					String line = in.nextLine();

					// Timestamps:
					if(line.contains("  join:")) 	PlayerJoin  = Long.parseLong(line.substring(line.lastIndexOf(" ")+1));
					if(line.contains("  quit:")) 	PlayerQuit  = Long.parseLong(line.substring(line.lastIndexOf(" ")+1));
					if(line.contains("  login:"))	PlayerLogin = Long.parseLong(line.substring(line.lastIndexOf(" ")+1));

					// login:
					if(line.contains("  logged_in:") && line.contains("true")) LoggedIn=true;
					if(line.contains("  username:"))  PlayerName   = line.substring(line.lastIndexOf(" ")+1);
					if(line.contains("  password:"))  PassWord     = line.substring(line.lastIndexOf(" ")+1);
					if(line.contains("  hash:"))      PassWordHash = line.substring(line.lastIndexOf(" ")+1);
					if(line.contains("  ipaddress:")) IpAddress    = line.substring(line.lastIndexOf(" ")+1);
					if(line.contains("  emaddress:")) EmAddress    = line.substring(line.lastIndexOf(" ")+1);
					if(line.contains("  vip:") && line.contains("true")) VipPlayer=true;
					if(line.contains("godmode_expires:")) Expire = Long.parseLong(line.substring(line.lastIndexOf(" ")+1));
					
					if(PlayerName.equals("")) {
						NoValidPlayerCount++;
						PlayerName="--NoName-" + String.valueOf(NoValidPlayerCount);
					}
				}
				in.close(); // don't forget to close resource leaks
			} catch (FileNotFoundException|NumberFormatException e) {
				// TODO Auto-generated catch block
				ImportError=true;
		        this.getLogger().info("Error in file: " + userdataFolder + File.separator + fileName);
				e.printStackTrace();
			}
			if(!ImportError) {
				// THIS IMPORTS/Inserts Removed Players From Profile-files into the database
				PreparedStatement ps;
				try {
					int RecAanwezig=0;
					String PlayerUUID =fileName.split("\\.")[0]; 
					//Connection con = MySQL.getConnection();
					ps = MySQL.getConnection().prepareStatement(
							"SELECT count(*) as Aanwezig " + 
							"FROM Players " + 
							"WHERE UUID = ?" 
							);
					ps.setString(1, PlayerName); // 1 is the first "?" in the SQL string
					ResultSet rs = ps.executeQuery();
					if (rs.next() == true) { 
						RecAanwezig=rs.getInt("Aanwezig");
					}
	    	        ps.close();					
					rs.close();
					if(RecAanwezig==0) {
						ps = MySQL.getConnection().prepareStatement(
								"INSERT IGNORE INTO Players " +
								"       ( Name, UUID, Joyn, Quit, Login, LoggedIn, Password, Hash, IpAdress, EmlAdress, Vip, GodModeEx) " +
								"VALUES (  ?  ,  ?  ,  ? ,   ? ,   ?  ,     ?   ,     ?   ,   ? ,     ?    ,    ?     ,  ? ,     ?    )" 
								);
						ps.setString(   1,  PlayerName);
						ps.setString(   2,  PlayerUUID);
						ps.setLong(     3,  PlayerJoin);
						ps.setLong(     4,  PlayerQuit);
						ps.setLong(     5,  PlayerLogin);
						ps.setBoolean(  6,  LoggedIn);
						ps.setString(   7,  PassWord);
						ps.setString(   8,  PassWordHash);
						ps.setString(   9,  IpAddress);
						ps.setString(  10,  EmAddress);
						ps.setBoolean( 11,  VipPlayer);
						ps.setLong(    12,  Expire);
						ps.executeUpdate();							
			            ps.close();
					}
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}	
		}
    }

    private void saveLangFile(String name) {
        if (!new File(this.getDataFolder() + File.separator + "lang" + File.separator + name + ".properties").exists())
            this.saveResource("lang" + File.separator + name + ".properties", false);
    }

    @Override
    public void onEnable() {
        AmkMcAuth.dataFolder = this.getDataFolder();

        if (!new File(getDataFolder(), "config.yml").exists()) this.saveDefaultConfig();

        this.c = new Config(this); // Is deze nodig ??, 'c' wordt niet gebruikt??
        this.log = this.getLogger();

        this.saveLangFile("en_us");

        try {
            new Language.LanguageHelper(new File(this.getDataFolder(), this.getConfig().getString("general.language_file", "lang/en_us.properties")));
        } catch (IOException e) {
            this.log.severe("Could not load language file: " + e.getMessage());
            this.log.severe("Disabling plugin.");
            this.setEnabled(false);
            return;
        }

        if (!Config.MySqlDbHost.equals("")) MySQL.connect(); // Connect to the Database System
        
        if (Config.checkOldUserdata) {
        	this.update();
        }
    	// We only do the updateToMySQL() if MySqlDbHost is used and we are in "BacKUp" mode.
    	// The update-ProfileFiles-To-MySQL only does its trick when the table is empty thought.!!!
    	// So the existing info will not be overwritten by possible incorrect data on server-start.
    	if (!Config.MySqlDbHost.equals("") & Config.MySqlDbFile.equals("Backup")) {
    		int Aantal=0;
    		PreparedStatement ps;
    		try { // Force MySQL connection to be Active, Just doing nothing except renew MySql Connection 
    			ps = MySQL.getConnection().prepareStatement(
    					"SELECT count(*) as Aantal " +
    					"FROM   Players " );
    			ResultSet res = ps.executeQuery();
    			//Code using ResultSet entries here
    			if (res.next() == true) {
    				Aantal = res.getInt("Aantal");
    			}
    			res.close();
    			ps.close();
    		} catch (SQLException e1) {
    			// TODO Auto-generated catch block
    			e1.printStackTrace();
    		}
    		if(Aantal == 0) this.updateToMySQL();; // The update-ProfileFiles-To-MySQL only when the table is empty!!!
    	}

    	PluginManager pm = this.getServer().getPluginManager();
        pm.registerEvents(new AuthListener(this), this);
        
        this.registerCommand(new CmdAmkAuth(this), "amkmcauth", this);
        this.registerCommand(new CmdLogin(this), "login", this);
        this.registerCommand(new CmdLogout(this), "logout", this);
        this.registerCommand(new CmdRegister(this), "register", this);
        this.registerCommand(new CmdSetEmail(this), "setemail", this);
        this.registerCommand(new CmdChngPwd(this), "changepassword", this);
        this.registerCommand(new CmdRecover(this), "recoverpwd", this);

        for (Player p : this.getServer().getOnlinePlayers()) {
            AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
            if (ap.isLoggedIn()) continue;
            if (ap.isRegistered()) ap.createLoginReminder(this);
            else ap.createRegisterReminder(this);
        }

        // Create the Bukkit-Timer to save the UserData on regular intervals
        if(this.createSaveTimer(this)==null){ // Uses (see top): public BukkitTask createSaveTimer(Plugin p)
        	this.getLogger().info(ChatColor.RED + "AutoSave Task not created!");
        }
        else
        {
        	if(Config.removeAfterDays>0) 
        		this.getLogger().info("Auto removal of inactive playerdata set to " + Config.removeAfterDays +" days old!");
        	else
        		this.getLogger().info("Auto removal of inactive playerdata disabled!");
        }

    	this.getLogger().info("Counting PlayerBase, IP-Addresses and nlp-players.");
        PConfManager.countPlayersFromIpAndGetVipPlayers();
    	this.getLogger().info("Counting done, PlayerBaseCount: " + String.valueOf(PConfManager.getPlayerCount() ) +
    			                           ", Ip-AddressCount: " + String.valueOf(PConfManager.getIpaddressCount() ) +
    			                           ", nlp-PlayerCount: " + String.valueOf(PConfManager.getVipPlayerCount() ) + 
    			                           "." );
    	
    	CmdAmkAuth.CheckDevMessage(Bukkit.getConsoleSender());

        if(this.createChkVersionTimer(this)==null){ // Uses (see top): public BukkitTask createChkVersionTimer(Plugin p)
        	this.getLogger().info("Check-AmkMcAuth-Version Task not created!");
        }
        
    	if (getConfig().getBoolean("general.metrics_enabled"))
    	{
    		////-- Hidendra's Metrics --//
    		//try {
    		//	Metrics metrics = new Metrics(this);
    		//	if (!metrics.start()) this.getLogger().info(Language.METRICS_OFF.toString());
    		//	else this.getLogger().info(Language.METRICS_ENABLED.toString());
    		//} catch (Exception ignore) {
    		//	// Failed to submit the stats :-(    			
    		//	this.getLogger().warning(Language.COULD_NOT_START_METRICS.toString());
    		//}
    		
            // All you have to do is adding this line in your onEnable method:
    		//@SuppressWarnings("unused")

			//Metrics metrics = new Metrics(this);
            //this.getLogger().info(Language.METRICS_ENABLED.toString());
            
    		int pluginId = 306; // <-- Replace with the id of your plugin!
    		
			Metrics metrics = new Metrics(this,pluginId);
			
            this.getLogger().info(AmkAUtils.colorize(Language.METRICS_ENABLED.toString()));

            // Optional: Add custom charts
            metrics.addCustomChart(new Metrics.SimplePie("Registered_PlayerCount", new Callable<String>() {
            	@Override
            	public String call() throws Exception {
                	int PlayCnt=(PConfManager.getPlayerCount()/100);
            		return String.valueOf(PlayCnt*100) + "-" + String.valueOf(((PlayCnt+1)*100)-1) ;
            		//return "My value";
            	}
            }));
            
            // Optional: Add custom charts
            //metrics.addCustomChart(new Metrics.SimplePie("Appl_Usage", new Callable<String>() {
            //	@Override
            //	public String call() throws Exception {
            //   	String Usage="le+";
            //		if(Config.MySqlDbHost.equals(""))
            //			Usage = Usage + "SFLO"; // SaveFileOnly 
            //		else
            //    		if(Config.MySqlDbFile.equals(""))
            //    			Usage = Usage + "SDBO"; // SaveDBOnly
            //    		else
            //    			Usage = Usage + "SD&F"; //SaveDB+Fil 
            //
			//		return Usage;
            //		//return "My value";              
            //	}
            //}));    		

            // Optional: Add custom charts
            metrics.addCustomChart(new Metrics.AdvancedPie("Tools_Usage", new Callable<Map<String, Integer>>() {
            	@Override
            	public Map<String, Integer> call() throws Exception {
            		
            		Map<String, Integer> valueMap = new HashMap<>();

            		if(Config.MySqlDbHost.equals(""))
            			valueMap.put("SaveFileOnly", 1);
            		else
                		if(Config.MySqlDbFile.equals(""))
            				valueMap.put("DatabaseOnly", 1);
                		else
            				valueMap.put("Database+filebackup", 1);

            		
                	if(Config.registrationType.equals("password"))
                		valueMap.put("Register-Pswrd", 1);
            		else
                		valueMap.put("Register-Email", 1);

            		if(Config.checkOldUserdata)
                		valueMap.put("CheckOldUserData", 1);

            		if(Config.emailForceSet)
                		valueMap.put("Force-SetEmail", 1);

            		if(Config.sessionType.contains("HiddenChat"))
                		valueMap.put("HiddenChat", 1);
            		else
                		valueMap.put("Commands", 1);

            		if(Config.UseCapcha)
                		valueMap.put("Using-Capcha", 1);
            		
            		if(Config.ShowMenuOption)
                		valueMap.put("MenuOptionHint", 1);
            		
            		if(Config.UseAutoMenu)
                		valueMap.put("Using-AutoMenu", 1);

            		if(Config.invisibleMode)
                		valueMap.put("Login-Invisible", 1);

            		if(Config.adventureMode)
                		valueMap.put("Login-Adventure", 1);

            		if (Config.useHideInventory)
                		valueMap.put("Using-HideInventory", 1);
            			

            		if(Config.teleportToSpawn)  {
                		if(!Config.spawnWorld.equals(""))  
                    		valueMap.put("OnJoin=Tp2World", 1);
                		else
                    		valueMap.put("Tp2Spawn", 1);
                		if(Config.useSpawnAt)
                    		valueMap.put("Tp2SpawnAt", 1);
            		}

            		int Pa = 0;
            		for (String playerAction : Config.playerActionSJoin) {
            			if(!playerAction.trim().isEmpty()) Pa++; 
            		}
            		if(Pa>0) valueMap.put("PaJoinCmd", Pa);
            		
            		Pa = 0;
            		for (String playerAction : Config.playerActionSJReg) {
            			if(!playerAction.trim().isEmpty()) Pa++; 
            		}
            		if(Pa>0) valueMap.put("PaJoinRegisterCmd", Pa);
            		
            		Pa = 0;
            		for (String playerAction : Config.playerActionSJGrc) {
            			if(!playerAction.trim().isEmpty()) Pa++; 
            		}
            		if(Pa>0) valueMap.put("PaJoinGraceCmd", Pa);
            		
            		Pa = 0;
            		for (String playerAction : Config.playerActionLogin) {
            			if(!playerAction.trim().isEmpty()) Pa++; 
            		}
            		if(Pa>0) valueMap.put("PaLoginCmd", Pa);
            		
            		Pa = 0;
            		for (String playerAction : Config.playerActionLogof) {
            			if(!playerAction.trim().isEmpty()) Pa++; 
            		}
            		if(Pa>0) valueMap.put("PaLogofCmd", Pa);
            		
            		Pa = 0;
            		for (String playerAction : Config.playerActionLeave) {
            			if(!playerAction.trim().isEmpty()) Pa++; 
            		}
            		if(Pa>0) valueMap.put("PaLeaveCmd", Pa);

					return valueMap;
            	}
            }));    		

            //// Optional: Add custom charts
            //metrics.addCustomChart(new Metrics.SimplePie("Server_Type", new Callable<String>() {
            //	@Override
            //	public String call() throws Exception {
            //		return getServer().getVersion();  // Has to show like: Spigot, Paper, eg.
            //		//return getServer().getBukkitVersion();  // Has to show like: Spigot, Paper, eg.
            //	}
            //}));
            
    	}
        else
        {
        	//this.getLogger().info("Metrics on plugin disabled.");
            this.getLogger().info(AmkAUtils.colorize(Language.METRICS_OFF.toString()));
        }

    	this.log.info("Server getVersion() reports: " + getServer().getVersion());  // Has to show like: Spigot, Paper, eg.

    	
        this.log.info(this.getDescription().getName() + " v" + this.getDescription().getVersion() + " " + AmkAUtils.colorize(Language.ENABLED.toString()) + ".");

        Filter MyFilter = new Filter()
        	{
        	public boolean isLoggable(LogRecord line) {
            	//Bukkit.getServer().getLogger().info("Debug, Command-Line-Logger: " + line.getMessage());
        		Bukkit.getServer().getConsoleSender().sendMessage("Send: " +line.getMessage());
        		if (line.getMessage().contains("/login") || line.getMessage().contains("/register") || line.getMessage().contains("/pass")) {
        			return false;
        		}
        	return true;
        	}
        };        
        //Bukkit.getLogger().setFilter(MyFilter);        
        Bukkit.getServer().getLogger().setFilter(MyFilter);        
        //this.getServer().getLogger().setFilter(MyFilter);
        
        //Bukkit.getServer().getLogger().setFilter(new McFilter());
        
    }

    @Override
    public void onDisable() {
        this.getServer().getScheduler().cancelTasks(this);

        for (Player p : this.getServer().getOnlinePlayers()) {
            AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
            if (ap.isLoggedIn()) ap.logout(this, false);
            ap.RestoreSurvivalInventory(p); // Restore Inventory
        }

        PConfManager.saveAllManagers("Normal");
        PConfManager.purge();

        if (!Config.MySqlDbHost.equals("")) MySQL.disconnect(); // DisConnect from the Database System

        this.log.info(this.getDescription().getName() + " v" + this.getDescription().getVersion() + " Disabled!.");
    }

    public void CommandDoUpdate(String HowToDo) {
    	if(HowToDo.equals("HashMap"))
    		update();
    	else
    		updateToMySQL();
    }
    
	// isRegistered, forceRegister, forceLogin: API-Functions for the FastLogin Plugin
    public boolean isRegistered(String player){
        AuthPlayer ap = AuthPlayer.getAuthPlayer(player);
        if (ap == null) {
        	this.getLogger().info(ChatColor.RED + AmkAUtils.colorize(Language.ERROR_OCCURRED.toString()));
            return false;
        }
        return(ap.isRegistered());
    }

    public void forceRegister(String player, String Password) {
        AuthPlayer ap = AuthPlayer.getAuthPlayer(player);
        if (ap == null) {
        	this.getLogger().info(ChatColor.RED + AmkAUtils.colorize(Language.ERROR_OCCURRED.toString()));
            return;
        }
        if (ap.isRegistered()) {
        	this.getLogger().info(ChatColor.RED + AmkAUtils.colorize(Language.PLAYER_ALREADY_REGISTERED.toString()));
            return;
        }
        for (String disallowed : Config.disallowedPasswords) {
            if (disallowed.equals("#NoPlayerName#")) disallowed = ap.getUserName();
            if (!Password.equalsIgnoreCase(disallowed)) continue;
            this.getLogger().info(ChatColor.RED + AmkAUtils.colorize(Language.DISALLOWED_PASSWORD.toString()));
            return;
        }
        final String name = AmkAUtils.forceGetName(ap.getUniqueId());
        if (ap.setPassword(Password, Config.passwordHashType)) {
            if(name!=player) ap.setUserName(player); //name not set?, set it!
            this.getLogger().info(ChatColor.BLUE + String.format(AmkAUtils.colorize(Language.REGISTERED_SUCCESSFULLY.toString()), ChatColor.GRAY + player + ChatColor.BLUE));
        }
        else
        	this.getLogger().info(ChatColor.RED + String.format(AmkAUtils.colorize(Language.COULD_NOT_REGISTER.toString()), ChatColor.GRAY + player + ChatColor.RED));
    }
    
    public void forceLogin(String player) {
        AuthPlayer ap = AuthPlayer.getAuthPlayer(player);
        if (ap == null) {
        	this.getLogger().info(ChatColor.RED + AmkAUtils.colorize(Language.ERROR_OCCURRED.toString()));
        	return;
        }
        Player p = ap.getPlayer();
        if (p == null) {
        	this.getLogger().info(ChatColor.RED + AmkAUtils.colorize(Language.PLAYER_NOT_ONLINE.toString()));
            return;
        }
        ap.login();
        this.getLogger().info(p.getName() + " " + AmkAUtils.colorize(Language.HAS_LOGGED_IN.toString()));
    }
}

