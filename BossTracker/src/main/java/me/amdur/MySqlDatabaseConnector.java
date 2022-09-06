package me.amdur;

import java.sql.*;

class MySqlDatabaseConnector{
    
    Connection conn = null;
    Statement  stmt = null;
    ResultSet  rs   = null;
    String connectionUrl = "jdbc:mysql://142.132.233.69:3306/s70360_BossTracker";
    String user          = "u70360_dpvl7qwZfD";
    String password      = "+gKbGZwfXwE2PHUqsG@rvTqU";

    public Connection connect() { 

        /*String connectionUrl = "jdbc:mysql://db5009576855.hosting-data.io:3306/dbs8119775";
        String user          = "dbu852036";
        String password      = "!sDBsYdWzR6fFpx";*/

        try (Connection conn = DriverManager.getConnection(connectionUrl, user, password)) {            
            System.out.println("Successfully connected to database.");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return conn;
    }

    // Set time of death
    public void setTod (String bossName, long tod) {

        String updateSql = "UPDATE bosstimes SET tod = '" + tod + "', windowopen = ((SELECT respawntimer FROM bosstimes WHERE bossname ='" + bossName + "' ) + '" + tod + "'), windowclose = ((SELECT respawntimer FROM bosstimes WHERE bossname ='" + bossName + "') + (SELECT randomtimer FROM bosstimes WHERE bossname ='" + bossName + "' ) + '" + tod + "') WHERE bossname = '" + bossName + "'";

        try(Connection conn = DriverManager.getConnection(connectionUrl, user, password);
            Statement stmt = conn.createStatement();
            ) {

            // Let us update age of the record with ID = 103;
            int rows = stmt.executeUpdate(updateSql);
            System.out.println("DB: TOD for " + bossName + " set to " + tod + ". (Rows affected: " + rows +")");                 
            }
        catch (SQLException e) {
            e.printStackTrace();
        } 
    }

    // Get time of death
    public long getTod (String bossName) {

        long tod = 0;
        String sql = "SELECT tod FROM bosstimes WHERE bossname = '" + bossName + "'";        

        try (Connection conn = DriverManager.getConnection(connectionUrl, user, password);
            Statement stmt  = conn.createStatement();
            ResultSet rs    = stmt.executeQuery(sql)) {
                if(rs.next()) {
                    //System.out.println("DB: TOD for " + bossName + " is: " + rs.getLong("tod"));
                    tod = rs.getLong("tod");
                    rs.close(); 
                    stmt.close();
                }
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

        try (Connection conn = DriverManager.getConnection(connectionUrl, user, password);
            Statement stmt  = conn.createStatement();
            ResultSet rs    = stmt.executeQuery(sql)) {  
                if(rs.next()) {          
                    //System.out.println("DB: Window for " + bossName + " opens at: " + rs.getLong("windowopen"));
                    windowOpen = rs.getLong("windowopen");
                    rs.close(); 
                    stmt.close();
                }
            }
        catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return windowOpen;
    }

    // Get all future windows
    public long getAllWindows (String bossName) {

        long windowOpen = 0;
        long currentTime = System.currentTimeMillis();

        String sql = "SELECT windowopen FROM bosstimes WHERE windowopen != null AND windowopen > " + currentTime + "";        

        try (Connection conn = DriverManager.getConnection(connectionUrl, user, password);
            Statement stmt  = conn.createStatement();
            ResultSet rs    = stmt.executeQuery(sql)) {  
                while(rs.next()) {          
                    //System.out.println("DB: Window for " + bossName + " opens at: " + rs.getLong("windowopen"));
                    windowOpen = rs.getLong("windowopen");
                    
                }
                rs.close(); 
                stmt.close();
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

        try (Connection conn = DriverManager.getConnection(connectionUrl, user, password);
            Statement stmt  = conn.createStatement();
            ResultSet rs    = stmt.executeQuery(sql)) { 
                if(rs.next()) {           
                    //System.out.println("DB: Window for " + bossName + " closes at: " + rs.getLong("windowclose"));
                    windowClose = rs.getLong("windowclose");
                    rs.close(); 
                    stmt.close();
                }
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
        
        try (Connection conn = DriverManager.getConnection(connectionUrl, user, password);
            Statement stmt  = conn.createStatement();
            ResultSet rs    = stmt.executeQuery(sql)) {                 
                if(rs.next()) {
                    //System.out.println("DB: Window for " + bossName + " closes at: " + rs.getLong("respawntimer"));
                    respawnTimer = rs.getLong("respawntimer");
                    rs.close(); 
                    stmt.close();
                }                
            }
        catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return respawnTimer;
    }
}