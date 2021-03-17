package com.mooo.amksoft.amkmcauth.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import com.mooo.amksoft.amkmcauth.AuthPlayer;
import com.mooo.amksoft.amkmcauth.Config;
import com.mooo.amksoft.amkmcauth.Language;
import com.mooo.amksoft.amkmcauth.AmkMcAuth;
import com.mooo.amksoft.amkmcauth.AmkAUtils;

public class CmdLogout implements CommandExecutor {

    @SuppressWarnings("unused") // Despite "unused": IT IS NEEDED in then onEnable Event !!!!!
    private final AmkMcAuth plugin;

    public CmdLogout(AmkMcAuth instance) {
        this.plugin = instance;
    }

    public boolean onCommand(CommandSender cs, Command cmd, String label, String[] args)
    {
        if (!(cs instanceof Player)) {
            cs.sendMessage(ChatColor.RED + AmkAUtils.colorize(Language.COMMAND_NO_CONSOLE.toString()));
            return true;
        }

        return LogMeOff((Player) cs, cmd.getName());
  	}


    public static boolean CmdLogMeOff(CommandSender cs, String cmd)
    {
  		return LogMeOff((Player) cs, cmd);
    }
    
    private static boolean LogMeOff(Player cs, String cmd) {
        if (cmd.equalsIgnoreCase("logout")) {
            if (!cs.hasPermission("amkauth.logout")) {
                AmkAUtils.dispNoPerms(cs);
                return true;
            }

            Player p = (Player) cs;
            AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
            if (!ap.isLoggedIn()) {
                cs.sendMessage(ChatColor.RED + AmkAUtils.colorize(Language.NOT_LOGGED_IN.toString()));
                return true;
            }
            cs.sendMessage(ChatColor.BLUE + AmkAUtils.colorize(Language.LOGGED_OUT.toString()));
            ap.setLastQuitTimestamp(System.currentTimeMillis());
            ap.setLastJoinTimestamp(System.currentTimeMillis());
            ap.logout(AmkMcAuth.getInstance(), true);

            if (Config.useHideInventory) ap.HideSurvivalInventory(p);
            
	        ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
            for (String playerAction : Config.playerActionLogof) {
    			if(!playerAction.trim().isEmpty()) {
    	            //Bukkit.dispatchCommand(console, playerAction.replace("$P", ap.getUserName()));

    	        	try {
    	        		if(playerAction.contains("AmkWait(")) 
    	        			AmkAUtils.createRunLaterCommand(ap.getUserName(), playerAction);
    	        		else
    	        			Bukkit.dispatchCommand(console, playerAction.replace("$P", ap.getUserName()));
    	        	} catch (Exception  error  ) {
    	        		AmkMcAuth.getInstance().getLogger().info("Error OnLogof Executing: " + playerAction.replace("$P", ap.getUserName()) );
    	            	error.printStackTrace();
    	        	}
    	            
    				//AmkMcAuth.MyQueue.Put("executeConsoleCommand:~" + playerAction.replace("$P", ap.getUserName()));
    			}
    		}
            
            return true;
        }
        return false;
    }

}
