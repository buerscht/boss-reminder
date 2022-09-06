package me.amdur;

import javax.security.auth.login.LoginException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.requests.GatewayIntent;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import java.util.Arrays;
import java.util.List;


public class BossTracker {   
    
    public static void main( String[] args ) throws LoginException, InterruptedException {

        String token = "";

        JDA api = JDABuilder.createDefault(token, GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_EMOJIS_AND_STICKERS)
                                .setStatus(OnlineStatus.ONLINE)
                                .setActivity(Activity.playing("@Boss Tracker help"))
                                .addEventListeners(new MessageListener())
                                .build()
                                .awaitReady();
        
        // Re-set scheduled messages
        MySqlDatabaseConnector  dbcon           = new MySqlDatabaseConnector();
        Guild                   server          = api.getGuildById("997407598364078150");        
        TextChannel             raidInfoChannel = server.getTextChannelById("997460044201328740");
        Long                    currentTime     = System.currentTimeMillis();
        String[]                availableBosses = {"Baium", "Barakiel", "Cabrio", "Golkonda", "Hallate", "Kernon", "Core", "Orfen", "QueenAnt", "Lilith", "Anakim", "Shadith", "Mos", "Hekaton", "Tayr"};

        for(int i = 0; i < availableBosses.length; i++) {                      

            String bossName         = availableBosses[i];
            Long tod                = dbcon.getTod(bossName);
            Long respawnTimer       = dbcon.getRespawnTimer(bossName);
            Long windowOpen         = dbcon.getWindowOpen(bossName);            
            Long bossRespawnTimer   = (currentTime - tod) + respawnTimer;
            List<Role> bossRoleList = server.getRolesByName(bossName, true);
            String windowOpenMessage;

            if(windowOpen > currentTime) {
                if (bossRoleList.size() > 0) {
                    String bossRole = Arrays.toString(bossRoleList.toArray()).replaceAll("[^0-9]","");
                    windowOpenMessage = ":camping: Spawn window for <@&" + bossRole + "> has opened!";
                }
                else {
                    windowOpenMessage = ":camping: Spawn window for " + bossName + " has opened!";
                }

                // Schedule message for respawn window
                ScheduledExecutorService scheduledTodMessage = Executors.newScheduledThreadPool(1);
                Runnable sendWindowOpenMessage = () -> raidInfoChannel.sendMessage(windowOpenMessage).queue();
                scheduledTodMessage.schedule(sendWindowOpenMessage, bossRespawnTimer, TimeUnit.MILLISECONDS);
                scheduledTodMessage.shutdown();

                System.out.println("Set scheduled message for " + bossName + " (" + windowOpen + ").");
            }            
            i++;
        } 
    }
}
