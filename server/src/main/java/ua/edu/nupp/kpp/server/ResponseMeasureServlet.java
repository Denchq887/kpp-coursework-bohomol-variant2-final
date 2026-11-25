package ua.edu.nupp.kpp.server;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static ua.edu.nupp.kpp.server.HttpSupport.*;

/**
 * /measures
 * GET  — список заходів реагування.
 * POST — action=add | update | delete.
 *   Параметри: id (update/delete), incidentId, description, responsible,
 *   plannedDate (рррр-мм-дд), completed (true/false).
 */
public class ResponseMeasureServlet extends HttpServlet {

    private final ResponseMeasureService service;

    public ResponseMeasureServlet() {
        this(new ResponseMeasureService());
    }

    ResponseMeasureServlet(ResponseMeasureService service) {
        this.service = service;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            writeJson(resp, service.getAll());
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
            } else if ("add".equals(action) || "update".equals(action)) {
                ResponseMeasure m = new ResponseMeasure();
                if ("update".equals(action)) {
                    m.setId(longParam(req, "id"));
                }
                m.setIncidentId(longParam(req, "incidentId"));
                m.setDescription(text(req, "description"));
                m.setResponsible(text(req, "responsible"));
                m.setPlannedDate(dateParam(req, "plannedDate"));
                m.setCompleted(boolParam(req, "completed"));
                if ("add".equals(action)) {
                    service.add(m);
                } else {
                    service.update(m);
                }
            } else {
                throw new IllegalArgumentException("Невідома операція");
            }
            writeJson(resp, service.getAll());
        } catch (Exception e) {
            writeError(resp, e);
        }
    }
}
