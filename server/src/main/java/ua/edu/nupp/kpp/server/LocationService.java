package ua.edu.nupp.kpp.server;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

/**
 * Service для локацій: валідація, перевірка зв'язків, логування.
 * Робота з MySQL виконується лише через DAO.
 */
public class LocationService {

    private static final Logger LOG = Logger.getLogger(LocationService.class.getName());

    private final LocationDAO dao;
    private final IncidentDAO incidentDAO;

    public LocationService() {
        this(new LocationDAO(), new IncidentDAO());
    }

    /** Конструктор із зовнішніми DAO (для тестування без MySQL). */
    LocationService(LocationDAO dao, IncidentDAO incidentDAO) {
        this.dao = dao;
        this.incidentDAO = incidentDAO;
    }

    public List<Location> getAll() throws SQLException {
        return dao.getAll();
    }

    public Location getById(long id) throws SQLException {
        return dao.getById(id);
    }

    public long add(Location l) throws SQLException {
        validate(l);
        long id = dao.add(l);
        LOG.info("Додано локацію");
        return id;
    }

    public void update(Location l) throws SQLException {
        validate(l);
        if (l.getId() == null || l.getId() <= 0) {
            throw new IllegalArgumentException("Не вказано ідентифікатор локації");
        }
        if (!dao.update(l)) {
            LOG.warning("Локацію для оновлення не знайдено");
            throw new IllegalArgumentException("Локацію не знайдено");
        }
        LOG.info("Оновлено локацію");
    }

    public void delete(long id) throws SQLException {
        if (incidentDAO.countByLocationId(id) > 0) {
            LOG.warning("Відхилено видалення локації: є пов'язані інциденти");
            throw new IllegalArgumentException(
                    "Локацію не можна видалити: до неї прив'язані інциденти");
        }
        if (!dao.delete(id)) {
            LOG.warning("Локацію для видалення не знайдено");
            throw new IllegalArgumentException("Локацію не знайдено");
        }
        LOG.info("Видалено локацію");
    }

    private void validate(Location l) {
        if (l == null) {
            throw new IllegalArgumentException("Некоректні дані локації");
        }
        if (blank(l.getName()) || blank(l.getRegion())) {
            throw new IllegalArgumentException("Назва та область локації є обов'язковими");
        }
        if (l.getName().length() > 160 || l.getRegion().length() > 120) {
            throw new IllegalArgumentException("Занадто довга назва або область локації");
        }
        if (!Double.isFinite(l.getLatitude()) || l.getLatitude() < -90 || l.getLatitude() > 90) {
            throw new IllegalArgumentException("Широта має бути в межах -90..90");
        }
        if (!Double.isFinite(l.getLongitude()) || l.getLongitude() < -180 || l.getLongitude() > 180) {
            throw new IllegalArgumentException("Довгота має бути в межах -180..180");
        }
    }

    private static boolean blank(String v) {
        return v == null || v.isBlank();
    }
}
