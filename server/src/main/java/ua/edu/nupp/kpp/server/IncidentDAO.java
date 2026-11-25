package ua.edu.nupp.kpp.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * DAO для таблиці incident. Стан зберігається як текст (state.name()),
 * а читається через IncidentState.valueOf(...). DAO не перевіряє
 * допустимість переходів між станами — це робить IncidentService.
 */
public class IncidentDAO {

    private static final String SQL_INSERT =
            "INSERT INTO incident(location_id, title, category, description, registered_at, "
                    + "state, f1, f2, f3, integral_index) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_SELECT_ALL =
            "SELECT id, location_id, title, category, description, registered_at, state, "
                    + "f1, f2, f3, integral_index FROM incident ORDER BY id";
    private static final String SQL_SELECT_BY_ID =
            "SELECT id, location_id, title, category, description, registered_at, state, "
                    + "f1, f2, f3, integral_index FROM incident WHERE id=?";
    /** Звичайне редагування даних: стан (state) тут навмисно не змінюється. */
    private static final String SQL_UPDATE =
            "UPDATE incident SET location_id=?, title=?, category=?, description=?, "
                    + "registered_at=?, f1=?, f2=?, f3=?, integral_index=? WHERE id=?";
    private static final String SQL_UPDATE_STATE =
            "UPDATE incident SET state=? WHERE id=?";
    private static final String SQL_DELETE =
            "DELETE FROM incident WHERE id=?";
    private static final String SQL_COUNT_BY_LOCATION =
            "SELECT COUNT(*) FROM incident WHERE location_id=?";

    /** Додає запис; id створює MySQL (AUTO_INCREMENT), його також записано в i. */
    public long add(Incident i) throws SQLException {
        try (Connection c = DBUtil.getConnection();
             PreparedStatement p = c.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            p.setLong(1, i.getLocationId());
            p.setString(2, i.getTitle());
            p.setString(3, i.getCategory());
            p.setString(4, i.getDescription());
            p.setObject(5, i.getRegisteredAt());
            p.setString(6, i.getState().name());
            p.setDouble(7, i.getF1());
            p.setDouble(8, i.getF2());
            p.setDouble(9, i.getF3());
            p.setDouble(10, i.getIntegralIndex());
            p.executeUpdate();
            try (ResultSet keys = p.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("MySQL не повернув id нового запису incident");
                }
                long id = keys.getLong(1);
                i.setId(id);
                return id;
            }
        }
    }

    public List<Incident> getAll() throws SQLException {
        List<Incident> result = new ArrayList<>();
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
    public Incident getById(long id) throws SQLException {
        try (Connection c = DBUtil.getConnection();
             PreparedStatement p = c.prepareStatement(SQL_SELECT_BY_ID)) {
            p.setLong(1, id);
            try (ResultSet rs = p.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /**
     * Оновлює дані інциденту (включно з f1, f2, f3, integral_index), крім стану.
     * Повертає false, якщо запису немає.
     */
    public boolean update(Incident i) throws SQLException {
        long id = Objects.requireNonNull(i.getId(), "id запису не задано");
        try (Connection c = DBUtil.getConnection();
             PreparedStatement p = c.prepareStatement(SQL_UPDATE)) {
            p.setLong(1, i.getLocationId());
            p.setString(2, i.getTitle());
            p.setString(3, i.getCategory());
            p.setString(4, i.getDescription());
            p.setObject(5, i.getRegisteredAt());
            p.setDouble(6, i.getF1());
            p.setDouble(7, i.getF2());
            p.setDouble(8, i.getF3());
            p.setDouble(9, i.getIntegralIndex());
            p.setLong(10, id);
            return p.executeUpdate() > 0;
        }
    }

    /** Лише записує переданий стан; допустимість переходу тут не перевіряється. */
    public boolean updateState(long id, IncidentState state) throws SQLException {
        try (Connection c = DBUtil.getConnection();
             PreparedStatement p = c.prepareStatement(SQL_UPDATE_STATE)) {
            p.setString(1, state.name());
            p.setLong(2, id);
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

    /** Кількість інцидентів, пов'язаних із локацією. */
    public int countByLocationId(long locationId) throws SQLException {
        try (Connection c = DBUtil.getConnection();
             PreparedStatement p = c.prepareStatement(SQL_COUNT_BY_LOCATION)) {
            p.setLong(1, locationId);
            try (ResultSet rs = p.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private static Incident map(ResultSet rs) throws SQLException {
        return new Incident(
                rs.getLong("id"),
                rs.getLong("location_id"),
                rs.getString("title"),
                rs.getString("category"),
                rs.getString("description"),
                rs.getObject("registered_at", LocalDateTime.class),
                IncidentState.valueOf(rs.getString("state")),
                rs.getDouble("f1"),
                rs.getDouble("f2"),
                rs.getDouble("f3"),
                rs.getDouble("integral_index"));
    }
}
