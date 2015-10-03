/*
 * SaveyBot 2.0
 * A generic IRC Bot by Savestate
 * (not related to the original SaveyBot on espernet)
 */
 
import java.util.concurrent.TimeUnit;
import org.jibble.pircbot.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.*;
import java.io.*;
import java.lang.StringBuilder;

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
                String url = "";
                try {
                    url = matcher.group(1);
                    URL site = new URL(url);
                    BufferedReader in = new BufferedReader(new InputStreamReader(site.openStream()));
                    String inputLine;
                    StringBuilder websiteContents = new StringBuilder();
                    while ((inputLine = in.readLine()) != null)
                        websiteContents.append(inputLine + "\n");
                    in.close();
                    String html = websiteContents.toString();
                    String websiteTitle = betweenTags("<title>", "</title>", html);
                    sendMessage(channel, Colors.BOLD + "Title: " + Colors.NORMAL + websiteTitle);
                    if (message.contains("youtube.com")) {
                        try {
                            // YOUTUBE VIDEO
                            String viewCount = betweenTags("<div class=\"watch-view-count\">", "</div>", html);
                            String uploadedBy = betweenTags("<link itemprop=\"url\" href=\"http://www.youtube.com/user/", "\">", html);
                            String likes    = betweenTags("title=\"I like this\" data-position=\"bottomright\" data-force-position=\"true\" data-orientation=\"vertical\"><span class=\"yt-uix-button-content\">", "</span>", html);
                            String dislikes = betweenTags("title=\"I dislike this\" data-position=\"bottomright\" data-force-position=\"true\" data-orientation=\"vertical\"><span class=\"yt-uix-button-content\">", "</span>", html);
                            sendMessage(channel, Colors.BOLD + "Uploaded by: " + Colors.NORMAL + uploadedBy
                                               + Colors.RED + " | " + Colors.NORMAL
                                               + Colors.BOLD + "Views: " + Colors.NORMAL + viewCount
                                               + Colors.RED + " | " + Colors.NORMAL
                                               + Colors.BOLD + "Likes/Dislikes: " + Colors.NORMAL + likes + "/" + dislikes);
                        } catch (Exception e) {
                            System.out.println("YouTube Scrape Failed!");
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("URL Get Failed: " + url);
                }
            }
        }
        
        
        // admin tools
        if (sender.equals("Savestate")) {
            if (message.equalsIgnoreCase(".disconnect")) {
                sendMessage(channel, "rip me");
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    //Handle exception
                }
                disconnect();
                System.exit(0);
            }
        }
        
    }
    
    private String betweenTags(String tagOpen, String tagClose, String html) {
        tagOpen = tagOpen.toLowerCase();
        tagClose = tagClose.toLowerCase();
        String htmlSearch = html.toLowerCase();
        int begin = htmlSearch.indexOf(tagOpen) + tagOpen.length();
        int end = htmlSearch.indexOf(tagClose, begin);
        String text = html.substring(begin, end).replaceAll("\n", " ").replaceAll("\r", " ");
        return text;
    }
    
}