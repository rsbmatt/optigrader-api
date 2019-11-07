package org.mahabal.optigrader.api.handler;

import com.google.gson.JsonElement;
import org.jdbi.v3.core.Jdbi;
import org.mahabal.optigrader.api.dao.SessionDao;
import org.mahabal.optigrader.api.dao.UserDao;
import org.mahabal.optigrader.api.gson.LoginRequest;
import org.mahabal.optigrader.api.model.Session;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Matthew
 */
public class LoginHandler extends AbstractHandler {


    private static final String ERROR_DESERIALIZE_FAIL = "Unable to deserialize JSON object to LoginRequest.";

    public LoginHandler(final Jdbi dbi) {
        super(dbi, "LoginHandler");
    }

    @Override
    protected void handleRequest(HttpServletRequest req, HttpServletResponse resp, String ip, JsonElement payload) throws IOException {

        // deserialize the json to a login request and validate it
        final LoginRequest login = gson.fromJson(payload, LoginRequest.class);
        if (login == null || !login.validate()) {
            log.trace("Unable to constructor LoginRequest from JSON payload. {}", payload);
            badRequest(resp, ERROR_DESERIALIZE_FAIL);
            return;
        }

        // attempt a login with the provided details in the LoginRequest
        dbi.withExtension(UserDao.class, dao -> dao.login(login.getLogin(), login.getPassword())).ifPresentOrElse(user -> {
            final Session session = dbi.withExtension(SessionDao.class, dao -> dao.create(user, ip));
            log.trace("Successfully logged in: '{}' [{}]", user.fullName(), user.getNid());
            sendSession(resp, session);
        }, () -> badRequest(resp, "Invalid username/password combination."));

    }
}
