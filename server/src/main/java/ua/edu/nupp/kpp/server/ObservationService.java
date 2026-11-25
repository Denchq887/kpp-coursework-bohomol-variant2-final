package ua.edu.nupp.kpp.server;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

/**
 * Service спостережень: валідація значень і факторів, перевірка існування інциденту,
 * автоматичний перерахунок інтегрального показника інциденту після змін.
 */
public class ObservationService {

    private static final Logger LOG = Logger.getLogger(ObservationService.class.getName());

    private final ObservationDAO dao;
    private final IncidentDAO incidentDAO;
    private final IncidentService incidentService;

    public ObservationService() {
        this(new ObservationDAO(), new IncidentDAO(), new IncidentService());
    }

    /** Конструктор із зовнішніми залежностями (для тестування без MySQL). */
    ObservationService(ObservationDAO dao, IncidentDAO incidentDAO, IncidentService incidentService) {
        this.dao = dao;
        this.incidentDAO = incidentDAO;
        this.incidentService = incidentService;
    }

    public List<Observation> getAll() throws SQLException {
        return dao.getAll();
    }

    public Observation getById(long id) throws SQLException {
        return dao.getById(id);
    }

    public List<Observation> getByIncidentId(long incidentId) throws SQLException {
        return dao.getByIncidentId(incidentId);
    }

    public long add(Observation o) throws SQLException {
        validate(o);
        requireIncident(o.getIncidentId());
        long id = dao.add(o);
        LOG.info("Додано спостереження");
        incidentService.recalculate(o.getIncidentId());
        return id;
    }

    public void update(Observation o) throws SQLException {
        validate(o);
        if (o.getId() == null || o.getId() <= 0) {
            throw new IllegalArgumentException("Не вказано ідентифікатор спостереження");
        }
        Observation old = dao.getById(o.getId());
        if (old == null) {
            LOG.warning("Спостереження для оновлення не знайдено");
            throw new IllegalArgumentException("Спостереження не знайдено");
        }
        requireIncident(o.getIncidentId());
        dao.update(o);
        LOG.info("Оновлено спостереження");
        incidentService.recalculate(o.getIncidentId());
        if (old.getIncidentId() != o.getIncidentId()) {
            incidentService.recalculate(old.getIncidentId());
        }
    }

    public void delete(long id) throws SQLException {
        Observation old = dao.getById(id);
        if (old == null) {
            LOG.warning("Спостереження для видалення не знайдено");
            throw new IllegalArgumentException("Спостереження не знайдено");
        }
        dao.delete(id);
        LOG.info("Видалено спостереження");
        incidentService.recalculate(old.getIncidentId());
    }

    private void requireIncident(long incidentId) throws SQLException {
        if (incidentDAO.getById(incidentId) == null) {
            throw new IllegalArgumentException("Інцидент не знайдено");
        }
    }

    private void validate(Observation o) {
        if (o == null) {
            throw new IllegalArgumentException("Некоректні дані спостереження");
        }
        if (o.getIncidentId() <= 0) {
            throw new IllegalArgumentException("Некоректний ідентифікатор інциденту");
        }
        if (o.getObservedAt() == null) {
            throw new IllegalArgumentException("Не вказано час спостереження");
        }
        if (blank(o.getParameter())) {
            throw new IllegalArgumentException("Назва контрольованого показника є обов'язковою");
        }
        if (o.getParameter().length() > 80) {
            throw new IllegalArgumentException("Занадто довга назва контрольованого показника");
        }
        if (!Double.isFinite(o.getMeasuredValue()) || o.getMeasuredValue() < 0
                || !Double.isFinite(o.getLimitValue()) || o.getLimitValue() <= 0) {
            throw new IllegalArgumentException("Некоректні числові дані спостереження");
        }
        if (!inRange(o.getScaleScore()) || !inRange(o.getDurationScore())) {
            throw new IllegalArgumentException("F2 і F3 мають бути в діапазоні 0..100");
        }
    }

    private static boolean inRange(double factor) {
        return Double.isFinite(factor) && factor >= 0 && factor <= 100;
    }

    private static boolean blank(String v) {
        return v == null || v.isBlank();
    }
}
