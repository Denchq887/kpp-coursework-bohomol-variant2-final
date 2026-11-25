package ua.edu.nupp.kpp.server;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static ua.edu.nupp.kpp.server.HttpSupport.*;

/**
 * /incidents
 * GET  — список інцидентів (або один за ?id=), разом зі станом, F1–F3 та інтегральним показником.
 * POST — action=add | update | delete | transition | recalculate.
 *   add/update: locationId, title, category, description, [registeredAt], id (для update)
 *   transition: id, state (REGISTERED | VERIFIED | RESPONSE | CLOSED)
 *   recalculate / delete: id
 * Правила переходів між станами та формула показника — у IncidentService, не тут.
 */
public class IncidentServlet extends HttpServlet {

    private final IncidentService service;

    public IncidentServlet() {
        this(new IncidentService());
    }

    IncidentServlet(IncidentService service) {
        this.service = service;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            if (req.getParameter("id") != null) {
                Incident i = service.getById(longParam(req, "id"));
                if (i == null) {
                    throw new IllegalArgumentException("Інцидент не знайдено");
                }
                writeJson(resp, i);
            } else {
                writeJson(resp, service.getAll());
            }
        } catch (Exception e) {
            writeError(resp, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        try {
            String action = req.getParameter("action");
            if ("delete".equals(action)) {
                service.delete(longParam(req, "id"));
            } else if ("recalculate".equals(action)) {
                service.recalculate(longParam(req, "id"));
            } else if ("transition".equals(action)) {
                service.changeState(longParam(req, "id"), parseState(text(req, "state")));
            } else if ("add".equals(action) || "update".equals(action)) {
                Incident i = new Incident();
                if ("update".equals(action)) {
                    i.setId(longParam(req, "id"));
                }
                i.setLocationId(longParam(req, "locationId"));
                i.setTitle(text(req, "title"));
                i.setCategory(text(req, "category"));
                i.setDescription(text(req, "description"));
                i.setRegisteredAt(optionalDateTimeParam(req, "registeredAt"));
                if ("add".equals(action)) {
                    service.add(i);
                } else {
                    service.update(i);
                }
            } else {
                throw new IllegalArgumentException("Невідома операція");
            }
            writeJson(resp, service.getAll());
        } catch (Exception e) {
            writeError(resp, e);
        }
    }

    /** Технічне перетворення рядка в enum; чи дозволений перехід, вирішує Service. */
    private static IncidentState parseState(String value) {
        try {
            return IncidentState.valueOf(value == null ? "" : value.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Невідомий стан інциденту");
        }
    }
}
