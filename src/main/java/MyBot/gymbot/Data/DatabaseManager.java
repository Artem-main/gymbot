//package MyBot.gymbot.Data;
//
//import java.sql.*;
//import java.util.ArrayList;
//import java.util.List;
//
//public class DatabaseManager {
//    private static final String DB_NAME = "bot_database.db";
//    private Connection connection;
//
//    public DatabaseManager() {
//        try {
//            connection = DriverManager.getConnection("jdbc:sqlite:" + DB_NAME);
//            createTables();
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private void createTables() throws SQLException {
//        String createUsersTable = "CREATE TABLE IF NOT EXISTS users (" +
//                "id INTEGER PRIMARY KEY," +
//                "username TEXT," +
//                "payment_status TEXT)";
//
//        String createExercisesTable = "CREATE TABLE IF NOT EXISTS exercises (" +
//                "id INTEGER PRIMARY KEY," +
//                "user_id INTEGER," +
//                "exercise_name TEXT," +
//                "weight REAL," +
//                "FOREIGN KEY(user_id) REFERENCES users(id))";
//
//        try (Statement stmt = connection.createStatement()) {
//            stmt.execute(createUsersTable);
//            stmt.execute(createExercisesTable);
//        }
//    }
//
//    public void addUser(long userId, String username, String paymentStatus) {
//        try (PreparedStatement ps = connection.prepareStatement(
//                "INSERT INTO users (id, username, payment_status) VALUES (?, ?, ?)")) {
//            ps.setLong(1, userId);
//            ps.setString(2, username);
//            ps.setString(3, paymentStatus);
//            ps.executeUpdate();
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public void addExercise(long userId, String exerciseName, double weight) {
//        try (PreparedStatement ps = connection.prepareStatement(
//                "INSERT INTO exercises (user_id, exercise_name, weight) VALUES (?, ?, ?)")) {
//            ps.setLong(1, userId);
//            ps.setString(2, exerciseName);
//            ps.setDouble(3, weight);
//            ps.executeUpdate();
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public List<String> getExercises(long userId) {
//        List<String> exercises = new ArrayList<>();
//        try (PreparedStatement ps = connection.prepareStatement(
//                "SELECT exercise_name, weight FROM exercises WHERE user_id = ?")) {
//            ps.setLong(1, userId);
//            ResultSet rs = ps.executeQuery();
//            while (rs.next()) {
//                exercises.add(rs.getString("exercise_name") + ": " + rs.getDouble("weight"));
//            }
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//        return exercises;
//    }
//
//    public void updatePaymentStatus(long userId, String status) {
//        try (PreparedStatement ps = connection.prepareStatement(
//                "UPDATE users SET payment_status = ? WHERE id = ?")) {
//            ps.setString(1, status);
//            ps.setLong(2, userId);
//            ps.executeUpdate();
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public void close() {
//        try {
//            if (connection != null) {
//                connection.close();
//            }
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//    }
//}
