package com.mooo.amksoft.amkmcauth;

import java.net.InetSocketAddress;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;			
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import com.mooo.amksoft.amkmcauth.tools.MySQL;

public class AuthPlayer {

    private final static Map<UUID, AuthPlayer> authPlayers = new HashMap<>();    
    private final PConfManager pcm;
    private UUID playerUUID;
    private String lastIPAddress;
    private int IpAddressCount=0;
    private long lastJoinTimestamp = 0L;
    private long lastWalkTimestamp = 0L;
    private long lastLoginTimestamp = 0L;
    private long lastQuitTimestamp = 0L;
    private Location lastJoinLocation;
    private BukkitTask reminderTask = null;

    private boolean CapchaOk = true; // default OK, only false if Capcha is false
    private String PictogramOldPwd = ""; // default ''. no PasswordChange pending
    private String PictogramNewPwd = ""; // default ''. no PasswordChange pending

    private boolean PlayerInventoryAvailable = false;
    private HashMap <String, ItemStack[]> inventories = new HashMap <String, ItemStack[]>();
    private HashMap <String, ItemStack[]> armour = new HashMap <String, ItemStack[]>();
    
    private AuthPlayer(UUID u) {
        this.playerUUID = u;
        this.pcm = PConfManager.getPConfManager(u);
        this.lastJoinTimestamp = this.pcm.getLong("timestamps.join", 0L);
        this.lastLoginTimestamp = this.pcm.getLong("timestamps.login", 0L);
        this.lastQuitTimestamp = this.pcm.getLong("timestamps.quit", 0L);
    }

    private AuthPlayer(Player p) {
        this(p.getUniqueId());
    }

    /**
     * Gets the AuthPlayer for the UUID of a player.
     *
     * @param u UUID to get AuthPlayer of
     * @return AuthPlayer
     */
    public static AuthPlayer getAuthPlayer(UUID u) {
        synchronized (AuthPlayer.authPlayers) {
            if (AuthPlayer.authPlayers.containsKey(u)) return AuthPlayer.authPlayers.get(u);
            final AuthPlayer ap = new AuthPlayer(u);
            AuthPlayer.authPlayers.put(u, ap);
            return ap;
        }
    }

    /**
     * Queries Mojang's API to get a UUID for the name, and then gets the AuthPlayer for that UUID.
     *
     * @param s Name
     * @return AuthPlayer or null if there was an error
     */
    public static AuthPlayer getAuthPlayer(String s) {
        //boolean Online=true;
        //UUID u;
        
    	//if(Bukkit.getOnlineMode()!= Online) 
		//{
    	//	// Server runs 'OffLine' AmkMcAuth calculates the UUID for this player...
    	//    u = UUID.nameUUIDFromBytes(("OfflinePlayer:" + s).getBytes(Charsets.UTF_8));    		
		//}
    	//else
    	//	{
    	//	try {
    	//		u = AmkAUtils.getUUID(s);
    	//	} catch (Exception ex) {
    	//		//ex.printStackTrace();
        //        return null;        		
        //	}
        //}
        UUID u;
    	try {
    		u = AmkAUtils.getUUID(s);
    	} catch (Exception ex) {
    		//ex.printStackTrace();
            return null;        		
        }
        return AuthPlayer.getAuthPlayer(u);
    }

    /**
     * Gets the AuthPlayer that represents a Player.
     *
     * @param p Player to get AuthPlayer of
     * @return AuthPlayer
     */
    public static AuthPlayer getAuthPlayer(Player p) {
        return AuthPlayer.getAuthPlayer(p.getUniqueId());
    }

    /**
     * Remove AmkMcAuth PlayerProfile from configuration by the UUID of a player. 
     *
     * @param u UUID of the AuthPlayer to remove
     */
    public void remAuthPlayer(UUID u){
        synchronized (AuthPlayer.authPlayers) {
            if (AuthPlayer.authPlayers.containsKey(u)) {
            	AuthPlayer.authPlayers.remove(u);
            }
        }
    }
    /**
     * Removes and Deletes an AuthPlayer by the UUID
     *
     * @param u UUID of the AuthPlayer to remove
     */
    public void removeAuthPlayer(UUID u) {
    	remAuthPlayer(u);
    	removeThisPlayer();
    }

    /**
     * Sets AmkMcAuth Player as not logged in and no Username and no password 
     *
     */
    public void removeThisPlayer() {
        //this.pcm.set("login.username", UserName);
    	//set EVERY playerInfo to null to clean it all.
        this.setLoggedIn(false);
    	this.pcm.set("login.password",null);
    	this.pcm.set("login.username",null);     	
    	this.pcm.set("godmode_expires", -1L);
    }

    /**
     * Checks if the AP has a password set.
     *
     * @return true if registered, false if not
     */
    public boolean isRegistered() {
        return this.pcm.isSet("login.password");
    }

    /**
     * Checks if Player is a VIP Player.
     *
     * @return true if VIP, false if not
     */
    public boolean isVIP() {
        return this.pcm.getBoolean("login.vip");
    }

    /**
     * Checks if the AP has logged in.
     *
     * @return true if logged in, false if not
     */
    public boolean isLoggedIn() {
        return this.pcm.getBoolean("login.logged_in");
    }

    /**
     * Sets the AP's logged in status. In most cases, login() or logout() should be used.
     *
     * @param loggedIn true if logged in, false if not
     */
    public void setLoggedIn(final boolean loggedIn) {
        this.pcm.set("login.logged_in", loggedIn);
    }

    /**
     * Sets the AP's Logged/Register-IpAddress count. Only used during Join+Register.
     *
     * @param The Login-IpAddress-Count of the Ip-Address the player is coming from
     */
    public void setPlayerIpCount(final int Count) {
    	this.IpAddressCount=Count;
    }
    
    /**
     * Sets the AP's VIP status. 
     *
     * @param VIP true if it is a VIP, false if not
     */
    public void setVIP(final boolean VIP) {
        this.pcm.set("login.vip", VIP);
        if(VIP)
        	PConfManager.addVipPlayer(getUserName());
        else
        	PConfManager.removeVipPlayer(getUserName());
    }

    /**
     * Sets the AP's logged in UserName. 
     *
     * @param loggedIn true if logged in, false if not
     */
    public void setUserName(final String UserName) {
        this.pcm.set("login.username", UserName);
    }
   
    
    /**
     * Checks if the AP has/had previously a logged in Session. Will return false if not correct logged in.
     *
     * @return true if (still) logged in and on same IP-Address
     */
    public boolean isInSession() {
        if (!this.isLoggedIn()) return false;
        if (this.lastLoginTimestamp <= 0L || this.lastQuitTimestamp <= 0L) return false;
        if (!getCurrentIPAddress().equals(this.lastIPAddress)) return false;
        return true;
    }

    /**
     * Checks if the AP is within a login session. Will return false if sessions are disabled.
     *
     * @return true if in a session, false if not or sessions are off
     * Extra test on NLP (NoLoginPassword) Player (Like VIP's).
     */
    public boolean isWithinSession() {
        if (!Config.sessionsEnabled) return false;
        if (Config.sessionsCheckIP && !isInSession()) return false; // Line replaced next 3 lines
        //if (this.lastLoginTimestamp <= 0L || this.lastQuitTimestamp <= 0L) return false;
        //if (!this.isLoggedIn()) return false;
        //if (Config.sessionsCheckIP && !getCurrentIPAddress().equals(this.lastIPAddress)) return false;
        long validUntil = Config.sessionLength * 60000L + this.lastQuitTimestamp;
        // added "this.getPlayer().hasPermission("amkauth.nlpwd")", so the permission is also "WithinSession"
        return (validUntil > System.currentTimeMillis() || this.isVIP() || this.getPlayer().hasPermission("amkauth.nlpwd"));
    }

    /**
     * Gets the hashed password currently set for the AP.
     *
     * @return Hashed password
     */
    public String getPasswordHash() {
        return this.pcm.getString("login.password");
    }

    /**
     * Gets the type of hash used to make the AP's password.
     *
     * @return Digest type
     */
    public String getHashType() {
        return this.pcm.getString("login.hash", "AMKAUTH");
    }

    /**
     * Gets the PConfManager of this AP.
     *
     * @return PConfManager
     */
    public PConfManager getConfiguration() {
        return this.pcm;
    }

    /**
     * Turns on the god mode for post-login if enabled in the config. Will auto-expire.
     */
    public void enableAfterLoginGodmode() {
        if (Config.godModeAfterLogin)
            this.pcm.set("godmode_expires", System.currentTimeMillis() + Config.godModeLength * 1000L);
        else
            this.pcm.set("godmode_expires", System.currentTimeMillis()-1000); // No-GodMode, so player already after that.        
    }

    /**
     * Checks if the player is in godmode from post-login godmode.
     *
     * @return true if in godmode, false if otherwise
     */
    public boolean isInAfterLoginGodmode() {
        if (!Config.godModeAfterLogin) return false;
        final long expires = pcm.getLong("godmode_expires", 0L);
        return expires >= System.currentTimeMillis();
    }

    /**
     * Logs an AP in. Does everything necessary to ensure a full login.
     */
    public void login() {
        final Player p = getPlayer();
        if (p == null) throw new IllegalArgumentException("That player is not online!");
        this.setLoggedIn(true);
        this.setUserName(p.getName());
        this.setLastLoginTimestamp(System.currentTimeMillis());
        final BukkitTask reminder = this.getCurrentReminderTask();
        if (reminder != null) reminder.cancel();
        final PConfManager pcm = getConfiguration();

        p.setCollidable(true);
        if (Config.invisibleMode) {
        	// Tell every online player that they can see me
    		for(Player op : Bukkit.getOnlinePlayers()) {
    			//if(p != op) {
    				op.showPlayer(AmkMcAuth.getInstance(), p);
   					if(!p.canSee(op)) {
   						AuthPlayer aop = getAuthPlayer(op);
   						if(aop.isLoggedIn()) p.showPlayer(AmkMcAuth.getInstance(), op);
   					}
    			//}
    		}        	
        }
        
        if (Config.adventureMode) {
            if (pcm.isSet("login.gamemode")) {
                try {
                    p.setGameMode(GameMode.valueOf(pcm.getString("login.gamemode", "SURVIVAL")));
                } catch (IllegalArgumentException e) {
                    p.setGameMode(GameMode.SURVIVAL);
                }
            }
            pcm.set("login.gamemode", null);
        }
        try { // This try should trap the "Crash on Teleport to Deleted World" bug
        	if (Config.allowMovementTime>0){ //  Last Time to reset to Login Location
        		//p.teleport(pcm.getLocation("login.lastlocation"));
        		p.teleport(this.getJoinLocation());
        	}
        	if (Config.teleportToSpawn) {
        		if (pcm.isSet("login.lastlocation")) p.teleport(pcm.getLocation("login.lastlocation"));
        		pcm.set("login.lastlocation", null);
        	}
        } catch (IllegalArgumentException e) {
        	// his Catch should fix the "Crash on Teleport to Deleted World" bug by teleporting to the SpawnLocation.
        	p.teleport(p.getLocation().getWorld().getSpawnLocation());
    		pcm.set("login.lastlocation", null);
        }
        
        // testing on sending commands !!!!!!!!!!!!!!!!
        //executeConsoleCommand
        //String command = "list";
        //ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
        //Bukkit.dispatchCommand(console, command);
        //AmkMcAuth.MyQueue.Put("executeConsoleCommand:~" + command);
        
        this.enableAfterLoginGodmode();
        this.setLoggedIn(true);
        this.setUserName(p.getName());
    }

    /**
     * Kicks a Unregistered player. This does really the same as forcing a player to rejoin.
     * <p/>
     * This will schedule reminders for the player.
     *
     * @param plugin Plugin to register reminder events under
     */
    public void forceLogout(Plugin plugin) {
    	// Player gets Unregistered using the unregister command!!!
    	//this.setLoggedIn(false);  // This should force the SetRegisterReminder  	
        this.logout(plugin, false);
        
        try {
			getPlayer().kickPlayer(AmkAUtils.colorize("Your Player is unregisterd, rejoin server to register again"));
		} catch (Exception e) {
			// TODO Auto-generated catch block
		}
    }

    /**
     * Logs a player out. This is not the same as forcing a player to rejoin.
     *
     * @param plugin          Plugin to register reminder events under
     * @param createReminders If reminders should be created for the player
     */
    public void logout(Plugin plugin, boolean createReminders) {
        final Player p = getPlayer();
        //if (p == null) throw new IllegalArgumentException(Language.PLAYER_NOT_ONLINE.toString());
        if (p != null) {
       		this.setLoggedIn(false);
       		if (createReminders) {
       			if (this.isRegistered()) 
       				this.createLoginReminder(plugin);
       			else 
       				this.createRegisterReminder(plugin);
       		}
        	
        	if (!this.isRegistered()) {
        		AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
        		ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
        		for (String playerAction : Config.playerActionSJReg) {
        			if(!playerAction.trim().isEmpty()) {
        				//Bukkit.dispatchCommand(console, playerAction.replace("$P", ap.getUserName()));
        				
        				try {        	        		
        	        		if(playerAction.contains("AmkWait(")) 
        	        			AmkAUtils.createRunLaterCommand(ap.getUserName(), playerAction);
        	        		else
        	        			Bukkit.dispatchCommand(console, playerAction.replace("$P", ap.getUserName()));
        					
        				} catch (Exception  error  ) {
        					Bukkit.getLogger().info("Error OnRgstr Executing: " + playerAction.replace("$P", ap.getUserName()) );
        					error.printStackTrace();
        				}
        				
        				//AmkMcAuth.MyQueue.Put("executeConsoleCommand:~" + playerAction.replace("$P", ap.getUserName()));
        			}
        		}
        	}
    		

        	final PConfManager pcm = getConfiguration();

            try { // This try should trap the "Crash on stopping The Server" bug
       			for(Player op : Bukkit.getOnlinePlayers()) {
    				AuthPlayer aop = getAuthPlayer(op);
    				if(!aop.isLoggedIn()) op.setCollidable(false);   				
       			}
            	if (Config.invisibleMode) {
           			// Tell every online player that they cannot see me
           			for(Player op : Bukkit.getOnlinePlayers()) {
           				//if(p != op){
           					op.hidePlayer(plugin, p);
           					if(p.canSee(op)) {
           						AuthPlayer aop = getAuthPlayer(op);
           						if(!aop.isLoggedIn()) p.hidePlayer(plugin, op);
           					}
           				//}
           			}
            	}
            } catch (IllegalArgumentException e) {
            	// Do noting, Error/stopping, nothing to hide...
            }
        	if (Config.adventureMode) {
        		if (!pcm.isSet("login.gamemode")) pcm.set("login.gamemode", p.getGameMode().name());
        		p.setGameMode(GameMode.ADVENTURE);
        	}
            //    //p.teleport(pcm.getLocation("login.lastlocation"));
            //	this.setJoinLocation(p.getLocation()); // Logout to Join Location Fix ??
            //}
        	if (Config.teleportToSpawn) {
    			Location location;
        		if (!pcm.isSet("login.lastlocation")) pcm.setLocation("login.lastlocation", p.getLocation());
        		if(!Config.spawnWorld.equals("")) {
        			// The Server Owner configured a special Spawn World (by name)
        			World world = Bukkit.getWorld(Config.spawnWorld); // Get this world
        			if(Config.useSpawnAt) // Use Special Spawn coordinates ??
        				// Yes, Use Special Spawn coordinates, Might fail if World not exists.
        				location = new Location(world,Config.spawnWorldX, Config.spawnWorldY,Config.spawnWorldZ);
        			else
        				// No, Use Configured Spawn Location in given World. Might fail if World not exists.
        				location = new Location(world,world.getSpawnLocation().getX(),world.getSpawnLocation().getY(),world.getSpawnLocation().getZ());
            		//p.teleport(location);
        		}
        		else
        			{
        			if(Config.useSpawnAt) 
        				location = new Location(p.getLocation().getWorld(), Config.spawnWorldX, Config.spawnWorldY,Config.spawnWorldZ);
        			else
        				location = p.getLocation().getWorld().getSpawnLocation();
        			
        		}
        		//p.teleport(p.getLocation().getWorld().getSpawnLocation());
    			p.teleport(location);  // Might fail if World and Span coordinates not exists.
        	}
            if (Config.allowMovementTime>0){ //  Last Time to remember the Login Location
                //p.teleport(pcm.getLocation("login.lastlocation"));
            	this.setJoinLocation(p.getLocation()); // Logout to Join Location Fix ??
            }
        	this.setLoggedIn(false);
    	}
    }

	/**
     * Sets the last time that an AP logged in.
     *
     * @param timestamp Time in milliseconds from epoch
     */
    public void setLastLoginTimestamp(final long timestamp) {
        this.lastLoginTimestamp = timestamp;
        this.pcm.set("timestamps.login", timestamp);
    }

    /**
     * Sets the last time that an AP quit.
     *
     * @param timestamp Time in milliseconds from epoch
     */
    public void setLastQuitTimestamp(final long timestamp) {
        this.lastQuitTimestamp = timestamp;
        this.pcm.set("timestamps.quit", timestamp);
    }


    /**
     * Gets the AP's Registered EMail address.
     *
     * @return Registered Email Address)
     */
    public String getEmailAddress() {
    	// the ""+ prevents from returing null value.
        return ""+this.pcm.getString("login.emladdress");
    }

    /**
     * Sets the Registered Email Address.
     *
     * @param The EmailAddress to set.
     */
    public boolean setEmailAddress(final String emladdress) {
        this.pcm.set("login.emladdress", emladdress);
        return true;
    }

    /**
     * Changes the player's password. Requires the old password for security verification.
     *
     * @param hashedPassword    The password hash of the new password.
     * @param oldHashedPassword The password hash of the old password.
     * @return true if password changed, false if not
     */
    public boolean setHashedPassword(String hashedPassword, String oldHashedPassword, final String hashType) {
        if (!this.getPasswordHash().equals(oldHashedPassword)) return false;
        return this.setHashedPassword(hashedPassword, hashType.toUpperCase());
        //this.pcm.set("login.password", hashedPassword);
        //this.pcm.set("login.hash", hashType.toUpperCase());
        //return true;
    }

    /**
     * Sets the AP's password.
     *
     * @param newPasswordHash An already encrypted password
     * @param hashType        What type of hash was used to encrypt the password (Java type)
     * @return true
     */
    public boolean setHashedPassword(String newPasswordHash, final String hashType) {
        this.pcm.set("login.password", newPasswordHash);
        this.pcm.set("login.hash", hashType);

        if(!Config.MySqlDbHost.equals("")) { // MySQL goes BEFORE ProfileFiles !!
        	PreparedStatement ps;
        	try {
        		String ppuid = this.getUniqueId().toString();
        		
        		// CODE UPDATE CREATED BY MDFM28
        		// CHECK IF UUID NOT EXIST STAT - METHODS COPIED FROM AMKMCAUTH
        		int RecAanwezig=0;
				ps = MySQL.getConnection().prepareStatement(
							"SELECT count(*) as Aanwezig " +
							"FROM   Players    " + 
							"WHERE  UUID = ?   "
						);
				ps.setString(1, ppuid);
				ResultSet rs = ps.executeQuery();   				        	
				//Code using ResultSet entries here
				if (rs.next() == true) {
					RecAanwezig=rs.getInt("Aanwezig");
				}
				
				ps.close();
				rs.close();

				// log.info("forceSaveMySQL player: " + pcm.getString("login.username") + " (UUID:" + FileUUId + ")");
				// log.info("forceSaveMySQL player: " + this.getUserName() + " (UUID:" + FileUUId + ")");
				// pcm.getString("login.username") and this.getUserName() should be the same?

				String SqlStatement ="";
				if(RecAanwezig==0) { // New Player, insert.
					SqlStatement =	"INSERT IGNORE INTO Players " +
									"       ( Password, Hash, Name, UUID ) " +
									"VALUES (    ?    ,   ? ,   ? ,   ?  ) " ;
				}
				else
					{ // Existing Player, update.
					SqlStatement =  "UPDATE Players         " +
									"SET    Password  = ? , " +
									"       Hash      = ? , " +
									"       Name      = ?   " +
									"WHERE  UUID      = ?   " ;
				}
				ps = MySQL.getConnection().prepareStatement(SqlStatement);
	            ps.setString(  1, newPasswordHash);
	            ps.setString(  2, hashType);
	            ps.setString(  3, this.getUserName());
	            ps.setString(  4, ppuid);
					
	            ps.executeUpdate();	
				ps.close();
			} catch (SQLException e1) {
        		// TODO Auto-generated catch block
        		e1.printStackTrace();
       		}
		}
       
        return true;
    }

    /**
     * Sets the AP's password.
     *
     * @param rawPassword Unencrypted password
     * @param hashType    What type of hash was used to encrypt the password (Java type)
     * @return true if password set, false if otherwise
     */
    public boolean setPassword(String rawPassword, final String hashType) {
        try {
            rawPassword = Hasher.encrypt(rawPassword, hashType);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false;
        }
        return this.setHashedPassword(rawPassword, hashType);
        
        //this.pcm.set("login.password", rawPassword);
        //this.pcm.set("login.hash", hashType);
        //return true;
    }

    /**
     * Changes the player's password. Requires the old password for security verification.
     *
     * @param rawPassword    Plaintext new password
     * @param rawOldPassword Plaintext old password
     * @param hashType       Hashtypes to be used on these passwords
     * @return true if password changed, false if not
     */
    public boolean setPassword(String rawPassword, String rawOldPassword, final String hashType) {
        final String oldPasswordHash = (!getHashType().equalsIgnoreCase(hashType)) ? getHashType() : hashType;
        try {
            rawPassword = Hasher.encrypt(rawPassword, hashType);
            rawOldPassword = Hasher.encrypt(rawOldPassword, oldPasswordHash);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false;
        }
        return this.setHashedPassword(rawPassword, rawOldPassword, hashType);
    }

    /**
     * Saves/Hides temporary the players Inventory When the player Joins.
     */
    public void HideSurvivalInventory(Player p){        
        ItemStack[] inv = p.getInventory().getContents();
        ItemStack[] arm = p.getInventory().getArmorContents();
        this.inventories.put(p.getName(), inv);
        this.armour.put(p.getName(), arm);
    	p.getInventory().clear();
        p.updateInventory();
        PlayerInventoryAvailable = true;
    }
    
    /**
     * Restores the temporary saved player Inventory in Successfull Login or on Leave .
     * The Restore can only be done One Time, so no Restore in Leave after Login.
    */
    public void RestoreSurvivalInventory(Player p){
        if (PlayerInventoryAvailable) {
        	p.getInventory().setContents(this.inventories.get(p.getName()));
        	p.getInventory().setArmorContents(this.armour.get(p.getName()));
        	this.inventories.remove(p.getName());
        	this.armour.remove(p.getName());
        	p.updateInventory();
            PlayerInventoryAvailable = false;
        }
    }
    
    /**
     * Gets the AP's last/previous used IP address (not the current IP-Address) .
     *
     * @return ip IP Address (after quit it is the Current Ip-Address)
     */
    public String getLastIPAddress() {
        //"login.ipaddress" only used in Register to check on multiple Usernames from Client-IP
    	//return this.lastIPAddress; is the same as below ?
        return this.pcm.getString("login.ipaddress");
    }

    /**
     * Sets the AP's last IP address.
     *
     * @param ip IP Address (IPv6 or IPv4 will work, as long as they are consistent)
     */
    public void setLastIPAddress(String ip) {
        this.lastIPAddress = ip.replace("/", "");
        //"login.ipaddress" only used in Register to check on multiple Usernames from Client-IP
        this.pcm.set("login.ipaddress", this.lastIPAddress);
    }

    /**
     * Updates the AP's IP address automatically
     */
    public void updateLastIPAddress() {
        final String ip = getCurrentIPAddress();
        if (ip.isEmpty()) return;
        this.setLastIPAddress(ip);
    }

    /**
     * Gets the AuthPlayer's current IP address.
     *
     * @return IP address in String form or empty string if the player was null
     */
    public String getCurrentIPAddress() {
        final Player p = this.getPlayer();
        if (p == null) return "";
        final InetSocketAddress isa = p.getAddress();
        if (isa == null) return "";
        return isa.getAddress().toString().replace("/", "");
    }

    /**
     * Gets the current task sending reminders to the AP.
     *
     * @return Task or null if no task
     */
    public BukkitTask getCurrentReminderTask() {
        return this.reminderTask;
    }

    /**
     * Creates a task to remind the AP to login.
     *
     * @param p Plugin to register task under
     * @return Task created
     */
    public BukkitTask createLoginReminder(Plugin p) {
        this.reminderTask = AmkAUtils.createLoginReminder(getPlayer(), p);
        return this.getCurrentReminderTask();
    }
    public BukkitTask createLoginEmailReminder(Plugin p) {
        this.reminderTask = AmkAUtils.createLoginEmailReminder(getPlayer(), p);
        return this.getCurrentReminderTask();
    }

    /**
     * Creates a task to remind the AP to register.
     *
     * @param p Plugin to register task under
     * @return Task created
     */
    public BukkitTask createRegisterReminder(Plugin p) {
        this.reminderTask = AmkAUtils.createRegisterReminder(getPlayer(), p);
        return this.getCurrentReminderTask();
    }

    /**
     * Creates a task to remind the AP to register.
     *
     * @param p Plugin to register task under
     * @return Task created
     */
    public BukkitTask createSetEmailReminder(Plugin p) {
        this.reminderTask = AmkAUtils.createSetEmailReminder(getPlayer(), p);
        return this.getCurrentReminderTask();
    }

    /**
     * Gets the Player object represented by this AuthPlayer.
     *
     * @return Player or null if player not online
     */
    public Player getPlayer() {
        return Bukkit.getPlayer(playerUUID);
    }

    /**
     * Gets the Player UserName represented by this AuthPlayer.
     *
     * @return Player or null if player not online
     */
    public String getUserName() {
        return this.pcm.getString("login.username");
     }

    /**
     * Gets the UUID associated with this AuthPlayer.
     *
     * @return UUID
     */
    public UUID getUniqueId() {
        return this.playerUUID;
    }

    /**
     * Gets the last time this AP joined the server. If this is 0, they have never joined.
     *
     * @return Timestamp in milliseconds from epoch
     */
    public long getLastJoinTimestamp() {
        return this.lastJoinTimestamp;
    }
    public long getLastWalkTimestamp() {
        return this.lastWalkTimestamp;
    }

    /**
     * Gets the AP's Logged/Register-IpAddress count. Only used during Join+Register.
     *
     * @return The Login-IpAddress-Count of the Ip-Address the player is coming from
     */
    public int getPlayerIpCount() {
    	return this.IpAddressCount;
    }

    /**
     * Sets the last time that an AP joined the server.
     *
     * @param timestamp Time in milliseconds from epoch
     */
    public void setLastJoinTimestamp(final long timestamp) {
        this.lastJoinTimestamp = timestamp;
        this.lastWalkTimestamp = timestamp;
        this.pcm.set("timestamps.join", timestamp);
    }
    
    public void setLastWalkTimestamp(final long timestamp) {
        this.lastWalkTimestamp = timestamp;
    }

    /**
     * Gets the Location where the Player was when he Joined.
     *
     * @return Location
     */
    public Location getJoinLocation() {
        return this.lastJoinLocation;
    }

    /**
     * Sets the location where the AP joined the server.
     *
     * @param Location
     */
    public void setJoinLocation(final Location l) {
        this.lastJoinLocation = l;
    }
    
    
    /**
     * Sets the Capcha Status.
     *
     * @param boolean true: Capcha Ok, false: Capcha wrong
     */
    public void setCapchaOk(boolean Status) {
    	this.CapchaOk = Status;
    }
    public boolean getCapchaOk() {
    	return this.CapchaOk;
    }
    
    /**
     * Sets the Password Change status 
     *
     * @param String "":NoChange, "AskOld":Get Old Password, "OldWrong":OldPassword Wrong, "AskNew":Get New Password 
     */
    public void setPwdOldStatus(String Status) {
    	this.PictogramOldPwd = Status;
    }
    public String getPwdOldStatus() {
    	return this.PictogramOldPwd;
    }
    
    /**
     * Sets the Password Change status 
     *
     * @param String "":NoChange, "AskOld":Get Old Password, "OldWrong":OldPassword Wrong, "AskNew":Get New Password 
     */
    public void setPwdNewStatus(String Status) {
    	this.PictogramNewPwd = Status;
    }
    public String getPwdNewStatus() {
    	return this.PictogramNewPwd;
    }
    
}
