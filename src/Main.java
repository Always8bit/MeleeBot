/*
 * SaveyBot 2.0
 * A generic IRC Bot by Savestate
 * (not related to the original SaveyBot on espernet)
 *
 * Main Class
 */
 
public class Main {
    
    public static void main(String[] args) throws Exception {
        SaveyBot saveybot = new SaveyBot();
        saveybot.setVerbose(false);
        saveybot.setEncoding("UTF-8");
        try {
            saveybot.connect(saveybot.getParam("server"));
            for(int i = 1;!saveybot.getParam("channel"+i).isEmpty();i++)
                saveybot.joinChannel(saveybot.getParam("channel"+i));
            saveybot.identify(saveybot.getParam("ident"));
        } catch (Exception e) {
            displayHelp("Provided arguments were invalid.\nDid you add a '#' to the channel argument?");
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