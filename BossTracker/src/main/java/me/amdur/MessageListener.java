package me.amdur;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.lang.String;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.UnicodeEmoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.TimeFormat;
import net.dv8tion.jda.api.entities.TextChannel;

public class MessageListener extends ListenerAdapter {    

    MessageReceivedEvent event;         // Message event
    User botUser;                       // Bot User 
    String channel;                     // Discord channel name
    String message;                     // Received message
    String author;                      // Message author
    boolean authorIsBot;                // Is message author a bot
    List<User> mentionedUsers;          // Mentioned users
    String[] messageParts;              // Received message split by whitespaces
    String bossName;                    // Received boss name      
    Guild server;                       // Server ID
    TextChannel raidInfoChannel;        // Raid Info Channel ID
             
    MySqlDatabaseConnector dbcon;       // SQLite connection

    long currentTime;                   // Current time in milliseconds
    long bossTodDate            = 0;    // Boss death timestamps    
    long bossWindowOpenDate     = 0;    // Boss respawn timestamps    
    long bossWindowCloseDate    = 0;    // Boss window close timestamps
    long bossRespawnTimer       = 0;    // Boss respawn timer
    long bossManualTod          = 0;    // Unix timestamp for manual TOD

    String[] availableBosses = {"Baium", "Barakiel", "Cabrio", "Golkonda", "Hallate", "Kernon", "Core", "Orfen", "QueenAnt", "Lilith", "Anakim", "Shadith", "Mos", "Hekaton", "Tayr"};

    List<Role> bossRoleList = new ArrayList<>();
    String bossRole = null;

    public void onMessageReceived (MessageReceivedEvent receivedEvent) {
        event = receivedEvent;
        currentTime = System.currentTimeMillis();
        setupEventVariables();
        
        if (!messageIsValid())
            return;

        dbcon = new MySqlDatabaseConnector();
        
        if (containsBossName() && (message.contains("dead") || message.contains("down"))) {
            updateTod();
            scheduleWindowOpenMessage();
            sendTodConfirmation();
        }

        else if (containsBossName() && message.contains("info")) 
            sendBossReport();

        else if (isBotMentioned() && message.contains("help")) 
            sendHelp();
    }

    public boolean messageIsValid() {
        return     event.isFromGuild() 
                && channel.equals("raid-info")                
                && !authorIsBot;
    }

    public boolean containsBossName() {
        return     !bossName.isEmpty()
                && bossIsListed();
    }

    public void setupEventVariables() {
        server          = event.getGuild();                                 // Server ID
        raidInfoChannel = server.getTextChannelById("997460044201328740");  // Raid Info Channel ID
        botUser         = event.getJDA().getSelfUser();                     // Bot User ID
        channel         = event.getChannel().getName();                     // Get channel
        message         = event.getMessage().getContentStripped();          // Get message
        author          = event.getAuthor().getAsMention();                 // Get author of message 
        authorIsBot     = event.getAuthor().isBot();                        // Checks if the message was sent from a bot    
        mentionedUsers  = event.getMessage().getMentions().getUsers();      // Lists the mentioned users

        messageParts    = message.split("\s");                      // Split message between spaces
        bossName        = messageParts[0].substring(1);         // Remove the first character        
    }

    public void updateTod() {
        String debugInfo;
        long tod;

        if (hasManualTod()) {
            tod = getManualTod();
            if (tod == 0) {
                return;
            }
            bossRespawnTimer = ((currentTime - bossManualTod) + dbcon.getRespawnTimer(bossName));
            debugInfo = "Manual TOD set (" + tod + ").";  
        } else {
            tod = currentTime;
            bossRespawnTimer = dbcon.getRespawnTimer(bossName);
            debugInfo = "Current TOD set (" + tod + ").";        
        }

        dbcon.setTod(bossName, tod);  
        System.out.println(debugInfo);
    }

    public void sendBossReport() {
        getBossTimestamps();

        if (bossTodDate == 0) {
            event.getChannel().sendMessage(":no_entry: Sorry " +  author + ", there is no recorded TOD for " + bossName + ".").queue();
            return;
        }

        reportTod();
        reportRespawnWindow();
    }

    public void sendHelp() {
        event.getChannel().sendMessage(":information_source: Use **@Bossname dead** to report a recent boss death.").queue();
        event.getChannel().sendMessage(":information_source: Use **@Bossname dead [UNIX Timesamp in milliseconds]** to manually set the time of death  (e.g. 1658695674957).").queue();
        event.getChannel().sendMessage(":information_source: Use **@Bossname info** to get the latest information.").queue();
        event.getChannel().sendMessage(":information_source: The following bosses are trackable: " + Arrays.toString(availableBosses)).queue();
        event.getChannel().sendMessage(":warning: Boss names must start with a capital letter for the commands to work (for now)!").queue();
    }

    public void sendTodConfirmation() {
        String todConfirmationMessage   = ":white_check_mark: " + bossName + " TOD registered, thank you " + author + "!";
        UnicodeEmoji checkmarkEmoji     = Emoji.fromUnicode("U+2705");
        
        event.getChannel().sendMessage(todConfirmationMessage).queue();
        event.getMessage().addReaction(checkmarkEmoji).queue();
    }

    public void scheduleWindowOpenMessage() {
        // Get boss role name
        bossRoleList    = event.getGuild().getRolesByName(bossName, true);
        bossRole        = Arrays.toString(bossRoleList.toArray()).replaceAll("[^0-9]","");

        String windowOpenMessage = ":camping: Spawn window for <@&" + bossRole + "> has opened!";

        // Schedule message for respawn window
        ScheduledExecutorService scheduledTodMessage = Executors.newScheduledThreadPool(1);
        Runnable sendWindowOpenMessage = () -> raidInfoChannel.sendMessage(windowOpenMessage).queue();
        scheduledTodMessage.schedule(sendWindowOpenMessage, bossRespawnTimer, TimeUnit.MILLISECONDS);
        scheduledTodMessage.shutdown();
    } 

    public void getBossTimestamps() {        
        bossTodDate         = dbcon.getTod(bossName);           // Current Date/Time
        bossWindowOpenDate  = dbcon.getWindowOpen(bossName);    // Current Date/Time + respawn time
        bossWindowCloseDate = dbcon.getWindowClose(bossName);   // Respawn time + random window

        /*System.out.println("Boss name: " + bossName);
        System.out.println("TOD: " + bossTodDate);
        System.out.println("Window opens: " + bossWindowOpenDate);
        System.out.println("Window closes: " + bossWindowCloseDate);*/
    }

    public void reportTod() {
        event.getChannel().sendMessage(":skull_crossbones: Last recorded TOD for " + bossName + ": " + TimeFormat.DATE_TIME_LONG.format(bossTodDate) + " (" + TimeFormat.RELATIVE.format(bossTodDate) + ")").queue();
    }

    public void reportRespawnWindow() {
        String respawnWindowMessage;

        if (windowIsOpen()) {
            respawnWindowMessage = ":green_circle: " + bossName + " spawn window opened " + TimeFormat.RELATIVE.format(bossWindowOpenDate) + " and closes in " + TimeFormat.RELATIVE.format(bossWindowCloseDate) + ". :bangbang:";
        }
        else if (windowIsClosed()) {
            respawnWindowMessage = ":question: Last " + bossName + " spawn window is closed, no TOD reported!";
            dbcon.setTod(bossName, 0);
        }
        // Window has not yet opened
        else {
            respawnWindowMessage = ":window: Next spawn window for " + bossName + " is open between " + TimeFormat.DATE_TIME_LONG.format(bossWindowOpenDate) + " (" + TimeFormat.RELATIVE.format(bossWindowOpenDate) + ") and " + TimeFormat.DATE_TIME_LONG.format(bossWindowCloseDate) + ".";
        }

        event.getChannel().sendMessage(respawnWindowMessage).queue();;
    }

    /*
    * ***** Helpers *****
    */

    public static boolean isNumeric(String str) {
        return str.matches("-?\\d+(\\.\\d+)?"); // Match a number with optional '-' and decimal.
    }

    public boolean hasManualTod() {
        return messageParts.length > 2 && isNumeric(messageParts[2]);
    }

    public long getManualTod() {
        return Long.valueOf(messageParts[2]).longValue(); // Converts the unix timestamp to a long
    }

    public boolean bossIsListed() {
        return Arrays.stream(availableBosses).anyMatch(bossName::equals); // Check if mentioned boss is listed 
    }

    public boolean windowIsOpen() {
        return (bossWindowOpenDate <= currentTime) && (bossWindowCloseDate > currentTime);
    }

    public boolean windowIsClosed() {
        return bossWindowCloseDate <= currentTime;
    }

    public boolean isBotMentioned() {
        System.out.println("Mentioned user:" + mentionedUsers);
        System.out.println("Bot User:" + botUser);   
        return mentionedUsers.size() > 0 && mentionedUsers.contains(botUser);            
    }
}