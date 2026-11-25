package ua.edu.nupp.kpp.server;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static ua.edu.nupp.kpp.server.HttpSupport.*;

/**
 * /locations
 * GET  — список локацій (або одна локація за ?id=).
 * POST — action=add | update | delete. Параметри: id, name, region, latitude, longitude.
 */
public class LocationServlet extends HttpServlet {

    private final LocationService service;

    public LocationServlet() {
        this(new LocationService());
    }

    LocationServlet(LocationService service) {
        this.service = service;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            if (req.getParameter("id") != null) {
                Location l = service.getById(longParam(req, "id"));
                if (l == null) {
                    throw new IllegalArgumentException("Локацію не знайдено");
                }
                writeJson(resp, l);
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
                Location l = new Location(
                        "update".equals(action) ? longParam(req, "id") : null,
                        text(req, "name"),
                        text(req, "region"),
                        doubleParam(req, "latitude"),
                        doubleParam(req, "longitude"));
                if ("add".equals(action)) {
                    service.add(l);
                } else {
                    service.update(l);
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
