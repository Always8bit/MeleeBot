/*
 * SaveyBot 2.0
 * A generic IRC Bot by Savestate
 * (not related to the original SaveyBot on espernet)
 */
 
import org.jibble.pircbot.*;
 
public class SaveyBot extends PircBot {
    
    public SaveyBot() {
        this.setName("SaveyBot");
    }
    
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            displayHelp("SaveyBot is a generic ircbot created by Savestate.");
            System.exit(-1);
        }
        SaveyBot saveybot = new SaveyBot();
        saveybot.setVerbose(true);
        try {
            saveybot.connect(args[0]);
            saveybot.joinChannel(args[1]);
        } catch (Exception e) {
            displayHelp("Provide arguments were invalid.\nDid you add a '#' to the channel argument?");
            System.exit(-2);
        }
    }
    
    public static void displayHelp(String s) {
        System.out.println();
        System.out.println(s);
        System.out.println();
        System.out.println("Usage: java SaveyBot [server] [channel]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("     [server]        The irc server address to connect to.");
        System.out.println("     [channel]       The irc channel to connect to. (including the #)");
        System.out.println();
    }
    
}