package com.mooo.amksoft.amkmcauth.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.mooo.amksoft.amkmcauth.AuthPlayer;
import com.mooo.amksoft.amkmcauth.Config;
import com.mooo.amksoft.amkmcauth.Hasher;
import com.mooo.amksoft.amkmcauth.Language;
import com.mooo.amksoft.amkmcauth.AmkAUtils;
import com.mooo.amksoft.amkmcauth.AmkMcAuth;

import java.security.NoSuchAlgorithmException;

public class CmdChngPwd implements CommandExecutor {

    @SuppressWarnings("unused") // Despite "unused": IT IS NEEDED in the onEnable Event !!!!!
	private final AmkMcAuth plugin;

    public CmdChngPwd(AmkMcAuth instance) {
        this.plugin = instance;
    }

    public boolean onCommand(CommandSender cs, Command cmd, String label, String[] args)
    {
        if (!(cs instanceof Player)) {
            cs.sendMessage(ChatColor.RED + AmkAUtils.colorize(Language.COMMAND_NO_CONSOLE.toString()));
            return true;
        }

        String oldPassword="";
    	String newPassword="";

        if (args.length == 1) { // missing password value
        	if(args[0].contains(",")) {
        		//String myArray[] = new String[2];        		
            	String[] passwords = new String[2];
        		passwords = args[0].split(",") ;
            	if(Config.UseAutoMenu) { // Use inventory menu?
            		if(passwords[0].isEmpty()) passwords[0]="menu";
            		if(passwords[1].isEmpty()) passwords[1]="menu";
            	}
        		args = passwords;
        		//args[0] = passwords[0]; 
        		//args[1] = passwords[1]; 
        	}
        }

        if (args.length < 2) { // missing password value
        	if(Config.UseAutoMenu) { // Use inventory menu?
            	if(args.length==0) {
            		oldPassword = "menu";
                	newPassword = "menu";
            	}
            	if(args.length==1) {
                	oldPassword = args[0]; //read directly to args[]  BugFix by MDFM28 
                	newPassword = "menu";
            	}
        	}
        	else
        		{
        		//cs.sendMessage(cmd.getDescription());
        		cs.sendMessage(String.format(Language.USAGE_CHANGEPAS2.toString(), new Object[] { cmd }));
        		cs.sendMessage(Language.USAGE_CHANGEPAS1.toString());
        	
        		return false;
        	}
        }
        else
        	{
        	//String oldPassword = AmkAUtils.getFinalArg(args, 0).trim(); // support spaces
        	//String newPassword = AmkAUtils.getFinalArg(args, 1).trim();
        	//BugFix by MDFM28, see:  https://github.com/AmkSoft/AmkMcAuth-v1.4.4/pull/1/commits/804c99cd2dafe34d5cccf9b6846b5bf33f07339e
        	//The 'args' itself is an array which consist of oldpassword argument and newpassword argument,   BugFix by MDFM28 
        	oldPassword = args[0]; //read directly to args[]  BugFix by MDFM28 
        	newPassword = args[1]; //read directly to args[]  BugFix by MDFM28
        }
        
  		return ChgMyPswd((Player) cs, cmd.getName(), oldPassword, newPassword);
  	}


    public static boolean CmdChgMyPswd(CommandSender cs, String cmd, String args)
    {    	
    	String[] passwords;
    	String[] newpasswords = new String[2];

    	if(args.contains(","))
    		passwords = args.split(",") ;
    	else
    		passwords = args.split(" ") ;

        if(passwords.length<2 )	{ // Missing Password value
           	if(Config.UseAutoMenu) { // Use inventory menu?
           		if(passwords.length==0)	{
           			newpasswords[0]="menu";
           			newpasswords[1]="menu";
           		}
           		else
           			{
           			if(passwords[0]==null || passwords[0].length()==0) 
           				newpasswords[0]="menu";
           			else
           				newpasswords[0]=passwords[0];
           			newpasswords[1]="menu";
           		}
           	}
           	else
           	 	{
           		cs.sendMessage(String.format(Language.USAGE_CHANGEPAS0.toString(), new Object[] { cmd }));
           		cs.sendMessage(Language.USAGE_CHANGEPAS1.toString());

           		return false;
           	 }
    	}
        else
        	{
      		newpasswords[0]=passwords[0].trim(); 
      		newpasswords[1]=passwords[1].trim();
    		if(newpasswords[0]==null || newpasswords[0].length()==0) newpasswords[0]="menu";
    		if(newpasswords[1]==null || newpasswords[1].length()==0) newpasswords[1]="menu";
        }

  		return ChgMyPswd((Player) cs, cmd, newpasswords[0], newpasswords[1]);
    }
    
    private static boolean ChgMyPswd(Player cs, String cmd, String oldRawPassword, String newRawPassword) {
        if (cmd.equalsIgnoreCase("changepassword")) {
            if (!cs.hasPermission("amkauth.changepassword")) {
                AmkAUtils.dispNoPerms(cs);
                return true;
            }

            Player p = (Player) cs;
            AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
            if (!ap.isLoggedIn()) {
                cs.sendMessage(ChatColor.RED + AmkAUtils.colorize(Language.YOU_MUST_LOGIN.toString()));
                return true;
            }
            
            String newPassword;
            String oldMnuPassword;
            String oldNrmPassword;
            for (String disallowed : Config.disallowedPasswords) {
                if (disallowed.equals("#NoPlayerName#")) disallowed = ap.getUserName();
                if (!newRawPassword.equalsIgnoreCase(disallowed)) continue;
                cs.sendMessage(ChatColor.RED + AmkAUtils.colorize(Language.DISALLOWED_PASSWORD.toString()));
                return true;
            }

            try {
                oldNrmPassword = Hasher.encrypt(oldRawPassword, ap.getHashType());
                
                //if(ap.getPwdOldStatus().trim().equals("")) // Default
                //	oldMnuPassword = oldNrmPassword; //Hasher.encrypt(oldRawPassword, ap.getHashType());
                //else // Password via menu pictogram
                	oldMnuPassword = Hasher.encrypt(ap.getUserName() + oldRawPassword, ap.getHashType());

                //if(ap.getPwdNewStatus().equals(""))  // Default
                //    newPassword = Hasher.encrypt(newRawPassword, Config.passwordHashType);
                //else // Password via menu pictogram
                    newPassword = Hasher.encrypt(ap.getUserName() + newRawPassword, Config.passwordHashType);
            } catch (NoSuchAlgorithmException e) {
                cs.sendMessage(ChatColor.RED + AmkAUtils.colorize(Language.ADMIN_SET_UP_INCORRECTLY.toString()));
                cs.sendMessage(ChatColor.RED + AmkAUtils.colorize(Language.YOUR_PASSWORD_COULD_NOT_BE_CHANGED.toString()));
                return true;
            }

			ap.setPwdOldStatus(""); // Clear status for InventoryClick-Event 
			ap.setPwdNewStatus(""); // Clear status for InventoryClick-Event

            if(oldRawPassword.equals("menu")) { // Using pictograms for Password selection
    			ap.setPwdOldStatus(oldRawPassword); // Save for InventoryClick-Event 
    			ap.setPwdNewStatus(newRawPassword); // Save for InventoryClick-Event
    			
       			String PsswrdMessage = AmkAUtils.colorize(Language.OLD_PASSWORD_MSSGE.toString());
            	cs.openInventory(AmkAUtils.GetMenuInventory("Password", PsswrdMessage, null));
            	return true;
            }
            else {
            	// We have a (old/previous) password and it is NOT 'menu', check with known old Password. 
                if (!ap.getPasswordHash().equals(oldMnuPassword) & !ap.getPasswordHash().equals(oldNrmPassword) ) {
                    cs.sendMessage(ChatColor.RED + AmkAUtils.colorize(Language.OLD_PASSWORD_INCORRECT.toString()));
                    return true;
                }
            }
            
            if(newRawPassword.equals("menu")) { // Using pictograms for Password selection
    			ap.setPwdOldStatus(oldRawPassword); // Save for InventoryClick-Event
    			ap.setPwdNewStatus(newRawPassword); // Save for InventoryClick-Event
       			String PsswrdMessage = AmkAUtils.colorize(Language.NEW_PASSWORD_MSSGE.toString());
            	cs.openInventory(AmkAUtils.GetMenuInventory("Password", PsswrdMessage, null));
            	return true;
            }
            
            ap.setHashedPassword(newPassword, Config.passwordHashType);
           	cs.sendMessage(ChatColor.BLUE + AmkAUtils.colorize(Language.YOUR_PASSWORD_CHANGED.toString()));
            return true;
        }
        return false;
    }
}
