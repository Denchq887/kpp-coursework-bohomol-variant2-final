package ua.edu.nupp.kpp.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * DAO для таблиці response_measure (заходи реагування).
 */
public class ResponseMeasureDAO {

    private static final String SQL_INSERT =
            "INSERT INTO response_measure(incident_id, description, responsible, planned_date, completed) "
                    + "VALUES(?, ?, ?, ?, ?)";
    private static final String SQL_SELECT_ALL =
            "SELECT id, incident_id, description, responsible, planned_date, completed "
                    + "FROM response_measure ORDER BY id";
    private static final String SQL_SELECT_BY_ID =
            "SELECT id, incident_id, description, responsible, planned_date, completed "
                    + "FROM response_measure WHERE id=?";
    private static final String SQL_UPDATE =
            "UPDATE response_measure SET incident_id=?, description=?, responsible=?, "
                    + "planned_date=?, completed=? WHERE id=?";
    private static final String SQL_DELETE =
            "DELETE FROM response_measure WHERE id=?";
    private static final String SQL_COUNT_BY_INCIDENT =
            "SELECT COUNT(*) FROM response_measure WHERE incident_id=?";

    /** Додає запис; id створює MySQL (AUTO_INCREMENT), його також записано в m. */
    public long add(ResponseMeasure m) throws SQLException {
        try (Connection c = DBUtil.getConnection();
             PreparedStatement p = c.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            bind(p, m);
            p.executeUpdate();
            try (ResultSet keys = p.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("MySQL не повернув id нового запису response_measure");
                }
                long id = keys.getLong(1);
                m.setId(id);
                return id;
            }
        }
    }

    public List<ResponseMeasure> getAll() throws SQLException {
        List<ResponseMeasure> result = new ArrayList<>();
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
    public ResponseMeasure getById(long id) throws SQLException {
        try (Connection c = DBUtil.getConnection();
             PreparedStatement p = c.prepareStatement(SQL_SELECT_BY_ID)) {
            p.setLong(1, id);
            try (ResultSet rs = p.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** Оновлює запис за m.getId(); повертає false, якщо такого запису немає. */
    public boolean update(ResponseMeasure m) throws SQLException {
        long id = Objects.requireNonNull(m.getId(), "id запису не задано");
        try (Connection c = DBUtil.getConnection();
             PreparedStatement p = c.prepareStatement(SQL_UPDATE)) {
            bind(p, m);
            p.setLong(6, id);
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

    /** Кількість заходів реагування інциденту. */
    public int countByIncidentId(long incidentId) throws SQLException {
        try (Connection c = DBUtil.getConnection();
             PreparedStatement p = c.prepareStatement(SQL_COUNT_BY_INCIDENT)) {
            p.setLong(1, incidentId);
            try (ResultSet rs = p.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private static void bind(PreparedStatement p, ResponseMeasure m) throws SQLException {
        p.setLong(1, m.getIncidentId());
        p.setString(2, m.getDescription());
        p.setString(3, m.getResponsible());
        p.setObject(4, m.getPlannedDate());
        p.setBoolean(5, m.isCompleted());
    }

    private static ResponseMeasure map(ResultSet rs) throws SQLException {
        return new ResponseMeasure(
                rs.getLong("id"),
                rs.getLong("incident_id"),
                rs.getString("description"),
                rs.getString("responsible"),
                rs.getObject("planned_date", LocalDate.class),
                rs.getBoolean("completed"));
    }
}
