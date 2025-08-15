package MyBot.gymbot.Data;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class DatabaseHelper {
    private Connection connection;
        private static final String DB_NAME = "users.db";

    @Value("${app.db.path}")
    private String dbPath;

    public DatabaseHelper() {
        try {
            // Формируем полный путь к БД
            String dbFile = "dataBase" + File.separator + DB_NAME;

            // Проверяем существование файла БД
            File file = new File(dbFile);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }

            // Подключаемся к базе данных
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile);

            // Создаем таблицы
            createTables();

            // Проверяем успешность создания
            if (!tableExists("users") || !tableExists("exercise_results")) {
                throw new SQLException("Таблицы не были созданы");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private boolean tableExists(String tableName) throws SQLException {
        String sql = "SELECT name FROM sqlite_master WHERE type='table' AND name=?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, tableName);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void createTables() throws SQLException {
        String createUsersTable = """
CREATE TABLE IF NOT EXISTS users (
    id TEXT PRIMARY KEY,
    user_id BIGINT UNIQUE,
    username TEXT
);
""";

        String createResultsTable = """
CREATE TABLE IF NOT EXISTS exercise_results (
    category TEXT,
    user_id BIGINT,
    exercise TEXT,
    weight INTEGER,
    PRIMARY KEY(category, user_id, exercise),
    FOREIGN KEY(user_id) REFERENCES users(user_id)
);
""";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createUsersTable);
            stmt.execute(createResultsTable);
            System.out.println("Таблицы созданы успешно");
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

    public Map<String, Integer> getUserResults(long userId) {
        String sql = "SELECT exercise, weight FROM exercise_results WHERE user_id = ?";
        Map<String, Integer> results = new HashMap<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    results.put(rs.getString("exercise"), rs.getInt("weight"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return results;
    }

    public void saveExerciseResult(long userId, String category, String exercise, int weight) {
        String sql = "INSERT OR REPLACE INTO exercise_results (category, user_id, exercise, weight) " +
                "VALUES (?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, category);
            pstmt.setLong(2, userId);
            pstmt.setString(3, exercise);
            pstmt.setInt(4, weight);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addUser(Long userId, String username) {
        String sql = "INSERT INTO users (id, user_id, username) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            String uuid = UUID.randomUUID().toString();
            pstmt.setString(1, uuid);
            pstmt.setLong(2, userId);
            pstmt.setString(3, username);
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

