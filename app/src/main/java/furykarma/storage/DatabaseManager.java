package furykarma.storage;

import furykarma.FuryKarma;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class DatabaseManager {

    private final FuryKarma plugin;
    private final File dbFile;
    private Connection connection;

    public DatabaseManager(FuryKarma plugin) {
        this.plugin = plugin;
        this.dbFile = new File(plugin.getDataFolder(), "karma.db");
    }

    /**
     * Initializes the connection to the SQLite database and creates the schema.
     */
    public void initialize() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        try {
            // Load the SQLite JDBC driver explicitly
            Class.forName("org.sqlite.JDBC");
            
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            connection = DriverManager.getConnection(url);

            createTables();
        } catch (ClassNotFoundException e) {
            plugin.getLogger().severe("SQLite JDBC Driver not found! Database functionality will be unavailable.");
            e.printStackTrace();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to connect to SQLite database!");
            e.printStackTrace();
        }
    }

    private void createTables() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS karma_logs (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "sender_uuid VARCHAR(36) NOT NULL," +
                "sender_name VARCHAR(16) NOT NULL," +
                "target_uuid VARCHAR(36) NOT NULL," +
                "target_name VARCHAR(16) NOT NULL," +
                "karma_type VARCHAR(8) NOT NULL," +
                "reason TEXT NOT NULL," +
                "timestamp BIGINT NOT NULL" +
                ");";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            
            // Create indexes for faster queries
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_sender_uuid ON karma_logs(sender_uuid);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_target_uuid ON karma_logs(target_uuid);");
        }
    }

    /**
     * Closes the connection to the database.
     */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to close SQLite connection properly.");
            e.printStackTrace();
        }
    }

    /**
     * Log a karma action to the database.
     */
    public void logKarma(UUID senderUuid, String senderName, UUID targetUuid, String targetName, String karmaType, String reason, long timestamp) {
        String sql = "INSERT INTO karma_logs (sender_uuid, sender_name, target_uuid, target_name, karma_type, reason, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?);";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, senderUuid.toString());
            pstmt.setString(2, senderName);
            pstmt.setString(3, targetUuid.toString());
            pstmt.setString(4, targetName);
            pstmt.setString(5, karmaType.toUpperCase());
            pstmt.setString(6, reason);
            pstmt.setLong(7, timestamp);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to insert karma log into database!");
            e.printStackTrace();
        }
    }

    /**
     * Gets the timestamp of the last karma given by a sender.
     * Returns 0 if they have never given karma.
     */
    public long getLastKarmaTime(UUID senderUuid) {
        String sql = "SELECT MAX(timestamp) FROM karma_logs WHERE sender_uuid = ?;";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, senderUuid.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to retrieve last karma timestamp!");
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Counts the total positive or negative karma received by a player.
     */
    public int getKarmaCount(UUID targetUuid, String karmaType) {
        String sql = "SELECT COUNT(*) FROM karma_logs WHERE target_uuid = ? AND karma_type = ?;";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, targetUuid.toString());
            pstmt.setString(2, karmaType.toUpperCase());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to count karma for UUID " + targetUuid);
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Retrieves recent karma logs received by a player, sorted by newest first.
     */
    public List<KarmaLogEntry> getRecentReceivedKarma(UUID targetUuid, int limit) {
        List<KarmaLogEntry> list = new ArrayList<>();
        String sql = "SELECT sender_name, karma_type, reason, timestamp FROM karma_logs WHERE target_uuid = ? ORDER BY timestamp DESC LIMIT ?;";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, targetUuid.toString());
            pstmt.setInt(2, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new KarmaLogEntry(
                            rs.getString("sender_name"),
                            rs.getString("karma_type"),
                            rs.getString("reason"),
                            rs.getLong("timestamp")
                    ));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to fetch recent karma for UUID " + targetUuid);
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Looks up a player's UUID and latest known name from the database by name (case-insensitive).
     * Returns null if not found.
     */
    public String[] getPlayerInfoByName(String name) {
        String sql = "SELECT target_uuid, target_name FROM karma_logs WHERE LOWER(target_name) = LOWER(?) ORDER BY timestamp DESC LIMIT 1;";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, name);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new String[]{ rs.getString("target_uuid"), rs.getString("target_name") };
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to look up player by name: " + name);
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Retrieves the top players ordered by average karma.
     * Orders by average (positive / total) DESC, and then by total ratings DESC.
     */
    public List<LeaderboardEntry> getTopKarma(int limit) {
        List<LeaderboardEntry> list = new ArrayList<>();
        
        String sql = "SELECT target_uuid, " +
                "  (SELECT target_name FROM karma_logs WHERE target_uuid = kl.target_uuid ORDER BY timestamp DESC LIMIT 1) as name, " +
                "  SUM(CASE WHEN karma_type = 'POSITIVE' THEN 1 ELSE 0 END) as pos, " +
                "  SUM(CASE WHEN karma_type = 'NEGATIVE' THEN 1 ELSE 0 END) as neg, " +
                "  COUNT(*) as total, " +
                "  (CAST(SUM(CASE WHEN karma_type = 'POSITIVE' THEN 1 ELSE 0 END) AS REAL) / COUNT(*)) as avg " +
                "FROM karma_logs kl " +
                "GROUP BY target_uuid " +
                "ORDER BY avg DESC, total DESC " +
                "LIMIT ?;";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("name");
                    int pos = rs.getInt("pos");
                    int neg = rs.getInt("neg");
                    int total = rs.getInt("total");
                    double avg = rs.getDouble("avg") * 100.0; // convert fraction to percentage
                    
                    list.add(new LeaderboardEntry(name, pos, neg, pos - neg, avg));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to fetch top karma leaderboard!");
            e.printStackTrace();
        }
        return list;
    }

    public static class KarmaLogEntry {
        private final String senderName;
        private final String karmaType;
        private final String reason;
        private final long timestamp;

        public KarmaLogEntry(String senderName, String karmaType, String reason, long timestamp) {
            this.senderName = senderName;
            this.karmaType = karmaType;
            this.reason = reason;
            this.timestamp = timestamp;
        }

        public String getSenderName() {
            return senderName;
        }

        public String getKarmaType() {
            return karmaType;
        }

        public String getReason() {
            return reason;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }

    public static class LeaderboardEntry {
        private final String name;
        private final int positives;
        private final int negatives;
        private final int net;
        private final double average;

        public LeaderboardEntry(String name, int positives, int negatives, int net, double average) {
            this.name = name;
            this.positives = positives;
            this.negatives = negatives;
            this.net = net;
            this.average = average;
        }

        public String getName() {
            return name;
        }

        public int getPositives() {
            return positives;
        }

        public int getNegatives() {
            return negatives;
        }

        public int getNet() {
            return net;
        }

        public double getAverage() {
            return average;
        }
    }
}
