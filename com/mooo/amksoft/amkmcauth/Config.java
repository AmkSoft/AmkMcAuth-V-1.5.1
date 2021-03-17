package com.mooo.amksoft.amkmcauth;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.util.FileUtil;

public class Config {

    public static boolean disableIfOnlineMode;
    public static boolean requireLogin;
    public static boolean kickIfAlreadyOnline;
    public static boolean KickOnPasswordFail;
    public static boolean useLoginPermission;
    public static boolean emailForceSet;
    public static String recoversubject;
    public static String recoverbodytxt;
    public static long emailRemindInterval;
    public static String loginPermission;
    public static String registrationType;
    public static String emlFromNicer;
    public static String emlFromEmail;
    public static String emlSmtpServr;
    public static String emlLoginName;
    public static String emlLoginPswd;    
    public static String emailsubject;
    public static String emailbodytxt;    
    public static String confirmbodytxt;    
    public static long emailWaitKick;    
    public static boolean allowChat;
    public static String chatPrefix;
    public static boolean allowCommands;
    public static List<String> allowedCommands;
    public static boolean allowMovementWalk;
    public static boolean allowMovementLook;
    public static long allowMovementTime;
    public static boolean godMode;
    public static boolean godModeAfterLogin;
    public static long godModeLength;
    public static boolean remindEnabled;
    public static long remindInterval;
    public static long saveUserdataInterval;
    public static boolean useHideInventory;
    public static boolean sessionsEnabled;
    public static boolean sessionsCheckIP;
    public static long sessionLength;
    public static long removeAfterDays;
    public static String sessionType;
    public static List<String> disallowedPasswords;
    public static List<String> disallowedEmlAdresses;
    public static String passwordHashType;
    public static boolean validateUsernames;
    public static String usernameRegex;
    public static boolean invisibleMode;
    public static List<String> playerActionSJoin;
    public static List<String> playerActionSJReg;    
    public static List<String> playerActionSJGrc;
    public static List<String> playerActionLogin;
    public static List<String> playerActionLogof;
    public static List<String> playerActionLeave;
    public static boolean adventureMode;
    public static boolean teleportToSpawn;
    public static String spawnWorld;
    public static boolean useSpawnAt;
    public static long spawnWorldX;
    public static long spawnWorldY;
    public static long spawnWorldZ;
    public static boolean kickPlayers;
    public static long kickAfter;
    public static boolean checkOldUserdata;
    public static long maxUsersPerIpaddress;
    public static long maxUsersPerEmaddress;
    public static String MySqlDbFile;
    public static String MySqlDbHost;
    public static String MySqlDbPort;
    public static String MySqlDbDbse;
    public static String MySqlOption; 
    public static String MySqlDbUser;
    public static String MySqlDbPswd;
    public static boolean metricsEnabled;
    public static boolean UseCapcha;
    public static boolean UseAutoMenu;
    public static boolean ShowMenuOption;
    
    private boolean NewSettng = false;

    private final AmkMcAuth plugin;

    
    public Config(AmkMcAuth instance) {
        this.plugin = instance;
        final File config = new File(plugin.getDataFolder(), "config.yml");
        if (!config.exists()) {
            if (!config.getParentFile().mkdirs()) 
            	this.plugin.getLogger().warning("Could not create config.yml directory.");
            this.plugin.saveDefaultConfig();            
        }
        this.reloadConfiguration();
    }

    /**
     * Player configuration manager.
     *
     * @param Configuration-File Name, Config-Element Name,  Default value
     */
    private String GetSetting(FileConfiguration ConfigFile, String ConfigName, String Default) {
    	String RetVal = "";
        if(ConfigFile.isSet(ConfigName)) {
        	RetVal= ConfigFile.getString(ConfigName);
        }
        else
        	{
        	ConfigFile.set(ConfigName, Default);        	
        	NewSettng=true;
        	RetVal = Default;
        	this.plugin.getLogger().info("Config.yml Check: New Setting: " + ConfigName + " added!");
        }
        return RetVal;
    }
    private Long GetSetting(FileConfiguration ConfigFile, String ConfigName, Long Default) {
    	Long RetVal = 0L;
        if(ConfigFile.isSet(ConfigName)) {
        	RetVal= ConfigFile.getLong(ConfigName);
        }
        else
        	{
        	ConfigFile.set(ConfigName, Default);        	
        	NewSettng=true;
        	RetVal = Default;
        	this.plugin.getLogger().info("Config.yml Check: New Setting: " + ConfigName + " added!");
        }
        return RetVal;
    }
    private boolean GetSetting(FileConfiguration ConfigFile, String ConfigName, boolean Default) {
    	boolean RetVal = false;
        if(ConfigFile.isSet(ConfigName)) {
        	RetVal= ConfigFile.getBoolean(ConfigName);
        }
        else
        	{
        	ConfigFile.set(ConfigName, Default);        	
        	NewSettng=true;
        	RetVal = Default;
        	this.plugin.getLogger().info("Config.yml Check: New Setting: " + ConfigName + " added!");
        }
        return RetVal;
    }
    private List<String> GetSetting(FileConfiguration ConfigFile, String ConfigName, List<String> Default) {
    	List<String> RetVal = null;
        if(ConfigFile.isSet(ConfigName)) {
        	RetVal= ConfigFile.getStringList(ConfigName);
        }
        else
        	{
        	ConfigFile.set(ConfigName, Default);        	
        	NewSettng=true;
        	RetVal = Default;
        	this.plugin.getLogger().info("Config.yml Check: New Setting: " + ConfigName + " added!");
        }
        return RetVal;
    }

    public void reloadConfiguration() {
        this.plugin.reloadConfig(); 
        final FileConfiguration c = this.plugin.getConfig();

    	NewSettng=false;

        requireLogin		= GetSetting(c,"login.require"						,true);
        UseCapcha			= GetSetting(c,"login.CapchaOnJoin"					,false);
        UseAutoMenu         = GetSetting(c,"login.AssumeAutoMenu"				,false);
        ShowMenuOption		= GetSetting(c,"login.ShowMenuBlockOption"			,false);
        disableIfOnlineMode	= GetSetting(c,"login.disable_if_online_mode"		,false);
        kickIfAlreadyOnline	= GetSetting(c,"login.kick_if_already_online"		,false);
        useHideInventory	= GetSetting(c,"login.hide_inventory_on_join"		,false);
        KickOnPasswordFail	= GetSetting(c,"login.kick_on_password_fail"		,false);

        useLoginPermission	= GetSetting(c,"login.permission.enabled"			,false);
        loginPermission		= GetSetting(c,"login.permission.permission"		,"amkauth.requirelogin");

    	registrationType	= GetSetting(c,"login.registration"					,"password");
    	emailForceSet		= GetSetting(c,"login.forcesetmail"					,false);
        emailRemindInterval = GetSetting(c,"login.emailremindtime"				,120L);
        emlFromNicer        = GetSetting(c,"login.regemlfromnice"				,"Server Nice Name");
        emlFromEmail        = GetSetting(c,"login.regemlfromemail"				,"Sender-Email-Address");
        emlSmtpServr        = GetSetting(c,"login.regemlsmtpservr"				,"Smtp-Mail-Server");
        emlLoginName        = GetSetting(c,"login.regemlloginname"				,"Smtp-Login-name");
        emlLoginPswd        = GetSetting(c,"login.regemlloginpswd"				,"Smtp-Login-Password");
        emailsubject        = GetSetting(c,"login.emailsubject"					,"MineCraft Player Registration Information");
        emailbodytxt        = GetSetting(c,"login.emailbodytxt"					,"Login Password for Player: %1$s is set to %2$s\\nYou can change this after login using the changepassword command\\nHappy Mining on our MineCraft Server.");
        confirmbodytxt      = GetSetting(c,"login.confirmbodytxt"				,"Confirm your new email-address by issuing\\nthis player command: %1$s");
        emailWaitKick       = GetSetting(c,"login.email_wait_kick"				,120L);
        recoversubject   	= GetSetting(c,"login.recoversubject"				,"Requested login password recovery");
        recoverbodytxt   	= GetSetting(c,"login.recoverbodytxt"				,"Login Password for Player: %1$s is reset to %2$s\\\\nYou can change this after login using the changepassword command\\\\nHappy Mining on our MineCraft Server.\"");
        
        disallowedEmlAdresses=GetSetting(c,"emailaddresses.disallowed"			,Arrays.asList("spamgourmet.","@guerillamail"));        
        
        allowChat			= GetSetting(c,"login.restrictions.chat.allowed"	,false);
        chatPrefix			= GetSetting(c,"login.restrictions.chat.prefix"		,"[NLI] ");

        allowCommands		= GetSetting(c,"login.restrictions.commands.allowed",false);
        allowedCommands		= GetSetting(c,"login.restrictions.commands.exempt"	,Arrays.asList("!","we cui"));        

        allowMovementWalk	= GetSetting(c,"login.restrictions.movement.walk"	,false);
        allowMovementLook	= GetSetting(c,"login.restrictions.movement.look_around",true);
        allowMovementTime	= GetSetting(c,"login.restrictions.movement.allowmovetime",0L);

        godMode				= GetSetting(c,"login.godmode.enabled"				,true);
        godModeAfterLogin	= GetSetting(c,"login.godmode.after_login.enabled"	,true);
        godModeLength		= GetSetting(c,"login.godmode.after_login.length"	,10L);

        remindEnabled		= GetSetting(c,"login.remind.enabled"				,true);
        remindInterval		= GetSetting(c,"login.remind.interval"				,10L);
        kickPlayers			= GetSetting(c,"login.remind.kick.enabled"			,false);
        kickAfter			= GetSetting(c,"login.remind.kick.wait"				,30L);

        sessionsEnabled		= GetSetting(c,"sessions.enabled"					,true);
        sessionsCheckIP		= GetSetting(c,"sessions.check_ip"					,true);
        sessionLength		= GetSetting(c,"sessions.length"					,15L);
        sessionType			= GetSetting(c,"sessions.LoginCommandsMessage"		,"Commands");        
        //sessionType = "Commands";  // OverRuled!!, Not working!!!

        playerActionSJoin	= GetSetting(c,"sessions.OnJoin"					,Arrays.asList(""));
        playerActionSJReg	= GetSetting(c,"sessions.OnRgstr"					,Arrays.asList(""));        
        playerActionSJGrc	= GetSetting(c,"sessions.OnGrace"					,Arrays.asList(""));        
        playerActionLogin	= GetSetting(c,"sessions.OnLogin"					,Arrays.asList(""));        
        playerActionLogof	= GetSetting(c,"sessions.OnLogof"					,Arrays.asList(""));        
        playerActionLeave	= GetSetting(c,"sessions.OnExit"					,Arrays.asList(""));        

        disallowedPasswords	= GetSetting(c,"passwords.disallowed"				,Arrays.asList("password","[password]"));        
        passwordHashType	= GetSetting(c,"passwords.hash_type"				,"AMKAUTH");

        // In the config.yml file this can be: `regex: "[\\w]{2,16}"`  or  `regex: '[\w]{2,16}'`
        // They are both correct, using single quote no escaping of the '\' escape character needed.
        validateUsernames	= GetSetting(c,"usernames.verify"					,true);
        usernameRegex		= GetSetting(c,"usernames.regex"					,"[\\w]{2,16}");

        invisibleMode		= GetSetting(c,"login.invisible_mode"				,false);
        adventureMode		= GetSetting(c,"login.adventure_mode"				,false);        
        teleportToSpawn		= GetSetting(c,"login.teleport_to_spawn"			,false);
        
        spawnWorld			= GetSetting(c,"login.tpToSpawnTrue.teleportToSpawnWorld","");
        useSpawnAt			= GetSetting(c,"login.tpToSpawnTrue.useSpawnAtLocation",false);  
        spawnWorldX			= GetSetting(c,"login.tpToSpawnTrue.spawnAtLocationX",0L);
        spawnWorldY			= GetSetting(c,"login.tpToSpawnTrue.spawnAtLocationY",66L);
        spawnWorldZ			= GetSetting(c,"login.tpToSpawnTrue.spawnAtLocationZ",0L);

        MySqlDbFile			= GetSetting(c,"saving.mysql_filemode"				,"Save");
        MySqlDbHost			= GetSetting(c,"saving.mysql_hostname"				,"");
        MySqlDbPort			= GetSetting(c,"saving.mysql_portnmbr"				,"3306");
        MySqlDbDbse			= GetSetting(c,"saving.mysql_database"				,"AmkMcData");
        MySqlOption			= GetSetting(c,"saving.mysql_dboption"				,"useSSL=false&autoReconnect=true");
        MySqlDbUser			= GetSetting(c,"saving.mysql_username"				,"AmkMcUser");
        MySqlDbPswd			= GetSetting(c,"saving.mysql_password"				,"AmkMcPswd");
        
        checkOldUserdata	= GetSetting(c,"saving.check_old_userdata"			,true);
        saveUserdataInterval= GetSetting(c,"saving.interval"					,10L);
        removeAfterDays 	= GetSetting(c,"saving.remove_inactive_after"		,99L);

        maxUsersPerIpaddress= GetSetting(c,"general.users_per_ipaddress"		,0L);
        maxUsersPerEmaddress= GetSetting(c,"general.users_per_emailaddress"		,0L);
        
        metricsEnabled		= GetSetting(c,"general.metrics_enabled"			,true);
        
        //-- Check for invalid inputs and set to default if invalid --//

        if (remindInterval < 1L)       remindInterval 		= 30L;
        if (emailRemindInterval < 1L)  emailRemindInterval	= 120L;
        if (saveUserdataInterval < 1L) saveUserdataInterval	= 5L;
        if (sessionLength < 1L)        sessionLength 		= 15L;
        if (kickAfter < 0L)            kickAfter 			= 0L;
        if (godModeLength <= 0L)       godModeLength 		= 10L;
        if (maxUsersPerIpaddress < 0L) maxUsersPerIpaddress	= 0L;
        if (maxUsersPerEmaddress < 0L) maxUsersPerEmaddress	= 0L;
        if (MySqlDbHost.equals("") && MySqlDbFile.equals("")) MySqlDbFile = "Save"; 

        if(NewSettng) { // We found new settings in the plugin NOT in the config.yml.
        	Date now = new Date();
        	SimpleDateFormat smdf = new SimpleDateFormat("yyyMMdd-HHmm"); 
        	String BackUp = ".backup." + smdf.format(now);
        	File xo = new File(plugin.getDataFolder(), "config.yml");
        	File xn = new File(plugin.getDataFolder(), "config.yml" + BackUp);
        	FileUtil.copy(xo,xn);
            this.plugin.saveConfig(); // Save new Configuration

        	this.plugin.getLogger().info("configuration backupped and recreated due to missing setting(s) ");
        }


        this.plugin.getLogger().info("Configuration settings loaded/reloaded from config.yml.");
    
    }

}
