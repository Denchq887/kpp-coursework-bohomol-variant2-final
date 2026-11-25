package ua.edu.nupp.kpp.server;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

/**
 * Service заходів реагування: валідація полів та перевірка існування інциденту.
 */
public class ResponseMeasureService {

    private static final Logger LOG = Logger.getLogger(ResponseMeasureService.class.getName());

    private final ResponseMeasureDAO dao;
    private final IncidentDAO incidentDAO;

    public ResponseMeasureService() {
        this(new ResponseMeasureDAO(), new IncidentDAO());
    }

    /** Конструктор із зовнішніми DAO (для тестування без MySQL). */
    ResponseMeasureService(ResponseMeasureDAO dao, IncidentDAO incidentDAO) {
        this.dao = dao;
        this.incidentDAO = incidentDAO;
    }

    public List<ResponseMeasure> getAll() throws SQLException {
        return dao.getAll();
    }

    public ResponseMeasure getById(long id) throws SQLException {
        return dao.getById(id);
    }

    public long add(ResponseMeasure m) throws SQLException {
        validate(m);
        requireIncident(m.getIncidentId());
        long id = dao.add(m);
        LOG.info("Додано захід реагування");
        return id;
    }

    public void update(ResponseMeasure m) throws SQLException {
        validate(m);
        if (m.getId() == null || m.getId() <= 0) {
            throw new IllegalArgumentException("Не вказано ідентифікатор заходу реагування");
        }
        requireIncident(m.getIncidentId());
        if (!dao.update(m)) {
            LOG.warning("Захід реагування для оновлення не знайдено");
            throw new IllegalArgumentException("Захід реагування не знайдено");
        }
        LOG.info("Оновлено захід реагування");
    }

    public void delete(long id) throws SQLException {
        if (!dao.delete(id)) {
            LOG.warning("Захід реагування для видалення не знайдено");
            throw new IllegalArgumentException("Захід реагування не знайдено");
        }
        LOG.info("Видалено захід реагування");
    }

    private void requireIncident(long incidentId) throws SQLException {
        if (incidentDAO.getById(incidentId) == null) {
            throw new IllegalArgumentException("Інцидент не знайдено");
        }
    }

    private void validate(ResponseMeasure m) {
        if (m == null) {
            throw new IllegalArgumentException("Некоректні дані заходу реагування");
        }
        if (m.getIncidentId() <= 0) {
            throw new IllegalArgumentException("Некоректний ідентифікатор інциденту");
        }
        if (blank(m.getDescription()) || blank(m.getResponsible())) {
            throw new IllegalArgumentException("Опис і відповідальний є обов'язковими");
        }
        if (m.getDescription().length() > 500 || m.getResponsible().length() > 120) {
            throw new IllegalArgumentException("Занадто довгий опис або назва відповідального");
        }
        if (m.getPlannedDate() == null) {
            throw new IllegalArgumentException("Не вказано планову дату заходу");
        }
    }

    private static boolean blank(String v) {
        return v == null || v.isBlank();
    }
}
