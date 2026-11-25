package ua.edu.nupp.kpp.server;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static ua.edu.nupp.kpp.server.HttpSupport.*;

/**
 * /observations
 * GET  — список спостережень; з параметром ?incidentId= — лише спостереження інциденту.
 * POST — action=add | update | delete.
 *   Параметри: id (update/delete), incidentId, observedAt, parameter,
 *   measuredValue, limitValue, scaleScore, durationScore.
 * Після змін Service сам перераховує інтегральний показник інциденту.
 */
public class ObservationServlet extends HttpServlet {

    private final ObservationService service;

    public ObservationServlet() {
        this(new ObservationService());
    }

    ObservationServlet(ObservationService service) {
        this.service = service;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            if (req.getParameter("incidentId") != null) {
                writeJson(resp, service.getByIncidentId(longParam(req, "incidentId")));
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
            } else if ("add".equals(action) || "update".equals(action)) {
                Observation o = new Observation();
                if ("update".equals(action)) {
                    o.setId(longParam(req, "id"));
                }
                o.setIncidentId(longParam(req, "incidentId"));
                o.setObservedAt(dateTimeParam(req, "observedAt"));
                o.setParameter(text(req, "parameter"));
                o.setMeasuredValue(doubleParam(req, "measuredValue"));
                o.setLimitValue(doubleParam(req, "limitValue"));
                o.setScaleScore(doubleParam(req, "scaleScore"));
                o.setDurationScore(doubleParam(req, "durationScore"));
                if ("add".equals(action)) {
                    service.add(o);
                } else {
                    service.update(o);
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
