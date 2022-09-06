package me.amdur;

import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;

public class DatabaseConnector { 
    
    public Connection connect() {

        // SQLite connection string
        String url = "jdbc:sqlite:/home/container/db/bosstracker.db";
        Connection conn = null;

        try {
            conn = DriverManager.getConnection(url);
            System.out.println("Successfully connected to SQLite.");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return conn;
    }
    
    // Set time of death
    public void setTod (String bossName, long tod) {

        String updateSql = "UPDATE bosstimes SET tod = '" + tod + "', windowopen = ((SELECT respawntimer FROM bosstimes WHERE bossname ='" + bossName + "' ) + '" + tod + "'), windowclose = ((SELECT respawntimer FROM bosstimes WHERE bossname ='" + bossName + "') + (SELECT randomtimer FROM bosstimes WHERE bossname ='" + bossName + "' ) + '" + tod + "') WHERE bossname = '" + bossName + "'";
        
        try (Connection conn = this.connect();
             Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(updateSql)) {            
             System.out.println("DB: TOD for " + bossName + " set to: " + tod);
            }
        catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    // Get time of death
    public long getTod (String bossName) {

        long tod = 0;
        String sql = "SELECT tod FROM bosstimes WHERE bossname = '" + bossName + "'";        

        try (Connection conn = this.connect();
             Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)) {
                System.out.println("DB: TOD for " + bossName + " is: " + rs.getLong("tod"));
                tod = rs.getLong("tod");
            }
        catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return tod;
    }

    // Get window open date
    public long getWindowOpen (String bossName) {

        long windowOpen = 0;
        String sql = "SELECT windowopen FROM bosstimes WHERE bossname = '" + bossName + "'";        

        try (Connection conn = this.connect();
             Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)) {            
                System.out.println("DB: Window for " + bossName + " opens at: " + rs.getLong("windowopen"));
                windowOpen = rs.getLong("windowopen");
            }
        catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return windowOpen;
    }

    // Get window close date
    public long getWindowClose (String bossName) {

        long windowClose = 0;
        String sql = "SELECT windowclose FROM bosstimes WHERE bossname = '" + bossName + "'";        

        try (Connection conn = this.connect();
             Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)) {            
                System.out.println("DB: Window for " + bossName + " closes at: " + rs.getLong("windowclose"));
                windowClose = rs.getLong("windowclose");
            }
        catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return windowClose;
    }

    // Get respawn timer
    public long getRespawnTimer (String bossName) {

        long respawnTimer = 0;
        String sql = "SELECT respawntimer FROM bosstimes WHERE bossname = '" + bossName + "'";
        
        try (Connection conn = this.connect();
             Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)) {            
                System.out.println("DB: Window for " + bossName + " closes at: " + rs.getLong("respawntimer"));
                respawnTimer = rs.getLong("respawntimer");
            }
        catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return respawnTimer;
    }
}