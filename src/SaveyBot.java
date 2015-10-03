/*
 * SaveyBot 2.0
 * A generic IRC Bot by Savestate
 * (not related to the original SaveyBot on espernet)
 */
 
import java.util.concurrent.TimeUnit;
import org.jibble.pircbot.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SaveyBot extends PircBot {
    
    public SaveyBot() {
        this.setName("SaveyBot");
    }
    
    public void onMessage(String channel, String sender, String login, String hostname, String message) {
        
        // URL Handling
        if (message.contains("http")) {
            //  ((http|https):\/\/\S+\.\S+) 
            // ^ URL Regex for this box
            String regex = "((http|https):\\/\\/\\S+\\.\\S+)";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                sendMessage(channel, "Regex Match: " + matcher.group(1));
            }
        }
        
        
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