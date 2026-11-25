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
 * DAO для таблиці observation. Додатково має вузькі операції,
 * потрібні предметній моделі: вибірка та підрахунок за incident_id.
 */
public class ObservationDAO {

    private static final String SQL_INSERT =
            "INSERT INTO observation(incident_id, observed_at, parameter, measured_value, "
                    + "limit_value, scale_score, duration_score) VALUES(?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_SELECT_ALL =
            "SELECT id, incident_id, observed_at, parameter, measured_value, limit_value, "
                    + "scale_score, duration_score FROM observation ORDER BY id";
    private static final String SQL_SELECT_BY_ID =
            "SELECT id, incident_id, observed_at, parameter, measured_value, limit_value, "
                    + "scale_score, duration_score FROM observation WHERE id=?";
    private static final String SQL_SELECT_BY_INCIDENT =
            "SELECT id, incident_id, observed_at, parameter, measured_value, limit_value, "
                    + "scale_score, duration_score FROM observation WHERE incident_id=? "
                    + "ORDER BY observed_at, id";
    private static final String SQL_UPDATE =
            "UPDATE observation SET incident_id=?, observed_at=?, parameter=?, measured_value=?, "
                    + "limit_value=?, scale_score=?, duration_score=? WHERE id=?";
    private static final String SQL_DELETE =
            "DELETE FROM observation WHERE id=?";
    private static final String SQL_COUNT_BY_INCIDENT =
            "SELECT COUNT(*) FROM observation WHERE incident_id=?";

    /** Додає запис; id створює MySQL (AUTO_INCREMENT), його також записано в o. */
    public long add(Observation o) throws SQLException {
        try (Connection c = DBUtil.getConnection();
             PreparedStatement p = c.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            bind(p, o);
            p.executeUpdate();
            try (ResultSet keys = p.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("MySQL не повернув id нового запису observation");
                }
                long id = keys.getLong(1);
                o.setId(id);
                return id;
            }
        }
    }

    public List<Observation> getAll() throws SQLException {
        List<Observation> result = new ArrayList<>();
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
    public Observation getById(long id) throws SQLException {
        try (Connection c = DBUtil.getConnection();
             PreparedStatement p = c.prepareStatement(SQL_SELECT_BY_ID)) {
            p.setLong(1, id);
            try (ResultSet rs = p.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** Усі спостереження одного інциденту (від найстаріших до найновіших). */
    public List<Observation> getByIncidentId(long incidentId) throws SQLException {
        List<Observation> result = new ArrayList<>();
        try (Connection c = DBUtil.getConnection();
             PreparedStatement p = c.prepareStatement(SQL_SELECT_BY_INCIDENT)) {
            p.setLong(1, incidentId);
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs));
                }
            }
        }
        return result;
    }

    /** Оновлює запис за o.getId(); повертає false, якщо такого запису немає. */
    public boolean update(Observation o) throws SQLException {
        long id = Objects.requireNonNull(o.getId(), "id запису не задано");
        try (Connection c = DBUtil.getConnection();
             PreparedStatement p = c.prepareStatement(SQL_UPDATE)) {
            bind(p, o);
            p.setLong(8, id);
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

    /** Кількість спостережень інциденту. */
    public int countByIncidentId(long incidentId) throws SQLException {
        try (Connection c = DBUtil.getConnection();
             PreparedStatement p = c.prepareStatement(SQL_COUNT_BY_INCIDENT)) {
            p.setLong(1, incidentId);
            try (ResultSet rs = p.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private static void bind(PreparedStatement p, Observation o) throws SQLException {
        p.setLong(1, o.getIncidentId());
        p.setObject(2, o.getObservedAt());
        p.setString(3, o.getParameter());
        p.setDouble(4, o.getMeasuredValue());
        p.setDouble(5, o.getLimitValue());
        p.setDouble(6, o.getScaleScore());
        p.setDouble(7, o.getDurationScore());
    }

    private static Observation map(ResultSet rs) throws SQLException {
        return new Observation(
                rs.getLong("id"),
                rs.getLong("incident_id"),
                rs.getObject("observed_at", LocalDateTime.class),
                rs.getString("parameter"),
                rs.getDouble("measured_value"),
                rs.getDouble("limit_value"),
                rs.getDouble("scale_score"),
                rs.getDouble("duration_score"));
    }
}
