/*
 * SaveyBot 2.0
 * A generic IRC Bot by Savestate
 * (not related to the original SaveyBot on espernet)
 */
 

import org.jibble.pircbot.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.*;
import java.net.*;
import java.io.*;
import java.lang.StringBuilder;
import java.security.MessageDigest;

public class SaveyBot extends PircBot {
    
    RussianRoulette rr;
    EightBall eb;
    ArrayList<FloodTimer> ftArray;
    
    public SaveyBot() {
        this.setName(getParam("botName"));
        rr = new RussianRoulette();
        eb = new EightBall();
        ftArray = new ArrayList<>();
    }
    
    public void onJoin(String channel, String sender, String login, String hostname) {
        ftArray.add(new FloodTimer(channel, Integer.parseInt(getParam("floodTimerSeconds"))));
    }
    
    public void onMessage(String channel, String sender, String login, String hostname, String message) {

        String mCommand = "";
        String mArgs    = "";
        String inv = getParam("invokingSymbol");
    
        // URL Handling
        if (message.contains("http")) {
            System.out.println("URL Parse");
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
                    URLConnection urlc = site.openConnection();
                    urlc.addRequestProperty("User-Agent", getParam("useragent"));
                    InputStream stream = urlc.getInputStream();
                    BufferedReader in = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
                    String inputLine;
                    StringBuilder websiteContents = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        websiteContents.append(inputLine + "\n");
                        // if websiteContents > 1Mb
                        // (a char is 16bits)
                        if (websiteContents.length() > 500000) {
                            System.out.println("URL Fetch exceeded 1Mb!");
                            break;
                        }
                    }
                    in.close();
                    String html = websiteContents.toString();
                    try {
                        if (message.toLowerCase().contains("youtube.com")) {
                            // If no <title> is found, it will throw an exception!
                            String websiteTitle = betweenTags("<title>", "</title>", html);
                            websiteTitle = websiteTitle.trim();
                            sendMessage(channel, Colors.BOLD + "Title: " + Colors.NORMAL + websiteTitle);
                        }
                    } catch (Exception e) {
                        // No <title> was found...
                        
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("URL Get Failed: " + url);
                }
            }
        }
        
        // split the command from the arguments
        try {
            String[] split = message.trim().split("\\s+", 2);
            mCommand = split[0].toLowerCase();
            try {
                mArgs    = split[1];
            } catch (Exception e) {
                // no args, only a command
            }
        } catch (Exception e) {
            
        }
        
        if (!mCommand.startsWith(inv))
            return;
        
        mCommand = mCommand.substring(1);

        // FLOOD PROTECTED COMMANDS 
        FloodTimer ft = null;
        for (int i=0; i<ftArray.size(); i++) {
            if (ftArray.get(i).getChannel().equals(channel))
                ft = ftArray.get(i);
        }
        
        if (null == ft) {
            System.out.println("FLOOD TIMER ERROR: CHANNEL NOT INIT!");
        } else {
            // Challonge Bracket Parsing
            if (mCommand.equals("bracket")) {
                System.out.println("Challonge Bracket");
                String api  = getParam("challongeApi");
                String user = getParam("challongeUser");
                String bracket = challongeUrlParse(mArgs);
                System.out.println("Loading Bracket: " + bracket);
                    try {
                        // get the participants
                        String url = "https://" + user
                                    + "@api.challonge.com/v1/tournaments/" + bracket + "/participants.xml?api_key="
                                    + api;
                        URL site = new URL(url);
                        BufferedReader in = new BufferedReader(new InputStreamReader(site.openStream()));
                        String inputLine;
                        StringBuilder xml = new StringBuilder();
                        while ((inputLine = in.readLine()) != null) {
                            xml.append(inputLine + "\n");
                        }
                        in.close();
                        String participants = xml.toString();
                        // now get the current matchups
                        url = "https://" + user
                                    + "@api.challonge.com/v1/tournaments/" + bracket + "/matches.xml?api_key="
                                    + api;
                        site = new URL(url);
                        in = new BufferedReader(new InputStreamReader(site.openStream()));
                        xml = new StringBuilder();
                        while ((inputLine = in.readLine()) != null) {
                            xml.append(inputLine + "\n");
                        }
                        in.close();
                        String matchups = xml.toString();
                        ArrayList<ArrayList<ChallongeMatch>> battles = matchupParser(participants, matchups);
                        ArrayList<ChallongeMatch> completed = battles.get(0);
                        ArrayList<ChallongeMatch> upcoming  = battles.get(1);
                        // grab the max number from the config...
                        int numToDisplay = Integer.parseInt(getParam("challongeMaxReturn"));
                        String completedMessage = "";
                        for (int i = completed.size()-1; i >= completed.size()-numToDisplay; i--) {
                            if (i < 0) 
                                break;
                            if (completed.size() == 0)
                                break;
                            completedMessage = completedMessage +  " " + completed.get(i).matchText;
                        }
                        String upcomingMessage = "";
                        for (int i = upcoming.size()-1; i >= upcoming.size()-numToDisplay; i--) {
                            if (i < 0) 
                                break;
                            if (upcoming.size() == 0)
                                break;
                            upcomingMessage = upcomingMessage +  " " + upcoming.get(i).matchText;
                        }
                        if (!(mArgs.contains("www") || mArgs.contains("http")))
                                    sendMessage(channel, "URL: http://challonge.com/" + bracket);
                        if (!upcomingMessage.isEmpty()) {
                            if (ft.invoke()) {
                                if (!completedMessage.isEmpty())
                                    sendMessage(channel, "Completed Matches:" + completedMessage);
                                sendMessage(channel, "Upcoming Matches:" + upcomingMessage);
                                ft.executedSuccessfully();
                            }
                        } else {
                            sendMessage(channel, Colors.BOLD + bracket + "'s Results" + Colors.NORMAL + ": " + challongeStandingsParse(bracket));
                        }
                    } catch (Exception e) {
                        System.out.println("Error parsing bracket!");
                    }
            }
        }
        
        // Brawl Kick
        if (mCommand.equals("brawl")) {
            kick(channel, sender, "What the fuck dude?");
        }
        
        // Twitch Stream Search
        if (mCommand.equals("twitch")) {
            String[] results = twitchAPISearch(mArgs);
            if (results.length == 2) {
                sendMessage(channel, results[0]);
                sendMessage(channel, results[1]);
            } else {
                sendMessage(channel, "Search \"" + mArgs + "\" provided no stream results");
            }
        }
        
        // Google Searching 
        if (mCommand.equals("g")) {
            System.out.println("Google Search");
            String url = mArgs;
            url = getParam("googlePrefix") + url;
            url = url.replaceAll(" ", "%20");
            sendMessage(channel, "Google Search: " + url);
        }
        
        // YouTube Searching 
        if (mCommand.equals("y")) {
            System.out.println("YouTube Search");
            String url = mArgs;
            url = getParam("youtubePrefix") + url;
            url = url.replaceAll("\\+", "%2B");
            url = url.replaceAll(" ", "+");
            sendMessage(channel, "YouTube Search: " + url);
        }
        
        // Russian Roulette
        if (mCommand.equals("rr")) {
            if (rr.fire()) {
                sendMessage(channel, Colors.RED + "*" + Colors.NORMAL + Colors.BOLD + "BANG" + Colors.NORMAL + Colors.RED + "*");
                sendMessage(channel, sender + " just blew his brains out. (not like they were important anyways)");
                sendMessage(channel, "A new bullet was loaded into the chamber.");
            } else {
                sendMessage(channel, Colors.RED + "*" + Colors.NORMAL + Colors.BOLD + "CLICK" + Colors.NORMAL + Colors.RED + "*");
            }
        }
        
        // 8-Ball
        if (mCommand.equals("8") || mCommand.equals("8ball") || mCommand.equals("eight") || mCommand.equals("is") || mCommand.equals("conch")){
            if (mArgs.isEmpty())
                return;
            if (mCommand.equals("is"))
                mArgs = "is " + mArgs;
            sendMessage(channel, sender + ": " + eb.response(mArgs));
        }

        if (mCommand.equals("why"))
            sendMessage(channel, sender + ": " + eb.getMessage(1));

        // aka commands from param file
        String aka = getParam("aka-" + mCommand);
        if (!aka.isEmpty()) {
            if (aka.contains("###*")) {
                if (mArgs.isEmpty()) {
                    sendMessage(sender, inv + mCommand + " needs more arguments you dumpass");
                    return;
                }
                aka = aka.replaceAll("\\#\\#\\#\\*", mArgs);
            }
            sendMessage(channel, aka);
        }
        
        // admin tools
        if (sender.equals(getParam("root"))) {
            
            if (mCommand.equalsIgnoreCase("disconnect")) {
                sendMessage(channel, "rip me");
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (Exception e) {
                    //Handle exception
                }
                disconnect();
                System.exit(0);
            }
            
            if (mCommand.equalsIgnoreCase("leave")) {
                try {
                    sendMessage(channel, "later losers");
                    TimeUnit.SECONDS.sleep(1);
                    partChannel(channel);
                } catch (Exception e) {
                    //Handle exception
                }
            }
            
            if (mCommand.equalsIgnoreCase("join")) {
                try {
                    TimeUnit.SECONDS.sleep(1);
                    joinChannel(mArgs);
                } catch (Exception e) {
                    sendMessage(channel, "wtf is that even a channel");
                }
            }
            
            if (mCommand.equalsIgnoreCase("msg")) {
                try {
                    TimeUnit.SECONDS.sleep(1);
                    String[] stuff = mArgs.split("\\s+", 2);
                    sendMessage(stuff[0], stuff[1]);
                } catch (Exception e) {
                    sendMessage(channel, "you probably typed that wrong");
                }
            }
            
            if (mCommand.equalsIgnoreCase("param-set")) {
                try {
                    String[] stuff = mArgs.split("\\s+", 2);
                    setParam(stuff[0], stuff[1]);
                } catch (Exception e) {
                    sendMessage(channel, "aka set failed (wow)");
                }
            }
            
            if (mCommand.equalsIgnoreCase("param-remove")) {
                try {
                    delParam(mArgs);
                } catch (Exception e) {
                    sendMessage(channel, "remove failed");
                }
            }
            
            if (mCommand.equalsIgnoreCase("param-list")) {
                try {
                    BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream("params.config"), "UTF-8"));
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        sendMessage(sender, inputLine);
                    }
                    in.close();
                } catch (Exception e) {
                    sendMessage(channel, "remove failed");
                }
            }
            
        }

        
    }
    
    private String challongeStandingsParse(String bracket) throws Exception {
        String url = "http://challonge.com/" + bracket + "/standings";
        
        String rankPrefix  = "<span>";
        String rankPostfix = "</span>";
        String rankSectionStart = "<td class='left display_name'>";
        
        URL site = new URL(url);
        URLConnection urlc = site.openConnection();
        urlc.addRequestProperty("User-Agent", getParam("useragent"));
        InputStream stream = urlc.getInputStream();
        BufferedReader in = new BufferedReader(new InputStreamReader(stream));
        String inputLine;
        StringBuilder html = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            html.append(inputLine + "\n");
        }
        in.close();
        String htmlText = html.toString();
        
        String[] names = new String[3];
        
        int previousIndex = 0;
        for(int i=0; i<3; i++) {
            String name = "none...";
            try {
                int index = htmlText.indexOf(rankSectionStart, previousIndex);
                name = parseTagInMatch(index, rankPrefix, rankPostfix, htmlText);
                previousIndex = index + 1;
            } catch (Exception e) {
                
            }
            names[i] = name;
        }
       
        String returnNames = "[" + Colors.BOLD + Colors.GREEN + "1st: " + Colors.NORMAL + names[0] + "]" + " | " + Colors.NORMAL
                           + "[" + Colors.BOLD + Colors.BLUE  + "2nd: " + Colors.NORMAL + names[1] + "]" + " | " + Colors.NORMAL
                           + "[" + Colors.BOLD + Colors.RED   + "3rd: " + Colors.NORMAL + names[2] + "]";
        return returnNames;
    }
    
    private String betweenTags(String tagOpen, String tagClose, String html) throws Exception{
        tagOpen = tagOpen.toLowerCase();
        tagClose = tagClose.toLowerCase();
        String htmlSearch = html.toLowerCase();
        int begin = htmlSearch.indexOf(tagOpen) + tagOpen.length();
        int end = htmlSearch.indexOf(tagClose, begin);
        if (((begin-tagOpen.length()) == -1) || (end == -1))
            throw new Exception();
        String text = html.substring(begin, end).replaceAll("\n", " ").replaceAll("\r", " ");
        return text;
    }
    
    public String getParam(String p) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream("params.config"), "UTF-8"));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.startsWith(p + ":")) {
                    in.close();
                    return inputLine.substring(p.length() + 1);
                }
            }
        } catch (Exception e) {
        }
        return "";
    }

    public void setParam(String p, String val) throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream("params.config"), "UTF-8"));
        StringBuilder  sb = new StringBuilder();
        String inputLine;
        boolean exists = false;
        while ((inputLine = in.readLine()) != null) {
            if (inputLine.startsWith(p + ":")) {
                inputLine = p + ":" + val;
                exists = true;
            }
            sb.append(inputLine).append("\n");
        }
        if (!exists) {
            sb.append(p + ":" + val).append("\n");
        }
        PrintWriter out = new PrintWriter("params.config");
        out.println(sb.toString());
        out.close();
    }
    
    public void delParam(String p) throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream("params.config"), "UTF-8"));
        StringBuilder  sb = new StringBuilder();
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            if (!inputLine.startsWith(p + ":"))
                sb.append(inputLine).append("\n");
        }
        PrintWriter out = new PrintWriter("params.config");
        out.println(sb.toString());
        out.close();
    }
    
    private String challongeUrlParse(String s) {
        // Example: .bracket http://challonge.com/scj-melee6
        // Example: .bracket http://scj.challonge.com/pm3
        // Example: .bracket specificName
        // Example: .bracket scj.challonge.com/pm3
        // Example: .bracket challonge.com/scj-pm3
        // http://stackoverflow.com/questions/767759/occurrences-of-substring-in-a-string
        String bracketIdent = "";
        s = s.trim();
        if (s.toLowerCase().startsWith("http")) {
            // remove http:// or https://
            int protocol = s.indexOf("://") + 3;
            s = s.substring(protocol);
        }
        // depending on how many .'s there are, we'll know
        // if it's a direct url, or userbased url
        String findStr = ".";
        int lastIndex = 0;
        int count = 0;
        while(lastIndex != -1){
            lastIndex = s.indexOf(findStr,lastIndex);
            if(lastIndex != -1){
                count++;
                lastIndex += findStr.length();
            }
        }
        if (count == 1) {
            // Direct URL
            String prefix = ".com/";
            int begin = s.indexOf(prefix) + prefix.length();
            int end   = s.indexOf("/", begin);
            if (end == -1) {
                bracketIdent = s.substring(begin);
            } else {
                bracketIdent = s.substring(begin, end);
            }
        } else if (count == 2) {
            // User based URL
            int userIndex = s.indexOf(".");
            String user = s.substring(0,userIndex);
            String prefix = ".com/";
            int begin = s.indexOf(prefix) + prefix.length();
            int end   = s.indexOf("/", begin);
            if (end == -1) {
                bracketIdent = user + "-" + s.substring(begin);
            } else {
                bracketIdent = user + "-" + s.substring(begin, end);
            }
        } else if (count == 0) {
            // Actual Bracket ID
            bracketIdent = s;
        }
        return bracketIdent;
    }
    
    // Completed is 0, upcoming is 1
    private ArrayList<ArrayList<ChallongeMatch>> matchupParser(String participants, String matchups) {
        ArrayList<ChallongeUser> users = new ArrayList<>();
        int searchIndexStart = 0;
        String idPrefix  = "<id type=\"integer\">";
        String idPostfix = "</id>";
        String namePrefix  = "<display-name>";
        String namePostfix = "</display-name>";
        searchIndexStart = participants.indexOf(idPrefix, searchIndexStart);
        while (searchIndexStart != -1) {
            searchIndexStart += idPrefix.length();
            int idEnd = participants.indexOf(idPostfix, searchIndexStart);
            String id = participants.substring(searchIndexStart, idEnd);

            searchIndexStart = participants.indexOf(namePrefix, searchIndexStart);
            searchIndexStart += namePrefix.length();
            int nameEnd = participants.indexOf(namePostfix, searchIndexStart);
            String displayName = participants.substring(searchIndexStart, nameEnd);
            
            users.add(new ChallongeUser(displayName, id));
            
            searchIndexStart = participants.indexOf(idPrefix, searchIndexStart);
        }
        // Users list now populated. 
        // It will be referenced as we go through the matchups since.
        // the matchup XML is based on IDs and not names.
        
        String player1Prefix  = "<player1-id type=\"integer\">";
        String player2Prefix  = "<player2-id type=\"integer\">";
        String player1Postfix = "</player1-id>";
        String player2Postfix = "</player2-id>";
        String roundPrefix    = "<round type=\"integer\">";
        String roundPostfix   = "</round>";
        // State: pending, open, complete...
        // default to pending unless complete!
        String statePrefix    = "<state>";
        String statePostfix   = "</state>";
        // only if match is complete...
        String winnerPrefix   = "<winner-id type=\"integer\">";
        String winnerPostfix  = "</winner-id>";
        String loserPrefix    = "<loser-id type=\"integer\">";
        String loserPostfix   = "</loser-id>";
        String scoresPrefix   = "<scores-csv>";
        String scoresPostfix  = "</scores-csv>";
        
        // each is contained in this tag...
        String matchPrefix  = "<match>";
        String matchPostfix = "</match>";
        
        ArrayList<ChallongeMatch> completed = new ArrayList<>();
        ArrayList<ChallongeMatch> upcoming  = new ArrayList<>();
        
        int nextMatch = matchups.indexOf(matchPrefix);
        while(nextMatch != -1) {
            nextMatch += matchPrefix.length();
            String p1 = parseTagInMatch(nextMatch, player1Prefix, player1Postfix, matchups);
            String p2 = parseTagInMatch(nextMatch, player2Prefix, player2Postfix, matchups);
            String state = parseTagInMatch(nextMatch, statePrefix, statePostfix, matchups);
            String round = parseTagInMatch(nextMatch, roundPrefix, roundPostfix, matchups);
            int roundInt = Integer.parseInt(round);
            // round formatting (negative is losers)
            if (roundInt < 0) {
                round = "L" + roundInt*-1;
            } else {
                round = "W" + roundInt;
            }
            roundInt = Math.abs(roundInt);
            boolean filledBracket = true;
            if (p1.isEmpty() || p2.isEmpty())
                filledBracket = false;
            // if we don't have a filled bracket, just skip!
            if (filledBracket) {
                // if our match has been completed...
                if (state.equals("complete")) {
                    String winner = parseTagInMatch(nextMatch, winnerPrefix, winnerPostfix, matchups);
                    String loser  = parseTagInMatch(nextMatch, loserPrefix, loserPostfix, matchups);
                    String scores = parseTagInMatch(nextMatch, scoresPrefix, scoresPostfix, matchups);
                    // replace userID with display name
                    for (int i = 0; i<users.size(); i++) {
                        if (winner.equals(users.get(i).userID))
                            winner = users.get(i).username;
                        if (loser.equals(users.get(i).userID))
                            loser = users.get(i).username;
                    }
                    ChallongeMatch cm = new ChallongeMatch(Colors.BOLD + "[" + round + "] " + Colors.NORMAL +
                                                            Colors.BLUE + winner + Colors.NORMAL +
                                                            " defeated " + Colors.RED + loser +
                                                            Colors.NORMAL + " (" + scores + ")", roundInt);
                    completed.add(cm);
                } else {
                    // replace userID with display name
                    for (int i = 0; i<users.size(); i++) {
                        if (p1.equals(users.get(i).userID))
                            p1 = users.get(i).username;
                        if (p2.equals(users.get(i).userID))
                            p2 = users.get(i).username;
                    }
                    ChallongeMatch cm = new ChallongeMatch(Colors.BOLD + "[" + round + "] " + Colors.NORMAL + p1 + " vs. " + p2, roundInt);
                    upcoming.add(cm);
                }
            }
            nextMatch = matchups.indexOf(matchPrefix, nextMatch);
        }
        ArrayList<ArrayList<ChallongeMatch>> matches = new ArrayList<ArrayList<ChallongeMatch>>();
       
        // Sort based on the round number (which was absolute valued earlier...)
        
        Collections.sort(completed,new Comparator<ChallongeMatch>() {
                @Override
                public int compare(ChallongeMatch m1, ChallongeMatch m2) {
                    if (m1.round < m2.round)
                        return -1;
                    if (m1.round > m2.round)
                        return 1;
                    return 0;
                }
            });
        
        Collections.sort(upcoming,new Comparator<ChallongeMatch>() {
                @Override
                public int compare(ChallongeMatch m1, ChallongeMatch m2) {
                    if (m1.round < m2.round)
                        return -1;
                    if (m1.round > m2.round)
                        return 1;
                    return 0;
                }
            });
        
        matches.add(completed);
        matches.add(upcoming);
        
        return matches;
    }
    
    private String parseTagInMatch(int startIndex, String prefix, String postfix, String xml) {
        int begin = xml.indexOf(prefix, startIndex) + prefix.length();
        int end   = xml.indexOf(postfix, startIndex);
        if ((begin != -1) && (end != -1))
            return xml.substring(begin, end);
        return "";
    }
    
    private String[] twitchAPISearch(String s) {
        try {
            s = s.replaceAll("\\s+","%20");
            URL site = new URL("https://api.twitch.tv/kraken/search/streams?q=" + s);
            URLConnection urlc = site.openConnection();
            urlc.addRequestProperty("User-Agent", getParam("useragent"));
            InputStream stream = urlc.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
            String inputLine;
            StringBuilder websiteContents = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                websiteContents.append(inputLine + "\n");
                // if websiteContents > 1Mb
                // (a char is 16bits)
                if (websiteContents.length() > 500000) {
                    System.out.println("URL Fetch exceeded 1Mb!");
                    return new String[1];
                }
            }
            in.close();
            
            String html = websiteContents.toString();

            String viewersPrefix =  "\"viewers\":";
            String viewersPostfix = ",";
            String gamePrefix = "\"game\":\"";
            String gamePostfix = "\",";
            String statusPrefix = "\"status\":\"";
            String statusPostfix = "\",";
            String userPrefix = "\"name\":\"";
            String userPostfix = "\",";

            String viewers = betweenTags(viewersPrefix, viewersPostfix, html);
            String game    = betweenTags(gamePrefix,    gamePostfix,    html);
            String status  = betweenTags(statusPrefix,  statusPostfix,  html);
            String user    = betweenTags(userPrefix,    userPostfix,    html);
            
            String[] responses = new String[2];
            
            responses[0] = Colors.BOLD + "Top Result: " + Colors.NORMAL + user + " playing " + 
                         game + Colors.RED + " | " + Colors.NORMAL + viewers + " Viewers" + 
                         Colors.RED + " | " + Colors.NORMAL + status;
            responses[1] = "http://twitch.tv/" + user;
            
            return responses;
        } catch (Exception e) {
            // do stuff
        }
        return new String[1];
    }
    
    private class ChallongeUser {
        
        public String username, userID;
        
        public ChallongeUser(String username, String userID) {
            this.username = username;
            this.userID = userID;
        }
        
    }
    
    private class ChallongeMatch {
        
        public String matchText;
        public int round;
        
        public ChallongeMatch(String matchText, int round) {
            this.round = round;
            this.matchText = matchText;
        }
    }

    private class RussianRoulette {
        
        private int chamber;
        
        public RussianRoulette() {
            loadNewBullet();
        }
        
        private void loadNewBullet() {
            chamber = ((int)(Math.random()*1024))%6;
        }
        
        public boolean fire() {
            if (--chamber < 0) {
                loadNewBullet();
                return true;
            }
            return false;
        }
        
    }

    private class EightBall {
        
        private ArrayList<ArrayList<String>> responses;

        final int RESPONSE_NO    = 0;
        final int RESPONSE_MAYBE = 1;
        final int RESPONSE_YES   = 2;
        
        public EightBall() {
            ArrayList<String> no    = new ArrayList<>();
            ArrayList<String> maybe = new ArrayList<>();
            ArrayList<String> yes   = new ArrayList<>();
            
            responses = new ArrayList<>(3);
            
            responses.add(RESPONSE_NO, no);
            responses.add(RESPONSE_MAYBE, maybe);
            responses.add(RESPONSE_YES, yes);

            // NO Responses
            no.add("I doubt it very much.");
            no.add("No chance.");
            no.add("The outlook is poor.");
            no.add("Unlikely.");
            no.add("About as likely as pigs flying.");
            no.add("You're kidding, right?");
            no.add("NO!");
            no.add("No.");
            no.add("NO.");
            no.add("The answer is a resounding no.");
            
            // MAYBE Responses
            maybe.add("Maybe...");
            maybe.add("No clue.");
            maybe.add("I don't know.");
            maybe.add("In your dreams.");
            maybe.add("The outlook is hazy, please ask again later.");
            maybe.add("What are you asking me for?");
            maybe.add("Come again?");
            maybe.add("You know the answer better than I.");
            maybe.add("The answer is def-- oooh! shiny thing!");
            
            // YES Responses
            yes.add("Yes!");
            yes.add("Of course.");
            yes.add("Naturally.");
            yes.add("Obviously.");
            yes.add("It shall be.");
            yes.add("The outlook is good.");
            yes.add("One would be wise to think so.");
            yes.add("The answer is certainly yes.");
           
        }
        
        public String response(String question) {
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                md.update(question.getBytes());
                byte[] hash = md.digest();
                int n = hash[hash.length-1];
                n = Math.abs(n);
                n = n%3;
                return getMessage(n);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "Eightball is having trouble answering your question... try wording it differently.";
        }
        
        public String getMessage(int n) {
            // 0 = NO, 1 = MAYBE, 2 = YES
            int range = responses.get(n).size();
            int randMessageIndex = ((int)(Math.random()*10000))%range;
            return responses.get(n).get(randMessageIndex);
        }
    }

    private class FloodTimer {

        private long pendingTime;
        private long currentInvocation;
        private long previousInvocation;
        private String channel;
        private int floodSeconds;
        
        public FloodTimer(String channel, int floodSeconds) {
            currentInvocation = 0;
            previousInvocation = 0;
            pendingTime = 0;
            this.floodSeconds = floodSeconds;
            this.channel = channel;
        }
        
        public boolean invoke() {
            pendingTime = System.currentTimeMillis()/1000L;
            if (pendingTime - currentInvocation > floodSeconds || pendingTime - previousInvocation > floodSeconds) {
                return true;
            }
            return false;
        }
        
        public void executedSuccessfully() {
            previousInvocation = currentInvocation;
            currentInvocation = pendingTime;
        }
        
        public String getChannel() {
            return channel;
        }
        
    }
    
}
