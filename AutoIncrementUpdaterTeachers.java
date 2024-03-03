import java.sql.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AutoIncrementUpdaterTeachers {

    public static void main(String[] args) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        String url = "jdbc:mysql://localhost:3306/university";
        String user = "root";
        String password = "";
        Runnable task = () -> {
            try {
                try (Connection connection = DriverManager.getConnection(url, user, password)) {
                    String selectQuery = "SELECT id FROM teachers ORDER BY id";
                    try (PreparedStatement selectStatement = connection.prepareStatement(selectQuery)) {
                        ResultSet resultSet = selectStatement.executeQuery();
                        String updateQuery = "UPDATE teachers SET id = ? WHERE id = ?";
                        try (PreparedStatement updateStatement = connection.prepareStatement(updateQuery)) {
                            int newId = 1;
                            while (resultSet.next()) {
                                int currentId = resultSet.getInt("id");
                                updateStatement.setInt(1, newId);
                                updateStatement.setInt(2, currentId);
                                updateStatement.executeUpdate();
                                newId++;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
        scheduler.scheduleAtFixedRate(task, 0, 1, TimeUnit.SECONDS);
    }
}
