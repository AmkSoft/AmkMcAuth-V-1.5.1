package com.mooo.amksoft.amkmcauth.commands;

import org.apache.commons.lang.RandomStringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.mooo.amksoft.amkmcauth.AmkAUtils;
import com.mooo.amksoft.amkmcauth.AmkMcAuth;
import com.mooo.amksoft.amkmcauth.AuthPlayer;
import com.mooo.amksoft.amkmcauth.Config;
import com.mooo.amksoft.amkmcauth.Language;
import com.mooo.amksoft.amkmcauth.tools.SMTP;


public class CmdRecover implements CommandExecutor {

    @SuppressWarnings("unused") // Despite "unused": IT IS NEEDED in then onEnable Event !!!!!
	private final AmkMcAuth plugin;

    public CmdRecover(AmkMcAuth instance) {
        this.plugin = instance;
    }

    public boolean onCommand(CommandSender cs, Command cmd, String label, String[] args)
    {
        if (!(cs instanceof Player)) {
            cs.sendMessage(ChatColor.RED + AmkAUtils.colorize(Language.COMMAND_NO_CONSOLE.toString()));
            return true;
        }

  		return SendNewPwd((Player) cs, cmd.getName());
  	}


    public static boolean CmdSendNewPwd(CommandSender cs, String cmd)
    	{
  		return SendNewPwd((Player) cs, cmd);
    }
    
    private static boolean SendNewPwd(Player cs, String cmd) {
        if (cmd.equalsIgnoreCase("recoverpwd")) {        	

            final Player p = (Player) cs;
            final AuthPlayer ap = AuthPlayer.getAuthPlayer(p);

            if (!Config.emlFromEmail.contains("@")) {
                cs.sendMessage(ChatColor.RED + "Incorrect Config Email Setup.");
                return true;
            }
            if (!ap.getEmailAddress().contains("@")) {
                cs.sendMessage(ChatColor.RED + String.format(AmkAUtils.colorize(Language.PLAYER_INVALID_EMAILADDRESS.toString() + "."), ap.getUserName(), "'" + ap.getEmailAddress() + "'"));
                return true;
            }

       		String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
       		final String Password = RandomStringUtils.random( 7, characters );            	

			if (ap.setPassword(Password, ap.getHashType())) {
				new Thread(new Runnable() {
					@Override
					public void run() {        		    	
						String Player = ap.getUserName();
    				
						// SetUp Email Session
						SMTP.Email email = SMTP.createEmptyEmail();
						//email.add("Content-Type", "text/html"); //Headers for the email (useful for html) you do not have to set them
						//email.add("Content-Type", "text/plain");  //Default Header ("text/plain") for the email.you do not have to set
						email.from(Config.emlFromNicer, Config.emlFromEmail); //The sender of the email.
						email.to(Player, ap.getEmailAddress()); //The recipient of the email.
						email.subject(Config.recoversubject); //Subject of the email
						email.body(String.format(Config.recoverbodytxt, Player, Password));
						// All the email stuff here
						//SMTP.sendEmail(smtpServer, email, password, mail, debug);    			        
						SMTP.sendEmail(Config.emlSmtpServr,
										Config.emlLoginName, 
										Config.emlLoginPswd, 
										email, false);
					}
				}).start();

				cs.sendMessage(ChatColor.BLUE + AmkAUtils.colorize(Language.PASSWORD_RECOVER_MAIL.toString()));
				AmkMcAuth.getInstance().getLogger().info(p.getName() + " !!!! Recover Password send to Player");
				return true;
			}
			else 
				{ 
				cs.sendMessage(ChatColor.RED + AmkAUtils.colorize(Language.PASSWORD_COULD_NOT_BE_SET.toString()));
				AmkMcAuth.getInstance().getLogger().info(p.getName() + " !!!! Recover Password could not be set");
			}
        }
        return false;
    }
}
