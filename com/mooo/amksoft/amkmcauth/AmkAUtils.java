package com.mooo.amksoft.amkmcauth;

import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.command.ConsoleCommandSender;

import com.google.common.base.Charsets;
import com.mooo.amksoft.amkmcauth.commands.CmdAmkAuth;
import com.mooo.amksoft.amkmcauth.tools.NameFetcher;
import com.mooo.amksoft.amkmcauth.tools.UUIDFetcher;

public class AmkAUtils {

    static public Material Blocks[] = {
        	Material.ACACIA_BOAT,		//  0
        	Material.BEEF,				//  1
        	Material.ACACIA_PLANKS,		//  2
        	Material.ANDESITE,			//  3
        	Material.DIRT,				//  4
        	Material.ANDESITE_WALL,		//  5
        	Material.BAMBOO,			//  6
        	Material.BIRCH_DOOR,		//  7
        	Material.BAKED_POTATO,		//  8
        	Material.BIRCH_FENCE,		//  9
        	Material.BIRCH_TRAPDOOR,	// 10
        	Material.BOOKSHELF,			// 11
        	Material.BLAST_FURNACE,		// 12
        	Material.CYAN_BED,			// 13
        	Material.HOPPER,			// 14
        	Material.DEAD_BUSH,			// 15
        	Material.HONEY_BOTTLE,		// 16
        	Material.DIORITE_SLAB,		// 17
        	Material.GREEN_BANNER,		// 18
        	Material.SMOOTH_STONE,		// 19
        	Material.FEATHER,			// 20
        	Material.END_ROD,			// 21
        	Material.LIME_WOOL,			// 22
        	Material.NETHER_BRICK,		// 23
        	Material.OAK_SAPLING,		// 24
        	Material.POLISHED_DIORITE,	// 25
        	Material.RED_SAND,			// 26
        	Material.STONE_SWORD,		// 27
        	Material.PINK_CONCRETE,		// 28
        	Material.CARVED_PUMPKIN,	// 29
        	Material.QUARTZ_BLOCK,		// 30
        	Material.TRAPPED_CHEST,		// 31
        	Material.MAGMA_BLOCK,		// 32
        	Material.COBBLESTONE,		// 33
        	Material.CHORUS_FLOWER,		// 34
        	Material.GLOWSTONE,			// 35
        	Material.BRICK_WALL,		// 36
        	Material.HONEY_BLOCK,		// 37
        	Material.PINK_SHULKER_BOX,	// 38
        	Material.DARK_OAK_STAIRS,	// 39
        	Material.JUNGLE_LEAVES,		// 40
        	Material.JUNGLE_WOOD,		// 41
        	Material.COAL_BLOCK,		// 42
        	Material.WOODEN_SHOVEL,		// 43
        	Material.WHEAT_SEEDS,		// 44
        	Material.WHITE_CARPET,		// 45
        	Material.STONE_BRICK_WALL,	// 46
        	Material.SALMON_BUCKET,		// 47
        	Material.SEA_LANTERN,		// 48
        	Material.TIPPED_ARROW		// 49
        };
    public static int MaxBlockCnt = Blocks.length;
    
    public static void dispNoPerms(CommandSender cs) {
        cs.sendMessage(ChatColor.RED + colorize(Language.NO_PERMISSION.toString()));
    }

    /**
     * Converts color codes to processed codes
     *
     * @param message Message with raw color codes, even the new 1.16 RGB coplors (like #202020)
     * @return String with processed colors
     */
    public static String colorize(final String message) {
        if (message == null) return null;

        // This peace of code is taken from a plugin named: ChatColorPlus-2.6
        // Developer of ChatColorPlus, thank you. I hope you don't mind i used your code.
        // "Better use good/working code from someone else than to write own crappy code."
        // Also: https://www.spigotmc.org/wiki/textcosmetics-colors-and-formats/
        // https://www.spigotmc.org/resources/textcosmetics-make-your-text-come-alive.32843/
        // https://www.spigotmc.org/threads/hex-color-code-translate.449748/#post-3867804        
        
        String text = message;  
        // SpecialChar: "§" is named: SectionSign, info: HTML Special characters.
        text = text.replaceAll("%Bold%", "§l");
        text = text.replaceAll("%Reset%", "§r");
        text = text.replaceAll("%Magic%", "§k");
        text = text.replaceAll("%Obfuscated%", "§k");
        text = text.replaceAll("%Strike%", "§m");
        text = text.replaceAll("%Strikethrough%", "§m");
        text = text.replaceAll("%Underline%", "§n");
        text = text.replaceAll("%Italic%", "§o");
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
          if (i < text.length() - 9) {
            String temp = text.substring(i, i + 9);
            if (temp.startsWith("%#") && temp.endsWith("%")) {
              try {
                Integer.parseInt(temp.substring(2, 8), 16);
                sb.append("§x");
                char[] c = temp.toCharArray();
                for (int i1 = 2; i1 < c.length - 1; i1++)
                  sb.append("§").append(c[i1]); 
                i += 8;
              } catch (NumberFormatException numberFormatException) {}
            } else {
              sb.append(text.charAt(i));
            } 
          } else {
            sb.append(text.charAt(i));
          } 
        }        
        
        text = sb.toString();
        
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    /**
     * Removes color codes that have not been processed yet (&char)
     * <p/>
     * This fixes a common exploit where color codes can be embedded into other codes:
     * &&aa (replaces &a, and the other letters combine to make &a again)
     *
     * @param message String with raw color codes
     * @return String without raw color codes
     */
    public static String decolorize(String message) {
        Pattern p = Pattern.compile("(?i)&[a-f0-9k-or]");
        boolean contains = p.matcher(message).find();
        while (contains) {
            message = message.replaceAll("(?i)&[a-f0-9k-or]", "");
            contains = p.matcher(message).find();
        }
        return message;
    }

    /**
     * Creates a task to remind the specified CommandSender with the specified message every specified interval.
     * <p/>
     * If kicks are enabled, this will kick the player after the specified (in the config).
     *
     * @param p        CommandSender to send the message to
     * @param pl       Plugin to register the task under
     * @param message  Message to send (will handle color codes and send new messages on \n)
     * @param interval Interval in ticks to send the message
     * @return Task created
     */
    private static BukkitTask createReminder(final Player p, Plugin pl, final String message, final long interval) {
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                if (Config.kickPlayers) {
                    AuthPlayer ap = AuthPlayer.getAuthPlayer(p.getUniqueId());
                    if (!ap.isLoggedIn() && ap.getLastJoinTimestamp() + (Config.kickAfter * 1000L) <= System.currentTimeMillis()) {
                        Player p = ap.getPlayer();
                        if (p != null)	p.kickPlayer(colorize(Language.TOOK_TOO_LONG_TO_LOG_IN.toString()));
                    }
                }
                for (String line : message.split("\\n")) p.sendMessage(colorize(line));
            }
        };
        return pl.getServer().getScheduler().runTaskTimer(pl, r, 0L, interval);
    }
    private static BukkitTask createEmailReminder(final Player p, Plugin pl, final String message, final long interval) {
        final Runnable r = new Runnable() {
            @Override
            public void run() {
            	AuthPlayer ap = AuthPlayer.getAuthPlayer(p.getUniqueId());
                if (!ap.isLoggedIn() && ap.getLastJoinTimestamp() + (Config.emailWaitKick * 1000L) <= System.currentTimeMillis()) {
                	Player p = ap.getPlayer();
                	if (p != null) { 
                		p.kickPlayer(colorize(Language.TOOK_TOO_LONG_TO_LOG_IN.toString()));
                		
    					PConfManager.removePlayerFromIp(ap.getCurrentIPAddress()); // "192.168.1.7"                		
                		PConfManager.removePlayer(p.getUniqueId());
                        PConfManager.removeAllPlayer(ap.getUserName().toLowerCase());
                        ap.removeAuthPlayer(ap.getUniqueId());
                		p.remove();                		
                	}
                }
                for (String line : message.split("\\n")) p.sendMessage(colorize(line));
            }
        };
        return pl.getServer().getScheduler().runTaskTimer(pl, r, 0L, interval);
    }
    
    private static BukkitTask createRunLater(final String PlayerName, final String playerAction, final long NumberOfTicks) {
        // NumberOfTicks" 20 ticks=1 second, we only use TICKs in this function.
		new BukkitRunnable() {
		    @Override
		    public void run() {
	            final Runnable r = new Runnable() {
	                @Override
	                public void run() {
	    				ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();		
	                	//AmkMcAuth.getInstance().getLogger().info("AmkWait!"); // Just for Timer Interval Debugging
	    		        Bukkit.dispatchCommand(console, playerAction.replace("$P", PlayerName));
	                }
	            };
      	        Bukkit.getServer().getScheduler().runTaskLater(AmkMcAuth.getInstance(), r, NumberOfTicks);
		    }
		}.runTaskAsynchronously(AmkMcAuth.getInstance());     
		return null;
    }

    // TaskTimer interval runs on "in-game ticks", so 1 second = 20 ticks! 
    // Thats why we CALCULATE ReminderTaskTimer as SECONDS times 20 (ticks)
    
    public static BukkitTask createRegisterReminder(Player p, Plugin pl) {
    	String RegType;
    	String SendMsg;
    	String ExtMsg="";
    	String Command="";

        if(Config.sessionType.contains("HiddenChat"))
        	Command = "t\\register";
		else
			Command = "/register";

        if(Config.registrationType.equalsIgnoreCase("email")) {
			RegType = colorize(Language.EMAIL_ADDRESS.toString()); //"email address";
		} else {
			RegType = colorize(Language.PASSWORD.toString()); //"password";
	    	if(Config.ShowMenuOption) { 
	    		if(Config.UseAutoMenu)
	    			ExtMsg="\n"+ ChatColor.BLUE + String.format(Language.USAGE_AUTOPASSWRD.toString(), Command);
	    		else
	    			ExtMsg="\n"+ ChatColor.BLUE + String.format(Language.USAGE_MENUPASSWRD.toString(), "menu");
	    	}			
		}

		if(Config.sessionType.contains("HiddenChat"))
			SendMsg = ChatColor.RED + colorize(Language.PLEASE_REGISTER_WITH0.toString()) + ":" + ChatColor.GRAY + " " + Command;
		else
			SendMsg = ChatColor.RED + colorize(Language.PLEASE_REGISTER_WITH1.toString()) + ":" + ChatColor.GRAY + " " + Command;
		return createReminder(p, pl, SendMsg + " [" + RegType + "]" + ChatColor.RED + "!" + ExtMsg, Config.remindInterval * 20L);
    }

    public static BukkitTask createSetEmailReminder(Player p, Plugin pl) {
		String RegType = colorize(Language.EMAIL_ADDRESS.toString()); //"email address";
	    return createReminder(p, pl, ChatColor.RED + colorize(Language.PLEASE_SETEMAIL_WITH.toString()) + ":" + ChatColor.GRAY + " /setemail [" + RegType + "]" + ChatColor.RED + "!", Config.emailRemindInterval * 20L);
    }

    public static BukkitTask createLoginReminder(Player p, Plugin pl) {
		String RegType = colorize(Language.PASSWORD.toString()); //"password";
    	String SendMsg;
    	String ExtMsg="";
    	String Command="";

        if(Config.sessionType.contains("HiddenChat"))
        	Command = "t\\login";
		else
			Command = "/login";

    	if(Config.ShowMenuOption) { 
    		if(Config.UseAutoMenu)
    			ExtMsg="\n"+ ChatColor.BLUE + String.format(Language.USAGE_AUTOPASSWRD.toString(), Command);
    		else
    			ExtMsg="\n"+ ChatColor.BLUE + String.format(Language.USAGE_MENUPASSWRD.toString(), "menu");
    	}

    	if(Config.sessionType.contains("HiddenChat"))
    		SendMsg = ChatColor.RED + colorize(Language.PLEASE_LOG_IN_WITH0.toString()) + ":" + ChatColor.GRAY + " " + Command;
		else
			SendMsg = ChatColor.RED + colorize(Language.PLEASE_LOG_IN_WITH1.toString()) + ":" + ChatColor.GRAY + " " + Command;
		return createReminder(p, pl, SendMsg + " [" + RegType + "]" + ChatColor.RED + "!" + ExtMsg, Config.remindInterval * 20L);
    }

    public static BukkitTask createLoginEmailReminder(Player p, Plugin pl) {
		String RegType = colorize(Language.PASSWORD.toString()); //"password";
		if(Config.sessionType.contains("HiddenChat"))
	        return createEmailReminder(p, pl, ChatColor.RED + colorize(Language.PLEASE_LOG_IN_WITH0.toString()) + ":" + ChatColor.GRAY + " t\\login [" + RegType + "]" + ChatColor.RED + "!", Config.remindInterval * 20L);
		else
			return createEmailReminder(p, pl, ChatColor.RED + colorize(Language.PLEASE_LOG_IN_WITH1.toString()) + ":" + ChatColor.GRAY + " /login [" + RegType + "]" + ChatColor.RED + "!", Config.remindInterval * 20L);
    }

    // TaskTimer interval runs on "in-game ticks", so 1 second = 20 ticks! 
    // We USE TICKS to create RunLater-Commands, not SECONDS this time.
    public static BukkitTask createRunLaterCommand(String PlayerName, String CommandUsingAmkWait) {
		//Get value first parameter (not explicit "AmkWait()" and remove the parameter from the string
    	CommandUsingAmkWait = CommandUsingAmkWait.trim();
    	String CommandToSend= CommandUsingAmkWait.substring(CommandUsingAmkWait.indexOf(' ')+1);

		long NumberOfTicks=0L;
		try {
			NumberOfTicks = Long.parseLong(CommandUsingAmkWait.split("[\\(\\)]")[1]); // In Ticks
	        return createRunLater(PlayerName, CommandToSend, NumberOfTicks);
		} catch (NumberFormatException nfe) {
			//System.out.println("NumberFormatException: " + nfe.getMessage());
			AmkMcAuth.getInstance().getLogger().info("Error AmkWait() NumberFormatException: (" + NumberOfTicks + ") " + CommandUsingAmkWait);
			nfe.printStackTrace();
		}
		return null;
    }
    public static BukkitTask createRunLaterCommand(String PlayerName, String ConsoleCommand, long WaitBeforeRun) {
        return createRunLater(PlayerName, ConsoleCommand, WaitBeforeRun);
    }

    /**
     * Creates a task to save the UserData on regular basis using the Config.saveUserdata Interval.
     * Config.saveUserdata states interval in minutes.
     * TaskTimer interval is in-game ticks, so 1 second = 20 ticks
     * To wait 1 minute set: minutes * 60 * 20 ticks interval on TaskTimer. 
     *
     * @param pl       Plugin to register the task under
     * @return Task created
     */
    public static BukkitTask createSaveTimerExec(Plugin pl) {
    	//final Plugin thisPl = pl; // Just for Timer Interval Debugging
        final Runnable r = new Runnable() {
            @Override
            public void run() {
            	//thisPl.getLogger().info("User profile data AutoSave Task is triggered!"); // Just for Timer Interval Debugging
                PConfManager.saveAllManagers("BackGround");                
            }
        };
        final long interval=Config.saveUserdataInterval * 60L * 20L; // 60 seconds/minute, 20 ticks/second
        // 0L is wait time before first Run, Setting it to interval skips Timer in StartUp..
    	//BukkitTask thisTask = pl.getServer().getScheduler().runTaskTimer(pl, r, 0L, interval);
    	BukkitTask thisTask = pl.getServer().getScheduler().runTaskTimer(pl, r, interval, interval);
    	pl.getLogger().info("User profile data AutoSave Task is started (interval: " + Config.saveUserdataInterval +" minutes)" );
        return thisTask;
        //return pl.getServer().getScheduler().runTaskTimer(pl, r, 0L, interval);
    }
    
    /**
     * Creates a task to Check AmkMcAuth current version.
     * TaskTimer interval is in-game ticks, so 1 second = 20 ticks
     * To wait 1 minute set: minutes * 60 * 20 ticks interval on TaskTimer. 
     *
     * @param pl       Plugin to register the task under
     * @return Task created
     */
    public static BukkitTask ChkVersionTimerExec(Plugin pl) {
    	//final Plugin thisPl = pl; // Just for Timer Interval Debugging
        final Runnable r = new Runnable() {
            @Override
            public void run() {
            	//thisPl.getLogger().info("Check AmkMcAuth current version is triggered!"); // Just for Timer Interval Debugging
            	CmdAmkAuth.CheckDevMessage(Bukkit.getConsoleSender());                
            }
        };
        // Ever X day(s), 24 hour/day, 60 minutes/hour, 60 seconds/minute, 20 ticks/second
        final long daysInterval = 5L; // every x days a version check-run
        final long interval= daysInterval * 24L * 60L * 60L * 20L;
        // 0L is wait time before first Run, Setting it to interval skips Timer in StartUp..
    	//BukkitTask thisTask = pl.getServer().getScheduler().runTaskTimer(pl, r, 0L, interval);
    	BukkitTask thisTask = pl.getServer().getScheduler().runTaskTimer(pl, r, interval, interval);
    	pl.getLogger().info("Check-AmkMcAuth-Version Task is started (interval: " + daysInterval +" days)" );
        return thisTask;
        //return pl.getServer().getScheduler().runTaskTimer(pl, r, 0L, interval);
    }
    
    public static BukkitTask createSaveTimer(Plugin pl) {
        return createSaveTimerExec(pl);
    }

    public static BukkitTask createChkVersionTimer(Plugin pl) {
        return ChkVersionTimerExec(pl);
    }

    
    
    /**
     * Joins an array of strings with spaces
     *
     * @param array    Array to join
     * @param position Position to start joining from
     * @return Joined string
     */
    public static String getFinalArg(String[] array, int position) {
        final StringBuilder sb = new StringBuilder();
        for (int i = position; i < array.length; i++) sb.append(array[i]).append(" ");
        return sb.substring(0, sb.length() - 1);
    }

    public static UUID getUUID(String name) throws Exception {
        boolean Online=true;
        
    	if(Bukkit.getOnlineMode()!= Online) 
		{
    		// Server runs 'OffLine' AmkMcAuth calculates the UUID for this player...
    	    return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(Charsets.UTF_8));    		
		}
    	else
    		{
    		// Server runs 'OnLine' Let Mojang calculate the UUID for this player...
   	        final Map<String, UUID> m = new UUIDFetcher(Arrays.asList(name)).call();
   	        for (Map.Entry<String, UUID> e : m.entrySet()) {
   	            if (e.getKey().equalsIgnoreCase(name)) return e.getValue();
   	        }
   	        throw new Exception("Couldn't find name in results.");
        }
        //final Map<String, UUID> m = new UUIDFetcher(Arrays.asList(name)).call();
        //for (Map.Entry<String, UUID> e : m.entrySet()) {
        //    if (e.getKey().equalsIgnoreCase(name)) return e.getValue();
        //}
        //throw new Exception("Couldn't find name in results.");
    }

    public static String getName(UUID u) throws Exception {
        return new NameFetcher(Arrays.asList(u)).call().get(u);
    }

    public static String forceGetName(UUID u) {
        String name;
        try {
            name = AmkAUtils.getName(u);
        } catch (Exception ex) {
            name = u.toString();
        }
        return name;
    }
    
	public static void showCapchaPopup(Player p, long WaitTime, String CapchaMessage, Material KlickBlock) {
		//System.out.println("Checking Inventory: " + p.getOpenInventory().getTitle().toString() );
		if(!p.getOpenInventory().getTitle().contains("Capcha")) {
			// Player has not yet the Capcha inventory popup shown, Server still loading??
			p.openInventory(GetMenuInventory("Capcha", CapchaMessage, KlickBlock));			

			// try again in WaitTime ticks 
			Bukkit.getScheduler().runTaskLater(AmkMcAuth.getInstance(), new Runnable() {
				public void run() {
					//p.openInventory(inv);
					AmkAUtils.showCapchaPopup(p, WaitTime, CapchaMessage,KlickBlock);
				}
			}, WaitTime); // wait WaitTime ticks on player-server login completion
		}		
	}
    
    public static Inventory GetMenuInventory(String Type, String Title, Material CapchaBlock) {
    	// Type = "Capcha" (create Capcha Inventory) or "Password" (create Password Inventory)
    	// Type is NOT Inventory Type (getType is something completely different)!!!!
    	int InvLength=0;
    	int CrrntBlock=0;
    	
    	if(Type.contains("Capcha")) {
    		InvLength=1*9;
    	}
    	else
    	{
    		InvLength=5*9;    		
    		// Have to be sure that the first/top random Block-range is within Inventory-range,
    		// So we never "forget" the potential Blocks used as PassWord as the random Block-range might be bigger.
        	if(MaxBlockCnt>InvLength) // The block-length can be longer then the Inventory list.
            	MaxBlockCnt=InvLength; // We use ONLY the first InvLength blocks (MaxBlockCnt set to InvLength).
    	}

    	Inventory inv = Bukkit.createInventory(null, InvLength, Title); // create inventory using InvLength slots and set "title"
		boolean capchaFound = false;
		CrrntBlock = getRandom(0, MaxBlockCnt-1); // Get random starting block in list
    	for(int i=0; i<InvLength; i++) { // By Counting through list we do not miss a Block
    		if(Blocks[CrrntBlock] == CapchaBlock) capchaFound=true ;
    		CrrntBlock++;
    		if(CrrntBlock>=MaxBlockCnt) CrrntBlock=0; // End of list, start from beginning, mights result in double blocks is list is shorter. 
    		inv.setItem(i,new ItemStack(Blocks[CrrntBlock],1));
    		//System.out.println("Material: " + Blocks[RandBlock].toString() );
    	}
		//inv.addItem(new ItemStack(Material.SAND,1)); // plaats 1 SAND op de eerst beschikbare plaats/positie
    	//inv.setItem(5, new ItemStack(Material.GRAVEL,14)); // plaats een Stack van 14 DIAMOND op positie 5.
    	
    	if(Type.contains("Capcha")) {
    		if(!capchaFound) {
    			int RandPosition=getRandom(0, InvLength-1);
    			inv.setItem(RandPosition, new ItemStack(CapchaBlock,1)); // place Capcha-Block on Random position.
    		}
    	}
    	
    	return inv;
    }
    
    public static int getRandom(int lower, int upper) {
        Random random = new Random();
        return random.nextInt((upper - lower) + 1) + lower;
    }

}

