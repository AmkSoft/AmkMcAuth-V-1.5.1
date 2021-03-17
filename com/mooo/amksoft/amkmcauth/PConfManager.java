package com.mooo.amksoft.amkmcauth;

import static java.nio.file.StandardOpenOption.*;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import java.util.logging.Logger;

//import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
//import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
//import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.google.common.io.PatternFilenameFilter;
//import com.mooo.amksoft.amkmcauth.commands.CmdAmkAuth;
import com.mooo.amksoft.amkmcauth.tools.MySQL;

public class PConfManager extends YamlConfiguration {
	
	private static final Map<UUID, PConfManager> pcms = new HashMap<>();
	private final Object saveLock = new Object();
	private File pconfl = null;
	private static String VipPlayers = " ";
	//private static String AllPlayers = "";
	private static int PlayerCount = 0;
	//private static int VersionCheckerCount = 0;

	//private static String IpAddresses[] = {};
	//private static List<String> IpAddresses = new ArrayList<String>();
	//private static List<Integer> IpAddressesCnt = new ArrayList<Integer>();

	private static String PlayerNameFile = AmkMcAuth.dataFolder + File.separator + "PlayerNames.txt";

	// 123.456.789.000+lf (=20max) + 4 bytes voor Integer-Number=RecordLength of 24.
	//private static int IpRecordlength=24; // Lengte van Records ivm FilePoiner positioning (unused)
	private static int IpAdrsCount = 0; // Move IpAddress-management to file, saves memory
	private static int IpAdrslength=20; // Length of Addres-field
	private static String IpAddressesFile = AmkMcAuth.dataFolder + File.separator + "IpAdressCnt.rnd";
	// email-address=max: 255 long+lf (=256max) + 4 bytes voor Integer-Number=RecordLength of 260.
	//private static int EmRecordlength=24; // Lengte van Records ivm FilePoiner positioning (unused)
	private static int EmAdrsCount = 0; // Move EmAddress-management to file, saves memory
	private static int EmAdrslength=255; // Length of Email-field
	private static String EmAddressesFile = AmkMcAuth.dataFolder + File.separator + "EmAdressCnt.rnd";

	private static Integer DebugSave=0;
	/**
	 * Player configuration manager
	 *
	 * @param p Player to manage
	 */
	PConfManager(OfflinePlayer p) {
		super(); // call parent constructor: (extends) YamlConfiguration
		File dataFolder = AmkMcAuth.dataFolder;
		this.pconfl = new File(dataFolder + File.separator + "userdata" + File.separator + p.getUniqueId() + ".yml");
		try {
			load(this.pconfl);
			//loadFromString(contents);
		} catch (Exception ignored) {
		}
	}

	/**
	 * Player configuration manager.
	 *
	 * @param u Player to manage
	 */
	PConfManager(UUID u) {
		super(); // call parent constructor: (extends) YamlConfiguration
		File dataFolder = AmkMcAuth.dataFolder;
		this.pconfl = new File(dataFolder + File.separator + "userdata" + File.separator + u + ".yml");
		try {
			load(this.pconfl);
		} catch (Exception ignored) {
		}	
	}

	/**
	 * No outside construction, please.
	 */
	//@SuppressWarnings("unused")
	PConfManager() {
	}

	public static PConfManager getPConfManager(Player p) {
		return PConfManager.getPConfManager(p.getUniqueId());
	}

	//public static PConfManager getPConfManager(UUID u) {
	//    synchronized (PConfManager.pcms) {
	//        if (PConfManager.pcms.containsKey(u)) return PConfManager.pcms.get(u);
	//    	final PConfManager pcm = new PConfManager(u);
	//	    PConfManager.pcms.put(u, pcm);
	//    	return pcm;
	//   }
	//}    
	public static PConfManager getPConfManager(UUID u) {
		synchronized (PConfManager.pcms) {
			if (PConfManager.pcms.containsKey(u)) return PConfManager.pcms.get(u);
			final PConfManager pcm = new PConfManager(u);

			//Logger log = AmkMcAuth.getInstance().getLogger();
			//log.info("PConfManager getPConfManager call");

			if (!Config.MySqlDbHost.equals("")) { // We use a MySQL database, witch goes FIRST !!!!
				//We have here an empty pcm structure (or filled with old Profile data is file existed)
				//Refill then structure using data from the MySQL database
				//Connection con = MySQL.getConnection();

				PreparedStatement ps;
				try {
					ps = MySQL.getConnection().prepareStatement(
							"SELECT Name    , Joyn     , Quit     , Login, LoggedIn , Password, " +
							"       Hash    , IpAdress , EmlAdress, Vip  , GodModeEx  " +
							"FROM   Players    " + 
							"WHERE  UUID = ?   "
							);
					ps.setString(1, u.toString());
					ResultSet res = ps.executeQuery();
					//Code using ResultSet entries here
					if (res.next() == true) {
						pcm.set("login.username",   res.getString("Name"));
						pcm.set("timestamps.join",  res.getLong("Joyn"));
						pcm.set("timestamps.quit",  res.getLong("Quit"));
						pcm.set("timestamps.login", res.getLong("Login"));
						pcm.set("login.logged_in",  res.getBoolean("LoggedIn"));
						pcm.set("login.password",   res.getString("Password"));
						pcm.set("login.hash",       res.getString("Hash"));
						pcm.set("login.ipaddress",  res.getString("IpAdress"));
						pcm.set("login.emladdress", res.getString("EmlAdress"));
						pcm.set("login.vip",        res.getBoolean("Vip"));
						pcm.set("godmode_expires",  res.getLong("GodModeEx"));
					}
					res.close();
					ps.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}	       		    				
			}
			PConfManager.pcms.put(u, pcm);
			return pcm;
        }
    }

	public static void saveAllManagers(String RunAs) {
		Logger log = AmkMcAuth.getInstance().getLogger();
		//log.info("saveAllManager timer Run");
		// Runs every Config.saveUserdataInterval minutes.

		if(DebugSave>0) log.info("Debug: Save Players Settings Started") ;

		synchronized (PConfManager.pcms) {
			//pcm only exists if player has joined..
			if(Config.removeAfterDays>0) {
				
				if(DebugSave>0) log.info("Debug: Checking Not-LogedIn Players.") ;
				
				UUID Uid;
				// 1000*60 sec. * 60 min. * 24 uur * Config.removeAfterDays dagen
				long PlayerSleepTime = (1000*60 * 60 * 24 * Config.removeAfterDays );
				//for (PConfManager pcm : PConfManager.pcms.values()) {
				List<UUID> toRemove = new ArrayList<UUID>();  // Fixes ConcurrentModificationException ??
				for (PConfManager pcm : PConfManager.pcms.values()) { // Gives ConcurrentModificationException on remove!!
					// First: Remove Old Entries from pcms (Player has NOT logged in for a LONG time)
					// This information is already saved on the previous saveAllManagers run, so no worry.
					// We are ONLY freeing up server internal memory, not Profile Files/SQL-Data!!!.
					// PlayerTimeoutAt is Join-time + Sleep-time. Is normaly higher!! then Current-Timestamp
					// Invalid Logons will be removed asap at they do not have a true Join-timestamp!

					long PlayerTimeoutAt = pcm.getLong("timestamps.join",0L) + PlayerSleepTime;
					//long PlayerTimeoutAt = ap.getLastJoinTimestamp() + PlayerSleepTime;
					long PlayerTimeLeft=PlayerTimeoutAt - System.currentTimeMillis();
					if(DebugSave==2) log.info("Debug: Player: " + pcm.getString("login.username") + 
							" PlayerTimeout: " + PlayerTimeoutAt + 
							" currentTimeMillis :" + System.currentTimeMillis() +
							" PlayerTimeLeft: " + (PlayerTimeLeft/1000) + " Sec.") ;
					//if(PlayerTimeoutAt < System.currentTimeMillis()) {
					if(PlayerTimeLeft<0) { // Out of Time, Remove PlayerData from memory
						String SoFar="Start try";
						try {
							Uid = AmkAUtils.getUUID(pcm.getString("login.username"));
							AuthPlayer ap = AuthPlayer.getAuthPlayer(Uid);
							if (Uid!=null && pcm.getString("login.username")!=null) { 
								//if (PConfManager.pcms.containsKey(Uid) & !ap.isLoggedIn()) {
								if (PConfManager.pcms.containsKey(Uid)) {
									//if(DebugSave) log.info("Player: " + pcm.getString("login.username") + " not logged in for " + 
									//		Config.removeAfterDays + " days. Clearing Profile-data.");
									log.info("Player: " + pcm.getString("login.username") + " not Joined in for " + 
											Config.removeAfterDays + " days. Removing in-memory Profile-data.");
									//SoFar="map.remove";
									//pcm.map.remove(Uid); // remove this pcm map
									SoFar="pcms.remove";
									toRemove.add(Uid); // Fixes ConcurrentModificationException ??
									//PConfManager.pcms.remove(Uid); // remove this pcms from list
									SoFar="ap.remAuthPlayer";
									ap.remAuthPlayer(Uid); // Removes authPlayers HashMap from AuthPlayer
									SoFar="Yes!!, All done";
								}								
							}            
						} catch (Exception ex) {
							log.info(SoFar + ": failed to Clear: " + pcm.getString("login.username") + "'s Profile-data!");
							//ex.printStackTrace();
						}
					}
				}
				for(UUID key: toRemove ) {  // Fixes ConcurrentModificationException ??
					PConfManager.pcms.remove(key); // remove this pcms from list   					
				}

				if(DebugSave>0) log.info("Debug: Checking Not-LogedIn Players Ended.") ;
				
			}

			
   			if (!Config.MySqlDbHost.equals("")) {
   				// Doe de SqlStatements	    		
   				if(RunAs.equals("Normal")) {
   		   			if(DebugSave>0) log.info("Debug: Save Player data to MySQL Normal.") ;
   					forceSaveMySQL(); // forceSaveMySQL() translates to pcm itself
   				}
   				else
   					{
   		   			if(DebugSave>0) log.info("Debug: Save Player data to MySQL Async.") ;
   					forceSaveMySQLAsync(); // forceSaveMySQL() translates to pcm itself
   				}
   			}
   			if (!Config.MySqlDbFile.equals("")) {
				long lastjoin=0;
				long second = 0;
				long minute = 0;
				long hour  = 0;
				long days = 0;

	   			if(DebugSave>0) log.info("Debug: Save Players Settings to Profile-data files started") ;
	   			
				for (PConfManager pcm : pcms.values()) {
					// Skip Save if "login.password" NOT set (=null/removed)
	   				if(pcm.isSet("login.password")) {
	   					if(DebugSave==2) {
	   						lastjoin=(System.currentTimeMillis() - pcm.getLong("timestamps.join",0L))/1000; // seconds
	   						second = (lastjoin) % 60;
	   						minute = (lastjoin / 60) % 60;
	   						hour =   (lastjoin / (60 * 60)) % 24;
	   						days =   (lastjoin / (60 * 60 * 24));
	   					
	   						log.info("Debug: saving Profile-data: " + pcm.getString("login.username") + " (last join: " +  
	   									String.format("%dd-%02dh%02dm%02ds", days ,hour, minute, second) + " ago)"); // Debug
	   					}	   					
	   					pcm.forceSave();
	   					// Bukkit.getLogger for Debugging 
	   				}
				}

				if(DebugSave>0) log.info("Debug: Save Players Settings to Profile-data files Ended") ;
				
			}
		}

		if(DebugSave>0) log.info("Debug: Save Players Settings Ended.") ;
		
	}

	public void forceSave() { // Normal FileSave
		synchronized (this.saveLock) {
			try {
				save(this.pconfl);
			} catch (IOException ignored) {
			}
		}
	}

	public static void forceSaveMySQLAsync() {
		new BukkitRunnable() {
			@Override
			public void run() {
				PConfManager.forceSaveMySQL();
			}
		}.runTaskAsynchronously(AmkMcAuth.getInstance());    		
	}
    
	public static void forceSaveMySQL() {
		Logger log = AmkMcAuth.getInstance().getLogger();
		//log.info("forceSaveMySQL timer Run");

		long lastjoin=0;
		long second = 0;
		long minute = 0;
		long hour  = 0;
		long days = 0;

		if(DebugSave>0) log.info("Debug: Save Player data to MySQL Started") ;
		
		PreparedStatement ps;
		try { // Force MySQL connection to be Active, Just doing nothing except renew MySql Connection 
			ps = MySQL.getConnection().prepareStatement(
					"SELECT count(*) as Aantal " +
					"FROM   Players " );
			ResultSet res = ps.executeQuery();
			//Code using ResultSet entries here
			if (res.next() == true) {
				res.getInt("Aantal");
			}
			res.close();
			ps.close();
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		synchronized (PConfManager.pcms) {
			//Connection con = MySQL.getConnection(); 
			for (PConfManager pcm : PConfManager.pcms.values()) {
				// Skip Save if "login.username" and "login.password" NOT set (=null/removed)
				//if(pcm.isSet("login.username") && pcm.getLong("godmode_expires", 0L)>0 ) {
   				if(pcm.isSet("login.password")) {
					try {
						String FileUUId = pcm.pconfl.getName().split("\\.",-1)[0];
						// First: Check if UUID records exists. If not, Insert empty one
						//PreparedStatement ps;
						ps = MySQL.getConnection().prepareStatement(
								"SELECT count(*) as Aanwezig " +
								"FROM   Players    " + 
   								"WHERE  UUID = ?   "
								);
						ps.setString(1, FileUUId);
						ResultSet res = ps.executeQuery();   				        	
						//res = ps.executeQuery();   				        	
						//Code using ResultSet entries here
						int Aantal=0;
						if (res.next() == true) {
							Aantal=res.getInt("Aanwezig");
						}
						res.close();
						ps.close();

						if(Aantal==0) {

		   					if(DebugSave==2) {		   						
		   						log.info("Debug: New Player (not found): adding to MySQL: " + pcm.getString("login.username")) ; // Debug
		   					}

							//log.info("forceSaveMySQL New player: " + pcm.getString("login.username") + " (UUID:" + FileUUId + ")");
							// has to be: No Player in Database, do NOT Insert and Update, because Players are Added at Login time
							ps = MySQL.getConnection().prepareStatement(
									"INSERT IGNORE INTO Players " +
									"       ( UUID) " +
									"VALUES (  ?  ) " 
									);
							ps.setString(1, FileUUId);
							ps.executeUpdate();
							ps.close();
							Aantal=1; // Yes, there is one now, Next: Update
						}
						if(Aantal>0) { // Update this Player (UUID).

		   					
							//ps = MySQL.getConnection().prepareStatement(
							//		"UPDATE Players         " + 
							//		"SET    Name      = ? , " +
							//		"       Joyn      = ? , " +
							//		"       Quit      = ? , " +
							//		"       Login     = ? , " +
							//		"       LoggedIn  = ? , " +
							//		"       Password  = ? , " + 
							//		"       Hash      = ? , " + 
							//		"       IpAdress  = ? , " +
							//		"       EmlAdress = ? , " +
							//		"       Vip       = ? , " +
							//		"       GodModeEx = ?   " +
							//		"WHERE  UUID      = ?   " +
							//		" AND   Login    <= ?   "
							//		);	       		    				
							//ps.setString(  1, pcm.getString("login.username"));	       								
							//ps.setLong(    2, pcm.getLong("timestamps.join", 0L));
							//ps.setLong(    3, pcm.getLong("timestamps.quit", 0L));
							//ps.setLong(    4, pcm.getLong("timestamps.login", 0L));
							//ps.setBoolean( 5, pcm.getBoolean("login.logged_in",false));
							//ps.setString(  6, pcm.getString("login.password",null));
							//ps.setString(  7, pcm.getString("login.hash", "AMKAUTH"));
							//ps.setString(  8, pcm.getString("login.ipaddress", null));
							//ps.setString(  9, pcm.getString("login.emladdress", null));
							//ps.setBoolean(10, pcm.getBoolean("login.vip",false));
							//ps.setLong(   11, pcm.getLong("godmode_expires", 0L));
							//ps.setString( 12, FileUUId);
							//ps.setLong(   13, pcm.getLong("timestamps.login", 0L));

							// As of version 1.4.4 AmkMcAuth saves Password Immediately on change, not here !!!
							// Only when the saved Login timestamp is older then the current login timestamp.
							// This to prevent from the new password being overwritten by an old password known
							// on a server where the player was previously was logging in using his old password.
							// If a player on login uses a wrong password, AmkMcAuth first reads the Players-Table.

		   					if(DebugSave==2) {
		   						lastjoin=(System.currentTimeMillis() - pcm.getLong("timestamps.join",0L))/1000; // seconds
		   						second = (lastjoin) % 60;
		   						minute = (lastjoin / 60) % 60;
		   						hour =   (lastjoin / (60 * 60)) % 24;
		   						days =   (lastjoin / (60 * 60 * 24));

		   						log.info("Debug: saving to MySQL: " + pcm.getString("login.username") + " (last join: " +  
	   									String.format("%dd-%02dh%02dm%02ds", days ,hour, minute, second) + " ago)"); // Debug
		   					}
							
							ps = MySQL.getConnection().prepareStatement(
									"UPDATE Players         " + 
									"SET    Name      = ? , " +
									"       Joyn      = ? , " +
									"       Quit      = ? , " +
									"       Login     = ? , " +
									"       LoggedIn  = ? , " +
									"       IpAdress  = ? , " +
									"       EmlAdress = ? , " +
									"       Vip       = ? , " +
									"       GodModeEx = ?   " +
									"WHERE  UUID = ?   "
								);	       		    				
							ps.setString(  1, pcm.getString("login.username"));	       								
							ps.setLong(    2, pcm.getLong("timestamps.join", 0L));
							ps.setLong(    3, pcm.getLong("timestamps.quit", 0L));
							ps.setLong(    4, pcm.getLong("timestamps.login", 0L));
							ps.setBoolean( 5, pcm.getBoolean("login.logged_in",false));
							ps.setString(  6, pcm.getString("login.ipaddress", null));
							ps.setString(  7, pcm.getString("login.emladdress", null));
							ps.setBoolean( 8, pcm.getBoolean("login.vip",false));
							ps.setLong(    9, pcm.getLong("godmode_expires", 0L));
							ps.setString( 10, FileUUId);
   						
							ps.executeUpdate();							
							ps.close();
						}
						//Code using ResultSet entries here
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}

		if(DebugSave>0) log.info("Debug: Save Players data to MySQL Ended") ;
		
	}

    
	/** 
	 * Get all PlayerProfile Files and parse info in it.
	 * Remembers IP-Addresses and VIP players. Call from onEnabled.
	 */
	public static void countPlayersFromIpAndGetVipPlayers() {    	
		if (!Config.MySqlDbHost.equals("")) {
			// Doe de SqlStatements
			try(
				//Connection con = MySQL.getConnection(); 
				PreparedStatement ps = MySQL.getConnection().prepareStatement(
						"SELECT count(*) as Aantal " +
						"FROM Players");
					ResultSet res = ps.executeQuery()
				) {
				//Code using ResultSet entries here
				// Initial PlayerCount = 0
				if (res.next() == true) {
					PlayerCount = res.getInt("Aantal");
					// VipPlayers are retrieved from MySQL using  listVipPlayers;
				}
				res.close();
				ps.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    		
			//new BukkitRunnable() {
			//    @Override
			//    public void run() {
			//        try(
			//        	//Connection con = MySQL.getConnection(); 
			//        	PreparedStatement ps = MySQL.getConnection().prepareStatement(
			//        			"SELECT * " +
			//        			"FROM Players");
			//        	ResultSet res = ps.executeQuery()
			//        	) {
			//            //Code using ResultSet entries here
			//        } catch (SQLException e) {
			//			// TODO Auto-generated catch block
			//			e.printStackTrace();
			//		}
			//    }
			//}.runTaskAsynchronously(AmkMcAuth.getInstance());    		
		}

		if (!Config.MySqlDbFile.equals("") ) {
			String AllPlayers = " ";
			String PlayerFound;
			PlayerCount=0;

			boolean VipFound=false;
			boolean UserNameFound=false;
			final File userdataFolder = new File(AmkMcAuth.dataFolder, "userdata");
			if (!userdataFolder.exists() || !userdataFolder.isDirectory()) return;

			File IpFile = new File(IpAddressesFile);
			IpFile.delete(); // Remove the 'old' IpAddressFile

			File EmFile = new File(EmAddressesFile);
			EmFile.delete(); // Remove the 'old' EmAddressFile

			for (String fileName : userdataFolder.list(new PatternFilenameFilter("(?i)^.+\\.yml$"))) {
				Scanner in;
				VipFound=false;
				UserNameFound=false;
				PlayerFound=" ";
				PlayerCount++;
				try {
					in = new Scanner(new File(userdataFolder + File.separator + fileName));
					//while (in.hasNextLine()) { // iterates each line in the file
					while (in.hasNext()) { // 1 more character?: iterates each line in the file
						String line = in.nextLine();
						if(line.contains("username:")) {
							PlayerFound = line.substring(line.lastIndexOf(" ")+1) + " ";
							// AllPlayers bugfix SamePlayer with different lower-/uppercase letters 
							AllPlayers = AllPlayers + PlayerFound;
							UserNameFound=true;
						}
						if(line.contains("vip: true")){
							VipFound=true;
						}
						if(line.contains("ipaddress:") && Config.maxUsersPerIpaddress>0 ){
							addPlayerToIp(line.substring(line.lastIndexOf(" ")+1));
						}
						if(line.contains("emladdress:") && Config.maxUsersPerEmaddress>0 ){
							addPlayerToEm(line.substring(line.lastIndexOf(" ")+1));
						}
					}
					if(VipFound && UserNameFound){
						VipPlayers = VipPlayers + PlayerFound;
					}
					in.close(); // don't forget to close resource leaks
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			byte data[]=AllPlayers.getBytes();
			Path p = Paths.get(PlayerNameFile);
			try (
				OutputStream out = new BufferedOutputStream(Files.newOutputStream(p, CREATE, TRUNCATE_EXISTING))
			) {
				out.write(data,0,data.length);
			} catch (IOException e) {
				e.printStackTrace();        	
			}    		
		} 
	}
    
    /** 
     * return number of Player Profiles (Registered Players).
     * Used only on startup to count for bStats statistics.
     */
    public static int getPlayerCount() {
    	return PlayerCount;
    }

    /** 
     * return number of Player Profiles (Active Players).
     */
    public static int getProfileCount() {
    	return PConfManager.pcms.size();
    }

    /** 
     * return number of unique Email-addresses.
     * Used in amka command (Only for Debug purposes).
     */
    public static int getEmaddressCount() {
    	int ReturnEmAdrsCount = 0; // Initial ReturnIpAddressCount=0
    	if (!Config.MySqlDbHost.equals("")) { // MySQL heeft voorrang !!
    		// Doe de SqlStatements    		
	        try(
		        //Connection con = MySQL.getConnection(); 
		        PreparedStatement ps = MySQL.getConnection().prepareStatement(
		        			"SELECT count(distinct EmlAdress) as Aantal " +
		        			"FROM Players");
		       	ResultSet res = ps.executeQuery()
	        ) {
	        	//Code using ResultSet entries here
	        	// Initial PlayerCount = 0
		        if (res.next() == true) {
		        	ReturnEmAdrsCount = res.getInt("Aantal");
		        }
    	        res.close();
    	        ps.close();
	        } catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	} else  {
    		ReturnEmAdrsCount = EmAdrsCount;
    		//return IpAddresses.size();
    	}
    	return ReturnEmAdrsCount;
    }

    /** 
     * return number of unique IP-addresses.
     * Used in amka command (Only for Debug purposes).
     */
    public static int getIpaddressCount() {
    	int ReturnIpAdrsCount = 0; // Initial ReturnIpAddressCount=0
    	if (!Config.MySqlDbHost.equals("")) { // MySQL heeft voorrang !!
    		// Doe de SqlStatements    		
    		try(
    			//Connection con = MySQL.getConnection(); 
    			PreparedStatement ps = MySQL.getConnection().prepareStatement(
    				"SELECT count(distinct IpAdress) as Aantal " +
    				"FROM Players");
    			ResultSet res = ps.executeQuery()
    		) {
    			//Code using ResultSet entries here
    			// Initial PlayerCount = 0
    			if (res.next() == true) {
    				ReturnIpAdrsCount = res.getInt("Aantal");
    			}
    			res.close();
    			ps.close();
    		} catch (SQLException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}
    	} else  {
    		ReturnIpAdrsCount = IpAdrsCount;
    		//return IpAddresses.size();
    	}
    	return ReturnIpAdrsCount;
    }

    /** 
     * list Email-address information.
     * Used in amka command (Only for Debug purposes).
     */
    public static void listEmaddressesInfo(final CommandSender cs) {
    	if (!Config.MySqlDbHost.equals("")) { // MySQL heeft voorrang !!!
    		// Doe de SqlStatements    		
    		new BukkitRunnable() {
    		    @Override
    		    public void run() {
    		    	try(
    		    			//Connection con = MySQL.getConnection(); 
    		    			PreparedStatement ps = MySQL.getConnection().prepareStatement(
    		    					"SELECT (case when EmlAdress is null then 'NULL' else EmlAdress end) as EmlAdress , " + 
    		    					"       count(case when EmlAdress is null then ' NULL' else EmlAdress end) as Aantal " +
    		    					"FROM   Players " +
    		    					"GROUP by EmlAdress ");
    		    			ResultSet res = ps.executeQuery()
    		    	) {
    		    		//Code using ResultSet entries here
    		    		while(res.next() == true) {
    		    			PlayerCount = res.getInt("Aantal");
    		    			if((res.getString("EmlAdress")+"").equals("") || (res.getString("EmlAdress")).equals("NULL")) {
    		    				cs.sendMessage(ChatColor.BLUE + "Registered player-count from Email-Address: " + 
    		    						"<email not set!>  " + res.getInt("Aantal") );
    		    			} else {
    		    				cs.sendMessage(ChatColor.BLUE + "Registered player-count from Email-Address: " + 
    		    						res.getString("EmlAdress") + "  " + res.getInt("Aantal") );
    		    			}
    		    		}
    		    		res.close();
    		    		ps.close();
    		    	} catch (SQLException e) {
    		    		// TODO Auto-generated catch block
    		    		e.printStackTrace();
    		    	}
    		    }
    		}.runTaskAsynchronously(AmkMcAuth.getInstance());    		
    	} else {
    		new BukkitRunnable() {
    			@Override
    			public void run() {
    				//for(int i=0; i<PConfManager.getIpaddressCount();i++) { // CommandSender cs
    				//	if(IpAddresses.get(i).equals("")) {
    				//		cs.sendMessage(ChatColor.BLUE + "Registered player-count from Ip-Address: " + 
    				//									"<ip not set!>  " + IpAddressesCnt.get(i) );
    				//	} else {
    				//		cs.sendMessage(ChatColor.BLUE + "Registered player-count from Ip-Address: " + 
    				//									IpAddresses.get(i) + "  " + IpAddressesCnt.get(i) );
    				//	}
    				//}
    				//cs.sendMessage(ChatColor.BLUE + "---------"); 
    				try {
    					RandomAccessFile rfp = new RandomAccessFile(EmAddressesFile,"rw");
    	       			
    					String TmpEmAdr="";
    					int TmpCount=0;
    	       			
    					for(int i=0 ; i<EmAdrsCount ; i++) {
    						TmpEmAdr = rfp.readLine();
    						TmpCount = rfp.readInt();
    						if(TmpEmAdr.trim().equals("")) {
    							cs.sendMessage(ChatColor.BLUE + "Registered player-count from Email-Address: " + 
    									"<Email not set!>  " + TmpCount );
    						} else {
    							cs.sendMessage(ChatColor.BLUE + "Registered player-count from Email-Address: " + 
    									TmpEmAdr + "  " + TmpCount );
    						}
    					}
    					rfp.close();

    				} catch (IOException e) {
    					e.printStackTrace();        	
    				}    		
    			}
    		}.runTaskAsynchronously(AmkMcAuth.getInstance());    		
    	} 
    }

    /** 
     * list IP-address information.
     * Used in amka command (Only for Debug purposes).
     */
    public static void listIpaddressesInfo(final CommandSender cs) {
    	if (!Config.MySqlDbHost.equals("")) { // MySQL heeft voorrang !!!
    		// Doe de SqlStatements    		
    		new BukkitRunnable() {
    			@Override
    			public void run() {
    				try(
    					//Connection con = MySQL.getConnection(); 
    						PreparedStatement ps = MySQL.getConnection().prepareStatement(
    								"SELECT (case when IpAdress is null then 'NULL' else IpAdress end) as IpAdress , " + 
    								"       count(case when IpAdress is null then 'NULL' else IpAdress end) as Aantal " +
    								"FROM   Players " +
    								"GROUP by IpAdress ");    		    		
    						ResultSet res = ps.executeQuery()
    				) {
    					//Code using ResultSet entries here
    					while(res.next() == true) {
    						PlayerCount = res.getInt("Aantal");
    						if(res.getString("IpAdress").equals("") || (res.getString("IpAdress")).equals("NULL")) {
    							cs.sendMessage(ChatColor.BLUE + "Registered player-count from Ip-Address: " + 
    									"<ip not set!>  " + res.getInt("Aantal") );
    						} else {
    							cs.sendMessage(ChatColor.BLUE + "Registered player-count from Ip-Address: " + 
    									res.getString("IpAdress") + "  " + res.getInt("Aantal") );
    						}

    					}
    					res.close();
    					ps.close();
    				} catch (SQLException e) {
    					// TODO Auto-generated catch block
    					e.printStackTrace();
    				}
    			}
    		}.runTaskAsynchronously(AmkMcAuth.getInstance());    		
    	} else {
    		new BukkitRunnable() {
    			@Override
    			public void run() {
    				//for(int i=0; i<PConfManager.getIpaddressCount();i++) { // CommandSender cs
    				//	if(IpAddresses.get(i).equals("")) {
    				//		cs.sendMessage(ChatColor.BLUE + "Registered player-count from Ip-Address: " + 
    				//									"<ip not set!>  " + IpAddressesCnt.get(i) );
    				//	} else {
    				//		cs.sendMessage(ChatColor.BLUE + "Registered player-count from Ip-Address: " + 
    				//									IpAddresses.get(i) + "  " + IpAddressesCnt.get(i) );
    				//	}
    				//}
    				//cs.sendMessage(ChatColor.BLUE + "---------"); 
    				try {
    					RandomAccessFile rfp = new RandomAccessFile(IpAddressesFile,"rw");
    	       			
    					String TmpIpAdr="";
    					int TmpCount=0;
    	       			
    					for(int i=0 ; i<IpAdrsCount ; i++) {
    						TmpIpAdr = rfp.readLine();
    						TmpCount = rfp.readInt();
    						if(TmpIpAdr.trim().equals("")) {
    							cs.sendMessage(ChatColor.BLUE + "Registered player-count from Ip-Address: " + 
    									"<ip not set!>  " + TmpCount );
    						} else {
    							cs.sendMessage(ChatColor.BLUE + "Registered player-count from Ip-Address: " + 
    									TmpIpAdr + "  " + TmpCount );
    						}
    					}
    					rfp.close();

    				} catch (IOException e) {
    					e.printStackTrace();        	
    				}    		
    			}
    		}.runTaskAsynchronously(AmkMcAuth.getInstance());    		
    	} 
    }

    /** 
     * return total playercount from 1 IP-address.
     * Used in register to check for maximum.
     */
    public static int countPlayersFromIp(String IpAddress) {
    	int ReturnCount=0;
    	if (!Config.MySqlDbHost.equals("")) { // MySQL heeft voorrang !!
    		// Doe de SqlStatements
    		PreparedStatement ps;    		
    		try{
    			//Connection con = MySQL.getConnection(); 
    			ps = MySQL.getConnection().prepareStatement(
    					"SELECT count(*) as Aantal " +
    					"FROM   Players " +
    					"WHERE IpAdress = ? ");
    			ps.setString(1, IpAddress);
    			ResultSet res = ps.executeQuery();
    			//Code using ResultSet entries here
    			if (res.next() == true) {
    				ReturnCount = res.getInt("Aantal");
    			}
    			res.close();
    			ps.close();
    		} catch (SQLException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}
    	} else  {
    		try {

       			RandomAccessFile rfp = new RandomAccessFile(IpAddressesFile,"rw");
       			
       			boolean Found=false;
       			String TmpIpAdr="";
       			
       			for(int i=0 ; i<IpAdrsCount & !Found ; i++) {
       				TmpIpAdr = rfp.readLine();
       				ReturnCount = rfp.readInt();
       				if(TmpIpAdr.trim().equals(IpAddress)) Found=true;
       			}
       			rfp.close();

    		} catch (IOException e) {
        		e.printStackTrace();        	
        	}

    		//int PlIdx = IpAddresses.indexOf(IpAddress);
    		//if(PlIdx==-1) return 0;
    		//return IpAddressesCnt.get(PlIdx);    		
    	}
		return ReturnCount;
    }

    /** 
     * return total playercount from 1 IP-address.
     * Used in register to check for maximum.
     */
    public static int countPlayersFromEm(String EmAddress) {
		int ReturnCount=0;
    	if (!Config.MySqlDbHost.equals("")) { // MySQL heeft voorrang !!
    		// Doe de SqlStatements
			PreparedStatement ps;    		
	        try{
	        	//Connection con = MySQL.getConnection(); 
	        	ps = MySQL.getConnection().prepareStatement(
	        			"SELECT count(*) as Aantal " +
	        			"FROM   Players " +
	        			"WHERE EmlAdress = ? ");
	        	ps.setString(1, EmAddress);
	        	ResultSet res = ps.executeQuery();
	            //Code using ResultSet entries here
	        	if (res.next() == true) {
	        		ReturnCount = res.getInt("Aantal");
	        	}
    	        res.close();
    	        ps.close();
	        } catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	} else  {
    		try {

       			RandomAccessFile rfp = new RandomAccessFile(EmAddressesFile,"rw");
       			
       			boolean Found=false;
       			String TmpEmAdr="";
       			
       			for(int i=0 ; i<EmAdrsCount & !Found ; i++) {
       				TmpEmAdr = rfp.readLine();
       				ReturnCount = rfp.readInt();
       				if(TmpEmAdr.trim().equals(EmAddress)) Found=true;
       			}
       			rfp.close();

    		} catch (IOException e) {
        		e.printStackTrace();        	
        	}
    		
    		//int PlIdx = IpAddresses.indexOf(IpAddress);
    		//if(PlIdx==-1) return 0;
    		//return IpAddressesCnt.get(PlIdx);    		
    	}
		return ReturnCount;
    }

    /** 
     * Add 1 player to the IP-address playercount.
     * Used in register 
     */
    public static void addPlayerToIp(String IpAddress) {
    	//addPlayerToIpSet(IpAddress);
		//this.plugin.MyQueue.Put("addPlayerToIp:~" + IpAddress);
    	AmkMcAuth.MyQueue.Put("addPlayerToIp:~" + IpAddress);
    }
    public static void addPlayerToIpSet(String IpAddress) {
    	if (!Config.MySqlDbHost.equals("")) {
    		// Doe de SqlStatements NIET, telling gaat vanuit de MySQL database    		
    	}
    	if (!Config.MySqlDbFile.equals("")) {
    		if(Config.maxUsersPerIpaddress==0) return; // No Player-Counting

    		// Make sure the length is within limit 
    		if(IpAddress.length()>IpAdrslength-1) {
    			IpAddress = IpAddress.substring(0, IpAdrslength-1);
    		}
    		
    		//int PlIdx = IpAddresses.indexOf(IpAddress);
    		//if(PlIdx==-1) {
    		//	IpAddresses.add(IpAddress);
    		//	IpAddressesCnt.add(1);
    		//} else {
    		//	IpAddressesCnt.set(PlIdx,IpAddressesCnt.get(PlIdx)+1);
    		//}

    		try {
		    	//Logger log = AmkMcAuth.getInstance().getLogger();
    			
    			String TmpIpAdr;
    			int TmpCount;
    			boolean Found=false;
    			
    			RandomAccessFile rfp = new RandomAccessFile(IpAddressesFile,"rw");

		    	//log.info("FilePointer: " + rfp.getFilePointer());
		    	//log.info("FileLength: " + rfp.length());
		    	//log.info("ADD: Find IpAdress: " + IpAddress);

    			while(rfp.getFilePointer()<rfp.length()) {
    				TmpIpAdr = rfp.readLine();
    				TmpCount = rfp.readInt();
    				if(TmpIpAdr.trim().equals(IpAddress)){
    					Found=true;
    					rfp.seek(rfp.getFilePointer()-4);
    					rfp.writeInt(TmpCount+1);
    			    	//log.info("ADD: Added 1 (=now: " + (TmpCount+1) + ") count to Ip-Adress: " + TmpIpAdr);
    				}
    			}
    			if(!Found) {
    				// We are at the end of the file.
    				// rfp.writeBytes(IpAddress+'\r'); // IpAdrslength
    				// padded is filled with nulls, NOT with spaces. so no trim() needed!!
    				
    				//if((IpAdrslength - IpAddress.length()-1) >=0 )
    				//	TmpCount = IpAdrslength - IpAddress.length()-1;
    				//else
    				//	{
    				//	TmpCount=0;
	   				// 	Logger log = AmkMcAuth.getInstance().getLogger();
	   				// 	log.info("forceSaveMySQL timer Run");
    				//
	   				//	log.info("Update IP-Address counting failed at Ip-Address: " + "'"+IpAddress+"'");
    				//}
    				//String padded = new String(new char[TmpCount]);
					//String padded = new String(new char[IpAdrslength - IpAddress.length()-1]);
    				String padded = new String(new char[IpAdrslength-1]);
    				if((IpAdrslength - IpAddress.length()-1) >=0 ) 
    					padded = new String(new char[IpAdrslength - IpAddress.length()-1]);
    				
    				rfp.writeBytes((IpAddress+padded).substring(0, IpAdrslength-1)+'\r');
    				rfp.writeInt(1);  // Counter is now 1 (this one is just added)
    				IpAdrsCount++;
    			}

    			// Positioneer de FilePointer: file.seek(200);
				// Vraag FilePointer op:       long pointer = rfp.getFilePointer();
				// Lees record uit File:       int aByte = rfp.read();
				// Schrijf Record naar File (op FilePointer): rfp.write("Hello World".getBytes());
    			rfp.close();
    		} catch (IOException e) {
    			e.printStackTrace();        	
    		}
    	} 
    }

    /** 
     * Add 1 player to the Email-address playercount.
     * Used in register 
     */
    public static void addPlayerToEm(String EmAddress) {
    	//addPlayerToEmSet(EmAddress);
		//this.plugin.MyQueue.Put("addPlayerToEm:~" + EmAddress);
    	AmkMcAuth.MyQueue.Put("addPlayerToEm:~" + EmAddress);
    }
    public static void addPlayerToEmSet(String EmAddress) {
    	if (!Config.MySqlDbHost.equals("")) {
    		// Doe de SqlStatements NIET, telling gaat vanuit de MySQL database    		
    	}
    	if (!Config.MySqlDbFile.equals("")) {
    		if(Config.maxUsersPerEmaddress==0) return; // No Player-Counting

    		// Make sure the length is within limit 
    		if(EmAddress.length()>EmAdrslength-1) {
    			EmAddress = EmAddress.substring(0, EmAdrslength-1);
    		}

    		//int PlIdx = IpAddresses.indexOf(IpAddress);
    		//if(PlIdx==-1) {
    		//	IpAddresses.add(IpAddress);
    		//	IpAddressesCnt.add(1);
    		//} else {
    		//	IpAddressesCnt.set(PlIdx,IpAddressesCnt.get(PlIdx)+1);
    		//}

    		try {
		    	//Logger log = AmkMcAuth.getInstance().getLogger();
    			
    			String TmpEmAdr;
    			int TmpCount;
    			boolean Found=false;
    			
    			RandomAccessFile rfp = new RandomAccessFile(EmAddressesFile,"rw");

		    	//log.info("FilePointer: " + rfp.getFilePointer());
		    	//log.info("FileLength: " + rfp.length());

    			while(rfp.getFilePointer()<rfp.length()) {
    				TmpEmAdr = rfp.readLine();
    				TmpCount = rfp.readInt();
    				if(TmpEmAdr.trim().equals(EmAddress)){
    					Found=true;
    					rfp.seek(rfp.getFilePointer()-4);
    					rfp.writeInt(TmpCount+1);
    				}
    			}
    			if(!Found) {
    				// We are at the end of the file.
    				// rfp.writeBytes(IpAddress+'\r'); // EmAdrslength
    				// padded is filled with nulls, NOT with spaces. so no trim() needed!!

    				//if((EmAdrslength - EmAddress.length()-1) >=0 )
    				//	TmpCount = EmAdrslength - EmAddress.length()-1;
    				//else
    				//	{
    				//	TmpCount=0;
	   				// 	Logger log = AmkMcAuth.getInstance().getLogger();
	   				// 	log.info("forceSaveMySQL timer Run");
    				//
	   				//	log.info("Update Email-Address counting failed at Email-Address: " + "'"+EmAddress+"'");
    				//}
    				//String padded = new String(new char[TmpCount]);
    				//String padded = new String(new char[EmAdrslength - EmAddress.length()-1]);
    				String padded = new String(new char[EmAdrslength-1]);
    				if((EmAdrslength - EmAddress.length()-1) >=0 ) 
    					padded = new String(new char[EmAdrslength - EmAddress.length()-1]);
    				
    				//rfp.writeBytes(EmAddress+padded+'\r');
    				rfp.writeBytes((EmAddress+padded).substring(0, EmAdrslength-1)+'\r');
    				rfp.writeInt(1); // Counter is now 1 (this one is just added)  
    				EmAdrsCount++;
    			}

    			// Positioneer de FilePointer: file.seek(200);
				// Vraag FilePointer op:       long pointer = rfp.getFilePointer();
				// Lees record uit File:       int aByte = rfp.read();
				// Schrijf Record naar File (op FilePointer): rfp.write("Hello World".getBytes());
    			rfp.close();
    		} catch (IOException e) {
    			e.printStackTrace();        	
    		}
    	} 
    }

    /** 
     * Reduce playercount from one IP-Address with 1.
     * Used in unregister 
     */
    public static void removePlayerFromIp(String IpAddress) {
    	//removePlayerFromIpSet(IpAddress);
		//this.plugin.MyQueue.Put("removePlayerFromIp:~" + IpAddress);
    	if(IpAddress != null) AmkMcAuth.MyQueue.Put("removePlayerFromIp:~" + IpAddress);
    }    
    public static void removePlayerFromIpSet(String IpAddress) {
    	if (!Config.MySqlDbHost.equals("")) {
    		// Doe de SqlStatements NIET, telling gaat vanuit de MySQL database    		
    	}
    	if (!Config.MySqlDbFile.equals("")) {
    		if(Config.maxUsersPerIpaddress==0) return; // No Player-Counting
    		
    		//int PlIdx = IpAddresses.indexOf(IpAddress);
    		//if(PlIdx>=0) {
    		//	int Cntr = IpAddressesCnt.get(PlIdx);
    		//	if(Cntr>0) IpAddressesCnt.set(PlIdx,Cntr-1);
    		//}

    		try {
		    	//Logger log = AmkMcAuth.getInstance().getLogger();
    			
    			String TmpIpAdr;
    			int TmpCount;
    			
    			RandomAccessFile rfp = new RandomAccessFile(IpAddressesFile,"rw");

		    	//log.info("REM: Find IpAddress: " + IpAddress);
    			
    			while(rfp.getFilePointer()<rfp.length()) {
    		    	TmpIpAdr = rfp.readLine();
    				TmpCount = rfp.readInt();
    		    	//log.info("REM: Found: " + TmpIpAdr + "  Count: " + TmpCount);
    				if(TmpIpAdr.trim().equals(IpAddress) && TmpCount>0) { // Found: Only substract if more then 0  
    					rfp.seek(rfp.getFilePointer()-4);
    					rfp.writeInt(TmpCount-1);
        		    	//log.info("REM: Updated: " + TmpIpAdr + "  Count now: " + (TmpCount-1));
    				}
    			}
    			rfp.close();
    		} catch (IOException e) {
    			e.printStackTrace();        	
    		}

    	}
    }

    /** 
     * Reduce playercount from a Email-Address with 1.
     * Used in unregister 
     */
    public static void removePlayerFromEm(String EmAddress) {
    	//removePlayerFromEmSet(EmAddress);
		//this.plugin.MyQueue.Put("removePlayerFromEm:~" + EmAddress);
    	AmkMcAuth.MyQueue.Put("removePlayerFromEm:~" + EmAddress);
    }
    public static void removePlayerFromEmSet(String EmAddress) {
    	if (!Config.MySqlDbHost.equals("")) {
    		// Doe de SqlStatements NIET, telling gaat vanuit de MySQL database    		
    	}
    	if (!Config.MySqlDbFile.equals("")) {
    		if(Config.maxUsersPerEmaddress==0) return; // No Player-Counting
    		
    		//int PlIdx = IpAddresses.indexOf(IpAddress);
    		//if(PlIdx>=0) {
    		//	int Cntr = IpAddressesCnt.get(PlIdx);
    		//	if(Cntr>0) IpAddressesCnt.set(PlIdx,Cntr-1);
    		//}

    		try {
		    	//Logger log = AmkMcAuth.getInstance().getLogger();
    			
    			String TmpEmAdr;
    			int TmpCount;
    			
    			RandomAccessFile rfp = new RandomAccessFile(EmAddressesFile,"rw");

		    	//log.info("FilePointer: " + rfp.getFilePointer());
		    	//log.info("FileLength: " + rfp.length());

    			while(rfp.getFilePointer()<rfp.length()) {
    				TmpEmAdr = rfp.readLine();
    				TmpCount = rfp.readInt();
					if(TmpEmAdr.trim().equals(EmAddress) && TmpCount>0) { // Found: Only substract if more then 0
						rfp.seek(rfp.getFilePointer()-4);
						rfp.writeInt(TmpCount-1);
    				}
    			}
    			rfp.close();
    		} catch (IOException e) {
    			e.printStackTrace();        	
    		}

    	}
    }

    /** 
     * Show PlayerCount on the VIP-Player list 
     */
    public static int getVipPlayerCount() {
        if (!Config.MySqlDbHost.equals("")) { // MySQL heeft voorrang !!
    		// Doe de SqlStatements
    		int PlayerVipCount=0; // Initial PlayerVipCount=0
	        try(
	        	//Connection con = MySQL.getConnection(); 
	        	PreparedStatement ps = MySQL.getConnection().prepareStatement(
	        			"SELECT count(*) as Aantal " +
	        			"FROM   Players " +
	        			"WHERE  Vip = true ");
	        	ResultSet res = ps.executeQuery()
	        	) {
	            	//Code using ResultSet entries here
	        	if (res.next() == true) {
	        		PlayerVipCount = res.getInt("Aantal");
	        	}
    	        res.close();
    	        ps.close();
	        } catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    		return PlayerVipCount;
		} else {
    		if (VipPlayers.trim().isEmpty())
    			return 0;
    		return VipPlayers.trim().split("\\s+").length -1; // separate string around spaces, remove extra spaces, count -1
		}
    }

    /** 
     * Show All Active Players names (pcms player profiles loaded)
     */
    public static void listActivePlayers(final CommandSender cs) {
		new BukkitRunnable() {
		    @Override
		    public void run() {
		    	String AllActivePlayers="";
		    	for (PConfManager pcm : PConfManager.pcms.values()) {			
		    		AllActivePlayers=AllActivePlayers + " " + pcm.getString("login.username");
		    		if(AllActivePlayers.length()>60 ) {				
		    			cs.sendMessage(AllActivePlayers.trim());
		    			AllActivePlayers="";
		    		}
		    	}
		    	if(AllActivePlayers.length()>0 ) {				
		    		cs.sendMessage(AllActivePlayers.trim());
		    	}
		    }
		}.runTaskAsynchronously(AmkMcAuth.getInstance());    		
    }

    /** 
     * Show Players on the VIP-Player list 
     */
    public static void listVipPlayers(final CommandSender cs) {
    	if (!Config.MySqlDbHost.equals("")) { // MySQL heeft voorrang
    		// Doe de SqlStatements
    		new BukkitRunnable() {
    		    @Override
    		    public void run() {
    		        try(
    		        	//Connection con = MySQL.getConnection(); 
    		        	PreparedStatement ps = MySQL.getConnection().prepareStatement(
    	        				"SELECT Name " +
    	    	        		"FROM   Players " +
    	    		        	"WHERE  Vip = true ");
    		        	ResultSet res = ps.executeQuery()
    		        	) {
    		            //Code using ResultSet entries here
    			        String VipPlyrs = "";
    		        	while (res.next() == true) {
    		        		VipPlyrs = VipPlyrs + res.getString("Name") + " ";
    		        	}
    		    		if(VipPlyrs.trim().length()>0){
    		    			cs.sendMessage(AmkAUtils.colorize(Language.NLP_LIST_PLAYERS.toString()));
    		    			cs.sendMessage(VipPlyrs);
    		    		} else {
    		    			cs.sendMessage(AmkAUtils.colorize(Language.NLP_LIST_PLAYERS_NONE.toString()));
    		    		}
    	    	        res.close();
    	    	        ps.close();
    		        } catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
    		    }
    		}.runTaskAsynchronously(AmkMcAuth.getInstance());    		
    	} else {
    		if(VipPlayers.trim().length()>0){
    			cs.sendMessage(AmkAUtils.colorize(Language.NLP_LIST_PLAYERS.toString()));
    			cs.sendMessage(VipPlayers);
    		} else {
    			cs.sendMessage(AmkAUtils.colorize(Language.NLP_LIST_PLAYERS_NONE.toString()));
    		}
    	}
    }

    /** 
     * Add a Player to the VIP-Player list. 
     * Used in nlpadd
     */
    public static void addVipPlayer(String NewPlayer) {
    	//if (!Config.MySqlDbHost.equals("")) {
    		// Doe de SqlStatements NIET, telling gaat vanuit de MySQL database
    	//}
    	if (!Config.MySqlDbFile.equals("")) {
    		if(!VipPlayers.contains(" " + NewPlayer + " ")){
    			VipPlayers = VipPlayers + NewPlayer + " ";
    		}
		}
    }    

    /** 
     * Remove a Player from the VIP-Player list. 
     * Used in nlprem
     */
    public static void removeVipPlayer(String PlayerToRemove) {
    	//if (!Config.MySqlDbHost.equals("")) {
    		// Doe de SqlStatements NIET, telling gaat vanuit de MySQL database
    	//}
    	if (!Config.MySqlDbFile.equals("")) {
    		if(VipPlayers.contains(" " + PlayerToRemove + " ")){
    			VipPlayers = VipPlayers.replaceAll("(?i) " + PlayerToRemove.toLowerCase() + " ", " ");
    			//VipPlayers.replace(" " + PlayerToRemove + " ", " ");
    		}
		}
    }

    /** 
     * Check to see if Player already Registered (on ALL-Player list), ignore case!! 
     */
    public static String doesPlayerExist(String PlayerToSearch) {

    	String ReturnPlayerExist = PlayerToSearch; // Return logged-in PlayerName if not found

    	if (!Config.MySqlDbHost.equals("")) { // MySQL goes FIRST !!
    		// Doe de SqlStatements
	        try {
		       	// Connection con = MySQL.getConnection(); 
		       	PreparedStatement ps = MySQL.getConnection().prepareStatement(
		        			"SELECT Name " +
		        			"FROM   Players " +
		        			"WHERE  LOWER(Name)  = ? ");
		        ps.setString(1, PlayerToSearch.toLowerCase());
		       	ResultSet res = ps.executeQuery();
		        //Code using ResultSet entries here
		       	if (res.next() == true) {
		       		ReturnPlayerExist = res.getString("Name");
		       	}
    	        res.close();
    	        ps.close();
	        } catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	} else {
    		String AllPlayers = "";
        	Path p = Paths.get(PlayerNameFile);
            if(!Files.exists(p)) return ReturnPlayerExist;

    		try {
    			AllPlayers = new String(Files.readAllBytes(p));
			} catch (IOException e) {
				e.printStackTrace();
			}

        	//String [] AllPlayersLC = AllPlayers.toLowerCase().split(" ");

    		//if (AllPlayersLC.length==0) 
        	if (!AllPlayers.trim().isEmpty()) 
    			{
    			//ReturnPlayerExist = (AllPlayers.indexOf(" " + PlayerToSearch.toLowerCase() + " ")) >= 0; // search for PlayerToSearch
    			//String [] RealNames = AllPlayers.split(" ");
    		    //for (int i=0; i<AllPlayersLC.length; i++ ) {
    		    //    if (AllPlayersLC[i].equals(PlayerToSearch.toLowerCase())) {    		        	
    		    //   	ReturnPlayerExist=RealNames[i];
    		    //    }
        		
            	//Logger log = AmkMcAuth.getInstance().getLogger();
        		
            	String AllPlayersLC = AllPlayers.toLowerCase();

            	// Surround PlayerToSearch with Spaces so it looks for "complete" PlayerNames, not just partial if it has different length. 
    			Integer PlayerIndexLC = (AllPlayersLC.indexOf(" " + PlayerToSearch.toLowerCase() + " ")); // search for PlayerToSearch
		    	//log.info("PlayerIndexLC: " + PlayerIndexLC);
    			// The indexOf() method returns the position of the first occurrence of a specified value in a string. 
    			if(PlayerIndexLC >= 0) { // -1 means: not found. If it's 0 then it's the first player in the file 
    		    	//log.info("PlayerIndexLC+1: " + PlayerIndexLC+1);
    		    	//log.info("PlayerToSearch.length(): " + PlayerToSearch.length());
    		    	//log.info("PlayerIndexLC+1+PlayerToSearch.length(): " + PlayerIndexLC+1+PlayerToSearch.length());
    				//Return Real Name, adjusting the character-Index in the string to +1 so it skips the included "search" Space.
    				ReturnPlayerExist = AllPlayers.substring(PlayerIndexLC+1 , PlayerIndexLC+1+PlayerToSearch.length());
    			}

            	//// Solution made by MDFM28 !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! 
				/////// MDFM FIRST PLAYER ALWAYS RETURN 0!!! BECAUSE IT INCLUDES TE SPACES SO THE INDEX IS ON THE START OF THE FILE! MDFM
				/////// it defeats the purpose of exesscive "spaces".
				////Integer PlayerIndexLC = (AllPlayersLC.indexOf(" " + PlayerToSearch.toLowerCase() + " ")); // search for PlayerToSearch
				///////
				//Integer PlayerIndexLC = (AllPlayersLC.indexOf(PlayerToSearch.toLowerCase())); // search for PlayerToSearch
				/////// MDFM "indexOf" return '-1' when there's no occurance. so better use "> 0", or just straight up use -1!
				/////// This way when ther's a new username the this return the leng of username string from -1. will definetly failed the login, and possibly causes an string out of index!
				////if(PlayerIndexLC != 0)
				//// This will Probably not work, because if there is a A PlayerName "HenkJan" and a new Player logs in using
				//// PlayerName "henk" this solution will tell that the PlayerName already exists, but it is not (different name).
    			//// Thats why i use spaces surrounding the New Playername while searching, so it looks only for complete names.
				//if(PlayerIndexLC > 0) {
				//	//ReturnPlayerExist = AllPlayers.substring(PlayerIndexLC+1 , PlayerIndexLC+1+PlayerToSearch.length()); // Return Real Name
				//	//"PlayerIndexLC+1+PlayerToSearch.length()" why the +1 is in the middle tho.
				//	ReturnPlayerExist = AllPlayers.substring(PlayerIndexLC , PlayerIndexLC+PlayerToSearch.length()); // Return Real Name
				//}

				
    			//ReturnPlayerExist = AllPlayers); // search for PlayerToSearch
    			//ReturnPlayerExist = (AllPlayers.indexOf(" " + PlayerToSearch.toLowerCase() + " ")); // search for PlayerToSearch
    		}
    	}
    	//log.info("ReturnPlayerExist: " + ReturnPlayerExist);
    	return ReturnPlayerExist;
    }
    /** 
     * Add a Player to the ALL-Player list. 
     */
    public static void addAllPlayer(String NewPlayer) {
        //addAllPlayerSet(NewPlayer);
    	AmkMcAuth.MyQueue.Put("addAllPlayer:~" + NewPlayer);
    }
    public static void addAllPlayerSet(String NewPlayer) {
    	//if (!Config.MySqlDbHost.equals("")) {
    		// Doe de SqlStatements NIET, updates gaan vanuit de MySQL database
    	//}
    	if (!Config.MySqlDbFile.equals("")) {
    		//if(!AllPlayers.contains(" " + NewPlayer + " ")){
    		//	AllPlayers = AllPlayers + NewPlayer + " ";
    		NewPlayer = " " + NewPlayer + " ";
    		
    		byte data[]=NewPlayer.getBytes();
    		Path p = Paths.get(PlayerNameFile);
    		try (
    			OutputStream out = new BufferedOutputStream(Files.newOutputStream(p, CREATE, APPEND))
    		) {
    			out.write(data,0,data.length);
    		} catch (IOException e) {
    			e.printStackTrace();        	
    		}        
    	}
    }
    
    /** 
     * remove a Player From the ALL-Player list. 
     */
    public static void removeAllPlayer(String PlayerToRemove) {
    	if (!Config.MySqlDbHost.equals("")) {
    		// Doe de SqlStatements NIET, Updates gaan vanuit de MySQL database
    	} 
    	if (!Config.MySqlDbFile.equals("")) {
    		String AllPlayers = "";
    		Path p = Paths.get(PlayerNameFile);

    		try {
    			AllPlayers = new String(Files.readAllBytes(p));
    		} catch (IOException e) {
    			e.printStackTrace();
    		}

    		AllPlayers = AllPlayers.replaceAll("(?i) " + PlayerToRemove.toLowerCase() + " ", " ");
        
    		byte data[]=AllPlayers.getBytes();
    		try (OutputStream out = new BufferedOutputStream(
    				Files.newOutputStream(p, CREATE, TRUNCATE_EXISTING))) {
    			out.write(data,0,data.length);
    		} catch (IOException e) {
    			e.printStackTrace();        	
    		}        
    	}
    }
    
    // Clear (Remove) complete HasMap, is totally Empty after this
    public static void purge() {
    	synchronized (PConfManager.pcms) {
    		PConfManager.pcms.clear();
    	}
    }
    
    // Wat doet deze code??, "exists()" wordt ogenschijnlijk NIET aangeroepen?
    public boolean exists() {
    	if (!Config.MySqlDbHost.equals("")) {
    		// Doe de SqlStatements
    		return true;
    	} else {
    		return this.pconfl.exists();
    	}
    }

    // Deze code "createFile()" wordt ogenschijnlijk NIET aangeroepen?
    public boolean createFile() {
    	if (!Config.MySqlDbHost.equals("")) {
    		// Doe de SqlStatements
    		return true;
    	} else {
    		try {
    			return this.pconfl.createNewFile();
    		} catch (IOException ignored) {
    			return false;
    		}
    	}
    }

    public static void removePlayer(UUID u) {
		synchronized (PConfManager.pcms) {
			if (PConfManager.pcms.containsKey(u)) {
				PConfManager.pcms.remove(u);
			}            
		}
    	if (!Config.MySqlDbHost.equals("")) {
    		// Doe de SqlStatements
	        try{
		    	//Connection con = MySQL.getConnection();
	        	PreparedStatement ps = MySQL.getConnection().prepareStatement(
	        			"DELETE FROM Players " +
	        			"WHERE  UUID = ?  " 
	        			);
	        	ps.setString(1, u.toString());
	        	ps.executeUpdate();							
    	        ps.close();
	        } catch (SQLException e) {
	        	// TODO Auto-generated catch block
	        	e.printStackTrace();
	        }
    	}
    	if (!Config.MySqlDbFile.equals("")) {
    		File dataFolder = AmkMcAuth.dataFolder;
    		File rfile = new File(dataFolder + File.separator + "userdata" + File.separator + u + ".yml");
    		if (rfile.exists()) rfile.delete();  // Als bestaat dan verwijderen..
    	}
    }

    // Turn SAVE-Debug on or off 
    public static String SetDebugSave(String OnOff) {
    	if(OnOff.equals("on")) 	DebugSave=1;
    	if(OnOff.equals("on2"))	DebugSave=2;
    	if(OnOff.equals("off")) DebugSave=0;
    	
    	if(DebugSave>0)
        	if(DebugSave==1)
        		return "on (normal)";
        	else    		
        		return "on2 (extended)";
    	else
    		return "off";
    }

    /**
     * Gets a Location from config
     * <p/>
     * This <strong>will</strong> throw an exception if the saved Location is invalid or has missing parts.
     *
     * @param path Path in the yml to fetch from
     * @return Location or null if path does not exist or if config doesn't exist
     */
    public Location getLocation(String path) {
        if (this.get(path) == null) return null;
        String world = this.getString(path + ".w");
        double x = this.getDouble(path + ".x");
        double y = this.getDouble(path + ".y");
        double z = this.getDouble(path + ".z");
        float pitch = this.getFloat(path + ".pitch");
        float yaw = this.getFloat(path + ".yaw");
        return new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch);
    }

    /**
     * Sets a location in config
     *
     * @param value Location to set
     * @param path  Path in the yml to set
     */
    public void setLocation(String path, Location value) {
        this.set(path + ".w", value.getWorld().getName());
        this.set(path + ".x", value.getX());
        this.set(path + ".y", value.getY());
        this.set(path + ".z", value.getZ());
        this.set(path + ".pitch", value.getPitch());
        this.set(path + ".yaw", value.getYaw());
    }

    public float getFloat(String path) {
        return (float) this.getDouble(path);
    }
}
