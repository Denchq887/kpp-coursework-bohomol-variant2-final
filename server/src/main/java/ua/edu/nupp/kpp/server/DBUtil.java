package ua.edu.nupp.kpp.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DBUtil {

    private static final String URL =
            "jdbc:mysql://localhost:3306/ecological_incidents";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    private DBUtil() {
    }

    public static Connection getConnection() throws SQLException {
        try {
            // Явне завантаження драйвера: у Tomcat він лежить у WEB-INF/lib застосунку
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Не знайдено драйвер MySQL Connector/J", e);
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
