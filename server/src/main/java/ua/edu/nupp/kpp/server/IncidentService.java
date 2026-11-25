package ua.edu.nupp.kpp.server;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.logging.Logger;

/**
 * Service екологічних інцидентів — об'єкта чотиристанного життєвого циклу.
 * Тут розміщені валідація, перевірка локації, розрахунок інтегрального показника
 * (через RiskCalculator), правила переходів між станами та логування.
 */
public class IncidentService {

    private static final Logger LOG = Logger.getLogger(IncidentService.class.getName());

    private final IncidentDAO dao;
    private final LocationDAO locationDAO;
    private final ObservationDAO observationDAO;
    private final ResponseMeasureDAO measureDAO;

    public IncidentService() {
        this(new IncidentDAO(), new LocationDAO(), new ObservationDAO(), new ResponseMeasureDAO());
    }

    /** Конструктор із зовнішніми DAO (для тестування без MySQL). */
    IncidentService(IncidentDAO dao,
                    LocationDAO locationDAO,
                    ObservationDAO observationDAO,
                    ResponseMeasureDAO measureDAO) {
        this.dao = dao;
        this.locationDAO = locationDAO;
        this.observationDAO = observationDAO;
        this.measureDAO = measureDAO;
    }

    /**
     * Дозволені переходи життєвого циклу:
     * REGISTERED → VERIFIED → RESPONSE → CLOSED. Інших переходів немає.
     */
    public static boolean isAllowedTransition(IncidentState from, IncidentState to) {
        return (from == IncidentState.REGISTERED && to == IncidentState.VERIFIED)
                || (from == IncidentState.VERIFIED && to == IncidentState.RESPONSE)
                || (from == IncidentState.RESPONSE && to == IncidentState.CLOSED);
    }

    public List<Incident> getAll() throws SQLException {
        return dao.getAll();
    }

    public Incident getById(long id) throws SQLException {
        return dao.getById(id);
    }

    /**
     * Реєструє новий інцидент. Стан (REGISTERED) і фактори (F1–F3, I = 0, бо
     * спостережень ще немає) призначає сервер: передані користувачем значення ігноруються.
     * Якщо час реєстрації не вказано, береться поточний.
     */
    public long add(Incident i) throws SQLException {
        validate(i);
        requireLocation(i.getLocationId());
        i.setId(null);
        if (i.getRegisteredAt() == null) {
            i.setRegisteredAt(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        }
        i.setState(IncidentState.REGISTERED);
        i.setF1(0);
        i.setF2(0);
        i.setF3(0);
        i.setIntegralIndex(0);
        long id = dao.add(i);
        LOG.info("Зареєстровано інцидент");
        return id;
    }

    /**
     * Звичайне редагування даних інциденту. Стан і фактори не змінюються:
     * стан змінює лише changeState(...), фактори — recalculate(...).
     */
    public void update(Incident i) throws SQLException {
        validate(i);
        if (i.getId() == null || i.getId() <= 0) {
            throw new IllegalArgumentException("Не вказано ідентифікатор інциденту");
        }
        Incident old = dao.getById(i.getId());
        if (old == null) {
            LOG.warning("Інцидент для оновлення не знайдено");
            throw new IllegalArgumentException("Інцидент не знайдено");
        }
        requireLocation(i.getLocationId());
        old.setLocationId(i.getLocationId());
        old.setTitle(i.getTitle());
        old.setCategory(i.getCategory());
        old.setDescription(i.getDescription());
        if (i.getRegisteredAt() != null) {
            old.setRegisteredAt(i.getRegisteredAt());
        }
        dao.update(old);
        LOG.info("Оновлено інцидент");
    }

    /** Змінює стан інциденту лише на наступний у життєвому циклі. */
    public void changeState(long id, IncidentState next) throws SQLException {
        if (next == null) {
            throw new IllegalArgumentException("Не вказано новий стан");
        }
        Incident current = dao.getById(id);
        if (current == null) {
            LOG.warning("Інцидент для зміни стану не знайдено");
            throw new IllegalArgumentException("Інцидент не знайдено");
        }
        if (!isAllowedTransition(current.getState(), next)) {
            LOG.warning("Недопустимий перехід стану: " + current.getState() + " -> " + next);
            throw new IllegalArgumentException("Недопустимий перехід стану");
        }
        dao.updateState(id, next);
        LOG.info("Змінено стан: " + current.getState() + " -> " + next);
    }

    /**
     * Перераховує F1, F2, F3 та інтегральний показник I за поточними спостереженнями
     * інциденту й зберігає їх. Користувач не вводить ці значення вручну.
     */
    public Incident recalculate(long incidentId) throws SQLException {
        Incident incident = dao.getById(incidentId);
        if (incident == null) {
            throw new IllegalArgumentException("Інцидент не знайдено");
        }
        RiskCalculator.Factors f = RiskCalculator.aggregate(observationDAO.getByIncidentId(incidentId));
        incident.setF1(f.f1());
        incident.setF2(f.f2());
        incident.setF3(f.f3());
        incident.setIntegralIndex(RiskCalculator.calculate(f.f1(), f.f2(), f.f3()));
        dao.update(incident);
        LOG.info("Перераховано інтегральний показник інциденту");
        return incident;
    }

    public void delete(long id) throws SQLException {
        if (observationDAO.countByIncidentId(id) > 0 || measureDAO.countByIncidentId(id) > 0) {
            LOG.warning("Відхилено видалення інциденту: є спостереження або заходи реагування");
            throw new IllegalArgumentException(
                    "Інцидент не можна видалити: є пов'язані спостереження або заходи реагування");
        }
        if (!dao.delete(id)) {
            LOG.warning("Інцидент для видалення не знайдено");
            throw new IllegalArgumentException("Інцидент не знайдено");
        }
        LOG.info("Видалено інцидент");
    }

    private void requireLocation(long locationId) throws SQLException {
        if (locationDAO.getById(locationId) == null) {
            throw new IllegalArgumentException("Локацію не знайдено");
        }
    }

    private void validate(Incident i) {
        if (i == null) {
            throw new IllegalArgumentException("Некоректні дані інциденту");
        }
        if (i.getLocationId() <= 0) {
            throw new IllegalArgumentException("Некоректний ідентифікатор локації");
        }
        if (blank(i.getTitle()) || blank(i.getCategory()) || blank(i.getDescription())) {
            throw new IllegalArgumentException("Назва, категорія та опис інциденту є обов'язковими");
        }
        if (i.getTitle().length() > 200 || i.getCategory().length() > 40
                || i.getDescription().length() > 1000) {
            throw new IllegalArgumentException("Занадто довгий текст у полях інциденту");
        }
    }

    private static boolean blank(String v) {
        return v == null || v.isBlank();
    }
}
