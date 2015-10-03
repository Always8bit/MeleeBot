/*
 * SaveyBot 2.0
 * A generic IRC Bot by Savestate
 * (not related to the original SaveyBot on espernet)
 */
 
import java.util.concurrent.TimeUnit;
import org.jibble.pircbot.*;
 
public class SaveyBot extends PircBot {
    
    public SaveyBot() {
        this.setName("SaveyBot");
    }
    
    public void onMessage(String channel, String sender, String login, String hostname, String message) {
        
        
        // admin tools
        if (sender.equals("Savestate")) {
            if (message.equalsIgnoreCase(".disconnect")) {
                sendMessage(channel, "rip me");
                try {
                    TimeUnit.SECONDS.sleep(2);
                } catch (InterruptedException e) {
                    //Handle exception
                }
                disconnect();
                System.exit(0);
            }
        }
        
    }
    
}