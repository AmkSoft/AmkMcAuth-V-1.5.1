package com.mooo.amksoft.amkmcauth;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Properties;

//public class Language {
//
//}

public enum Language {

    ADMIN_HELP,
    ADMIN_SET_UP_INCORRECTLY,
    ALREADY_LOGGED_IN,
    ALREADY_REGISTERED,
    ANOTHER_PLAYER_WITH_NAME,
    COMMAND_NO_CONSOLE,
    CONFIGURATION_RELOADED,
    CONTACT_ADMIN,
    CONVERTED_USERDATA,
    COULD_NOT_CONVERT_USERDATA,
    COULD_NOT_LOG_IN,
    COULD_NOT_REGISTER,
    COULD_NOT_REGISTER_COMMAND,
    INCORRECT_EMAIL_CONF,
    COULD_NOT_START_METRICS,
    DISALLOWED_PASSWORD,
    DISALLOWED_EMLADDRESS,
    EMAIL_SET_AND_REGISTERED,
    EMAIL_SET_AND_CONFIRMSEND,
    ENABLED,
    ERROR,
    ERROR_OCCURRED,
    HAS_LOGGED_IN,
    HAS_REGISTERED,
    HELP_CHANGEPASSWORD,
    HELP_GETUUID,
    HELP_HELP,
    HELP_LOGIN,
    HELP_LOGOUT,
    HELP_NLPADD,
    HELP_NLPLIST,
    HELP_NLPREM,
    HELP_REGISTER,    
    HELP_REGEMAIL,    
    HELP_RELOAD,
    HELP_UNREGISTER,
    INCORRECT_PASSWORD,
    INVALID_SUBCOMMAND,
    INVALID_USERNAME,
    LOGGED_IN_SUCCESSFULLY,
    LOGGED_IN_VIA_NLPAUTH,
    LOGGED_IN_VIA_NLPLIST,
    LOGGED_IN_VIA_SESSION,
    LOGGED_OUT,
    METRICS_ENABLED,
    METRICS_OFF,
    NLP_LIST_PLAYERS,
    NLP_LIST_PLAYERS_NONE,
    NLP_SET_UPDATED,
    NO_PERMISSION,
    NOT_ENOUGH_ARGUMENTS,
    NOT_LOGGED_IN,
    OLD_PASSWORD_INCORRECT,
    PASSWORD_CHANGED,
    PASSWORD_COULD_NOT_BE_CHANGED,
    PASSWORD_COULD_NOT_BE_SET,
    PASSWORD_SET_AND_REGISTERED,
    PLAYER_ALREADY_REGISTERED,
    PLAYER_INVALID_EMAILADDRESS,
    PLAYER_REGISTERED_OTHERCASE,
    PLAYER_EXCEEDS_MAXREGS_IP,
    PLAYER_EXCEEDS_MAXREGS_EM,
    PLAYER_LOGGED_IN,
    PLAYER_LOGGED_OUT,
    PLAYER_NOT_LOGGED_IN,
    PLAYER_NOT_ONLINE,
    PLAYER_NOT_REGISTERED,
    PLAYER_REMOVED,
    PLAYER_REGISTER_COUNT,
    PLAYER_REGISTER_DISABLED,
    PLEASE_LOG_IN_WITH0,
    PLEASE_LOG_IN_WITH1,
    PLEASE_SETEMAIL_WITH,
    PLEASE_REGISTER_WITH0,    
    PLEASE_REGISTER_WITH1,    
    REGISTERED_SUCCESSFULLY,
    TOOK_TOO_LONG_TO_LOG_IN,
    TRY,
    USAGE_LOGIN0,  
    USAGE_LOGIN1,    
    USAGE_LOGIN2,    
    USAGE_REGISTER0,
    USAGE_REGISTER1,
    USAGE_REGISTER2,
    USAGE_CHANGEPAS0,
    USAGE_CHANGEPAS1,
    USAGE_CHANGEPAS2,
    USAGE_MENUPASSWRD,    
    USAGE_AUTOPASSWRD,    
    USED_INCORRECT_PASSWORD,
    WAS_LOGGED_IN_VIA_NLPAUTH,
    WAS_LOGGED_IN_VIA_NLPLIST,
    WAS_LOGGED_IN_VIA_SESSION,
    YOU_MUST_LOGIN,
    YOUR_PASSWORD_CHANGED,
    PASSWORD_RECOVER_MAIL,
    YOUR_PASSWORD_COULD_NOT_BE_CHANGED,
    EMAIL_ADDRESS,
    PASSWORD,
    CAPCHA_MESSAGE,
    WRONG_CAPCHA_CLICK,
    OLD_PASSWORD_MSSGE,
    NEW_PASSWORD_MSSGE,
    PASSWORD_MESSAGE,
    REGISTER_MESSAGE,
    CURRENT,
    NOTCONFIRMD;

	
    /**
     * Gets the message.
     *
     * @return Message
     */
    @Override
    public String toString() {
        return LanguageHelper.getString(name());
    }

    protected static class LanguageHelper {

        private static Properties p = new Properties();

        protected LanguageHelper(File f) throws IOException {
            final Reader in = new InputStreamReader(new FileInputStream(f), "UTF-8");
            LanguageHelper.p.load(in);
        }

        protected LanguageHelper(String s) throws IOException {
            final Reader in = new InputStreamReader(new FileInputStream(new File(s)), "UTF-8");
            LanguageHelper.p.load(in);
        }

        /**
         * Gets a property that is never null.
         *
         * @param node Node to get
         * @return String or "Language property "node" not defined."
         */
        private static String getString(String node) {
            String prop = LanguageHelper.p.getProperty(node);
            if (prop == null) {
            	prop = "Language property \"" + node + "\" not defined.";            	
            	System.out.println("AmkMcAuth: " + prop +", update your LanguageFile by adding this property!");
            	
            	// Use Defaults for missing Language properties......
            	if(node.equals("CAPCHA_MESSAGE")) 		prop = "Click %1$s Capcha.";
            	if(node.equals("WRONG_CAPCHA_CLICK")) 	prop = "Failed to click Capcha? Kicked because you are not Human!";
            	if(node.equals("PASSWORD_MESSAGE")) 	prop = "Login using your password block.";
            	if(node.equals("REGISTER_MESSAGE")) 	prop = "Register using a password block.";
            	if(node.equals("OLD_PASSWORD_MSSGE")) 	prop = "Click current password block.";
            	if(node.equals("NEW_PASSWORD_MSSGE")) 	prop = "Click block to use as password.";
            	if(node.equals("USAGE_MENUPASSWRD")) 	prop = "type %1$s as password if you use menu/inventory-block";
            	if(node.equals("USAGE_AUTOPASSWRD")) 	prop = "just type %1$s is you use menu/inventory-block as password";
            	if(node.equals("PASSWORD_RECOVER_MAIL")) prop = "Recover login new password send to your email address.";
            }
            return prop;
        }
    }

}
