package MyBot.gymbot.Data;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.*;

@Component
public class DatabaseHelper {
    private Connection connection;
        private static final String DB_NAME = "users.db";

    @Value("${app.db.path}")
    private String dbPath;

    public DatabaseHelper() {
        try {
            // Подключаемся к базе данных
            connection = DriverManager.getConnection("jdbc:sqlite:" + DB_NAME);
            createTable();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createTable() throws SQLException {
        String createTableSql = """
                CREATE TABLE IF NOT EXISTS users (
                    id BIGINT PRIMARY KEY,
                    user_id BIGINT UNIQUE,
                    username TEXT
                );
                """;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTableSql);
        }
    }

    public void close() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addUser(Long userId, String username) {
        String sql = "INSERT INTO users (user_id, username) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setString(2, username);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            // Если пользователь уже существует, обновим его данные
            if (e.getSQLState().equals("23505")) {
                updateUser(userId, username);
            } else {
                e.printStackTrace();
            }
        }
    }

    private void updateUser(Long userId, String username) {
        String sql = "UPDATE users SET username = ? WHERE user_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setLong(2, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public User getUser(long userId) {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getLong("user_id"),
                            rs.getString("username")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

}


    class User {
    private long userId;
    private String username;

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

        public void setUsername(String username) {
            this.username = username;
        }
    public User(long userId, String username) {
        this.userId = userId;
        this.username = username;
    }
}

