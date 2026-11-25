package ua.edu.nupp.kpp.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Єдиний клас, через який JavaFX спілкується із сервером.
 * GET — читання списків, POST (application/x-www-form-urlencoded) — add / update / delete / transition.
 * JSON перетворюється на клієнтські об'єкти через ObjectMapper. SQL, JDBC і MySQL тут відсутні.
 */
public class HttpIncidentClient {

    /** Збігається з finalName у server/pom.xml та context path WAR у Tomcat. */
    private static final String BASE = "http://localhost:8080/ecological-incidents-server";

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    // ---------- Локації ----------

    public List<Location> getLocations() {
        return read(get("/locations"), new TypeReference<List<Location>>() { });
    }

    public void addLocation(Location l) {
        post("/locations", locationForm("add", l));
    }

    public void updateLocation(Location l) {
        Map<String, String> p = locationForm("update", l);
        p.put("id", String.valueOf(l.getId()));
        post("/locations", p);
    }

    public void deleteLocation(long id) {
        post("/locations", form("delete", "id", String.valueOf(id)));
    }

    // ---------- Інциденти ----------

    public List<Incident> getIncidents() {
        return read(get("/incidents"), new TypeReference<List<Incident>>() { });
    }

    public void addIncident(Incident i) {
        post("/incidents", incidentForm("add", i));
    }

    public void updateIncident(Incident i) {
        Map<String, String> p = incidentForm("update", i);
        p.put("id", String.valueOf(i.getId()));
        post("/incidents", p);
    }

    public void deleteIncident(long id) {
        post("/incidents", form("delete", "id", String.valueOf(id)));
    }

    /** Клієнт лише передає бажаний стан; чи дозволений перехід, вирішує сервер. */
    public void changeIncidentState(long id, IncidentState state) {
        post("/incidents", form("transition", "id", String.valueOf(id), "state", state.name()));
    }

    /** Просить сервер перерахувати F1–F3 та інтегральний показник. */
    public void recalculateIncident(long id) {
        post("/incidents", form("recalculate", "id", String.valueOf(id)));
    }

    // ---------- Спостереження ----------

    public List<Observation> getObservations() {
        return read(get("/observations"), new TypeReference<List<Observation>>() { });
    }

    public void addObservation(Observation o) {
        post("/observations", observationForm("add", o));
    }

    public void updateObservation(Observation o) {
        Map<String, String> p = observationForm("update", o);
        p.put("id", String.valueOf(o.getId()));
        post("/observations", p);
    }

    public void deleteObservation(long id) {
        post("/observations", form("delete", "id", String.valueOf(id)));
    }

    // ---------- Заходи реагування ----------

    public List<ResponseMeasure> getMeasures() {
        return read(get("/measures"), new TypeReference<List<ResponseMeasure>>() { });
    }

    public void addMeasure(ResponseMeasure m) {
        post("/measures", measureForm("add", m));
    }

    public void updateMeasure(ResponseMeasure m) {
        Map<String, String> p = measureForm("update", m);
        p.put("id", String.valueOf(m.getId()));
        post("/measures", p);
    }

    public void deleteMeasure(long id) {
        post("/measures", form("delete", "id", String.valueOf(id)));
    }

    // ---------- Формування параметрів ----------

    private static Map<String, String> form(String action, String... keyValues) {
        Map<String, String> p = new LinkedHashMap<>();
        p.put("action", action);
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            p.put(keyValues[i], keyValues[i + 1]);
        }
        return p;
    }

    private static Map<String, String> locationForm(String action, Location l) {
        return form(action,
                "name", l.getName(),
                "region", l.getRegion(),
                "latitude", String.valueOf(l.getLatitude()),
                "longitude", String.valueOf(l.getLongitude()));
    }

    private static Map<String, String> incidentForm(String action, Incident i) {
        Map<String, String> p = form(action,
                "locationId", String.valueOf(i.getLocationId()),
                "title", i.getTitle(),
                "category", i.getCategory(),
                "description", i.getDescription());
        if (i.getRegisteredAt() != null && !i.getRegisteredAt().isBlank()) {
            p.put("registeredAt", i.getRegisteredAt());
        }
        return p;
    }

    private static Map<String, String> observationForm(String action, Observation o) {
        return form(action,
                "incidentId", String.valueOf(o.getIncidentId()),
                "observedAt", o.getObservedAt(),
                "parameter", o.getParameter(),
                "measuredValue", String.valueOf(o.getMeasuredValue()),
                "limitValue", String.valueOf(o.getLimitValue()),
                "scaleScore", String.valueOf(o.getScaleScore()),
                "durationScore", String.valueOf(o.getDurationScore()));
    }

    private static Map<String, String> measureForm(String action, ResponseMeasure m) {
        return form(action,
                "incidentId", String.valueOf(m.getIncidentId()),
                "description", m.getDescription(),
                "responsible", m.getResponsible(),
                "plannedDate", m.getPlannedDate(),
                "completed", String.valueOf(m.isCompleted()));
    }

    // ---------- HTTP ----------

    private String get(String path) {
        HttpRequest req = HttpRequest.newBuilder(URI.create(BASE + path)).GET().build();
        return send(req);
    }

    private String post(String path, Map<String, String> params) {
        HttpRequest req = HttpRequest.newBuilder(URI.create(BASE + path))
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(encode(params)))
                .build();
        return send(req);
    }

    /** Усі текстові значення кодуються (українські літери, пробіли, &, + тощо). */
    private static String encode(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(e.getKey()).append('=')
                    .append(URLEncoder.encode(e.getValue() == null ? "" : e.getValue(), StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    private String send(HttpRequest req) {
        HttpResponse<String> r;
        try {
            r = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Запит до сервера перервано", e);
        } catch (Exception e) {
            throw new ServerUnavailableException("Сервер недоступний. Перевірте, чи запущено Tomcat і MySQL.", e);
        }
        if (r.statusCode() >= 400) {
            throw new RuntimeException(errorText(r.body(), r.statusCode()));
        }
        return r.body();
    }

    /** Із JSON {"error": "..."} дістає повідомлення сервера для показу користувачу. */
    private String errorText(String body, int status) {
        try {
            String msg = mapper.readTree(body).path("error").asText("");
            if (!msg.isBlank()) {
                return msg;
            }
        } catch (Exception ignored) {
            // тіло відповіді не є JSON (наприклад, сторінка 404 від Tomcat)
        }
        return "Помилка сервера (HTTP " + status + ")";
    }

    private <T> T read(String json, TypeReference<T> type) {
        try {
            return mapper.readValue(json, type);
        } catch (Exception e) {
            throw new RuntimeException("Сервер повернув некоректний JSON", e);
        }
    }
}
