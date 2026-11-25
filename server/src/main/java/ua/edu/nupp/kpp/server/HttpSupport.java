package ua.edu.nupp.kpp.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Спільні допоміжні методи HTTP-рівня: формування JSON-відповіді, JSON-помилки
 * та технічне перетворення рядкових параметрів запиту (String → long/double/дата).
 * Предметних правил тут немає — вони залишаються на рівні Service.
 */
final class HttpSupport {

    private static final Logger LOG = Logger.getLogger(HttpSupport.class.getName());

    /** Єдиний ObjectMapper: дати записуються у JSON як ISO-рядки, а не як масиви чисел. */
    static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private HttpSupport() {
    }

    static void writeJson(HttpServletResponse resp, Object data) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        MAPPER.writeValue(resp.getWriter(), data);
    }

    /**
     * IllegalArgumentException (валідація, некоректні параметри, заборонений перехід,
     * відсутній пов'язаний запис) → 400. Усе інше → 500 без показу stack trace.
     */
    static void writeError(HttpServletResponse resp, Exception e) throws IOException {
        boolean clientError = e instanceof IllegalArgumentException;
        String message;
        if (clientError) {
            message = e.getMessage() == null ? "Некоректний запит" : e.getMessage();
        } else {
            LOG.log(Level.SEVERE, "Внутрішня помилка сервера", e);
            message = "Внутрішня помилка сервера";
        }
        resp.setStatus(clientError
                ? HttpServletResponse.SC_BAD_REQUEST
                : HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        writeJson(resp, Map.of("error", message));
    }

    static String text(HttpServletRequest req, String name) {
        return req.getParameter(name);
    }

    private static String required(HttpServletRequest req, String name) {
        String v = req.getParameter(name);
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException("Не вказано параметр " + name);
        }
        return v.trim();
    }

    static long longParam(HttpServletRequest req, String name) {
        String v = required(req, name);
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Параметр " + name + " має бути цілим числом");
        }
    }

    static double doubleParam(HttpServletRequest req, String name) {
        String v = required(req, name).replace(',', '.');
        try {
            return Double.parseDouble(v);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Параметр " + name + " має бути числом");
        }
    }

    static boolean boolParam(HttpServletRequest req, String name) {
        String v = req.getParameter(name);
        return v != null && (v.equalsIgnoreCase("true") || v.equals("1"));
    }

    /** Формати: 2026-10-10 08:15:00, 2026-10-10 08:15, 2026-10-10T08:15:00. */
    static LocalDateTime dateTimeParam(HttpServletRequest req, String name) {
        return parseDateTime(required(req, name), name);
    }

    static LocalDateTime optionalDateTimeParam(HttpServletRequest req, String name) {
        String v = req.getParameter(name);
        return (v == null || v.isBlank()) ? null : parseDateTime(v.trim(), name);
    }

    private static LocalDateTime parseDateTime(String value, String name) {
        String v = value.replace(' ', 'T');
        if (v.length() == 16) {
            v = v + ":00";
        }
        try {
            return LocalDateTime.parse(v);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Параметр " + name + " має мати формат рррр-мм-дд гг:хх:сс");
        }
    }

    static LocalDate dateParam(HttpServletRequest req, String name) {
        try {
            return LocalDate.parse(required(req, name));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Параметр " + name + " має мати формат рррр-мм-дд");
        }
    }
}
