package org.mahabal.optigrader.api.handler;

import com.google.common.io.CharStreams;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.jdbi.v3.core.Jdbi;
import org.mahabal.optigrader.api.model.Session;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;

/**
 * @author Matthew
 */
public abstract class AbstractHandler extends HttpServlet {

    static final String KEY_SUCCESS = "success";
    static final String ERROR_INVALID_JSON = "Payload was not a valid JSON object!";


    // GSON singleton to be used by every handler for deserialization/serialization of JSON
    static final Gson gson = new Gson();
    protected Logger log;
    static JsonParser parser = new JsonParser();

    final Jdbi dbi;

    AbstractHandler(final Jdbi dbi, final String name) {
        this.log = LogManager.getLogger(name);
        this.dbi = dbi;
    }

    String getIpAddress(final HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null) ip = request.getRemoteAddr();
        return ip;
    }

    String getPayload(final BufferedReader reader) {
        try {
            return String.join("", CharStreams.readLines(reader));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    void badRequest(final HttpServletResponse response, final String message) {
        error(response, HttpServletResponse.SC_BAD_REQUEST, message);
    }

    void ok(final HttpServletResponse response, final JsonElement payload) {
        respond(response, HttpServletResponse.SC_OK, payload);
    }

    void ok(final HttpServletResponse response, final String status) {
        final JsonObject object = new JsonObject();
        object.addProperty("status", status);
        ok(response, object);
    }

    private void respond(final HttpServletResponse response, final int opcode, final JsonElement payload) {
        try {
            response.setStatus(opcode);
            response.addHeader("Access-Control-Allow-Origin", "*");
            response.getWriter().write(payload.toString());
            log.trace("response status: {}, payload: {}", opcode, payload.toString());
            response.getWriter().flush();
        } catch (final IOException ioe) {
            ioe.printStackTrace();
        }
    }

    void error(final HttpServletResponse response, final int opcode, final String message) {
        final JsonObject payload = new JsonObject();
        payload.addProperty("error", message);
        respond(response, opcode, payload);
    }

    void sendSession(HttpServletResponse response, Session session) {
        if (session != null) {
            log.info("Session sent for user: " + session.nid + ", token: " + session.token);
            final JsonObject payload = new JsonObject();
            payload.addProperty("token", session.token);
            ok(response, payload);
        }
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        // get the IP that the request came from
        final String ip = this.getIpAddress(req);
        log.trace("connection recv: {}", ip);

        // parse the gson payload and make sure all good
        final JsonElement element = parser.parse(getPayload(req.getReader()));
        if (element == null || !element.isJsonObject()) {
            badRequest(resp, ERROR_INVALID_JSON);
            return;
        }

        try {

            log.debug("received payload: {}", element);
            this.handleRequest(req, resp, ip, element);

        } catch (final Exception e) {

            error(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());

        }
    }

    protected abstract void handleRequest(final HttpServletRequest req, final HttpServletResponse resp,
                                          final String ip, final JsonElement payload) throws Exception;

}