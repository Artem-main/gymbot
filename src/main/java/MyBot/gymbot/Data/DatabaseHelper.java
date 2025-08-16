package MyBot.gymbot.Data;

import MyBot.gymbot.controller.TelegramBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.sql.*;
import java.util.*;

@Component
public class DatabaseHelper {
    private static final Logger log = LoggerFactory.getLogger(TelegramBot.class);
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
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                category TEXT,
                user_id BIGINT,
                exercise TEXT,
                weight INTEGER,
                timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY(user_id) REFERENCES users(user_id)
            );
            """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createResultsTable);
            log.info("Таблица результатов создана успешно");
        }

        String addIdColumn = """
            ALTER TABLE exercise_results
            ADD COLUMN id INTEGER PRIMARY KEY AUTOINCREMENT;
            """;

        try (Statement stmt = connection.createStatement()) {
            // Создаем таблицу пользователей
            stmt.execute(createUsersTable);

            // Проверяем существование столбца id
            if (!columnExists("exercise_results", "id")) {
                // Если столбца нет, добавляем его
                stmt.execute(addIdColumn);
            } else {
                // Если столбец уже существует, создаем таблицу
                stmt.execute(createResultsTable);
            }

            log.info("Таблицы созданы успешно");
        }
    }

    private boolean columnExists(String tableName, String columnName) throws SQLException {
        String sql = "PRAGMA table_info(" + tableName + ")";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                if (rs.getString("name").equals(columnName)) {
                    return true;
                }
            }
        }
        return false;
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

    public void saveExerciseResult(long userId, String category, String exercise, int weight) {
        try {
            // Сначала удаляем старые записи, оставляя только 10 последних
            String deleteOldRecords = """
                DELETE FROM exercise_results
                WHERE user_id = ? AND category = ? AND exercise = ?
                AND rowid NOT IN (
                    SELECT rowid
                    FROM exercise_results
                    WHERE user_id = ? AND category = ? AND exercise = ?
                    ORDER BY timestamp DESC
                    LIMIT 10
                )
                """;

            try (PreparedStatement pstmt = connection.prepareStatement(deleteOldRecords)) {
                pstmt.setLong(1, userId);
                pstmt.setString(2, category);
                pstmt.setString(3, exercise);
                pstmt.setLong(4, userId);
                pstmt.setString(5, category);
                pstmt.setString(6, exercise);
                pstmt.executeUpdate();
            }

            // Затем добавляем новую запись
            String sql = "INSERT INTO exercise_results (category, user_id, exercise, weight) " +
                    "VALUES (?, ?, ?, ?)";

            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, category);
                pstmt.setLong(2, userId);
                pstmt.setString(3, exercise);
                pstmt.setInt(4, weight);
                pstmt.executeUpdate();
            }

        } catch (SQLException e) {
            log.error("Ошибка при сохранении результата упражнения", e);
        }
    }

    // метод возвращает 10 последних результатов для упражнения
    public List<Map<String, Object>> getLastResults(long userId, String exercise) {
        String sql = """
                SELECT * FROM exercise_results
                WHERE user_id = ? AND exercise = ?
                ORDER BY timestamp DESC
                LIMIT 10
                """;

        List<Map<String, Object>> results = new ArrayList<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setString(2, exercise);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id", rs.getInt("id"));
                    row.put("category", rs.getString("category"));
                    row.put("weight", rs.getInt("weight"));
                    row.put("timestamp", rs.getTimestamp("timestamp"));
                    results.add(row);
                }
            }
        } catch (SQLException e) {
            log.error("Ошибка при получении результатов", e);
        }

        return results;
    }

    public Map<String, Integer> getUserResultsByCategory(long userId, String category) {
        String sql = "SELECT exercise, weight FROM exercise_results " +
                "WHERE user_id = ? AND category = ?";
        Map<String, Integer> results = new HashMap<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setString(2, category);

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

