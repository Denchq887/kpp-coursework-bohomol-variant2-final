package ua.edu.nupp.kpp.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * DAO для таблиці location. Містить лише доступ до даних (SQL/JDBC),
 * без валідації та предметної логіки.
 */
public class LocationDAO {

    private static final String SQL_INSERT =
            "INSERT INTO location(name, region, latitude, longitude) VALUES(?, ?, ?, ?)";
    private static final String SQL_SELECT_ALL =
            "SELECT id, name, region, latitude, longitude FROM location ORDER BY id";
    private static final String SQL_SELECT_BY_ID =
            "SELECT id, name, region, latitude, longitude FROM location WHERE id=?";
    private static final String SQL_UPDATE =
            "UPDATE location SET name=?, region=?, latitude=?, longitude=? WHERE id=?";
    private static final String SQL_DELETE =
            "DELETE FROM location WHERE id=?";

    /** Додає запис; id створює MySQL (AUTO_INCREMENT), його також записано в l. */
    public long add(Location l) throws SQLException {
        try (Connection c = DBUtil.getConnection();
             PreparedStatement p = c.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            bind(p, l);
            p.executeUpdate();
            try (ResultSet keys = p.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("MySQL не повернув id нового запису location");
                }
                long id = keys.getLong(1);
                l.setId(id);
                return id;
            }
        }
    }

    public List<Location> getAll() throws SQLException {
        List<Location> result = new ArrayList<>();
        try (Connection c = DBUtil.getConnection();
             PreparedStatement p = c.prepareStatement(SQL_SELECT_ALL);
             ResultSet rs = p.executeQuery()) {
            while (rs.next()) {
                result.add(map(rs));
            }
        }
        return result;
    }

    /** Повертає запис або null, якщо запису з таким id немає. */
    public Location getById(long id) throws SQLException {
        try (Connection c = DBUtil.getConnection();
             PreparedStatement p = c.prepareStatement(SQL_SELECT_BY_ID)) {
            p.setLong(1, id);
            try (ResultSet rs = p.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** Оновлює запис за l.getId(); повертає false, якщо такого запису немає. */
    public boolean update(Location l) throws SQLException {
        long id = Objects.requireNonNull(l.getId(), "id запису не задано");
        try (Connection c = DBUtil.getConnection();
             PreparedStatement p = c.prepareStatement(SQL_UPDATE)) {
            bind(p, l);
            p.setLong(5, id);
            return p.executeUpdate() > 0;
        }
    }

    /** Видаляє запис; повертає false, якщо такого запису немає. */
    public boolean delete(long id) throws SQLException {
        try (Connection c = DBUtil.getConnection();
             PreparedStatement p = c.prepareStatement(SQL_DELETE)) {
            p.setLong(1, id);
            return p.executeUpdate() > 0;
        }
    }

    private static void bind(PreparedStatement p, Location l) throws SQLException {
        p.setString(1, l.getName());
        p.setString(2, l.getRegion());
        p.setDouble(3, l.getLatitude());
        p.setDouble(4, l.getLongitude());
    }

    private static Location map(ResultSet rs) throws SQLException {
        return new Location(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("region"),
                rs.getDouble("latitude"),
                rs.getDouble("longitude"));
    }
}
