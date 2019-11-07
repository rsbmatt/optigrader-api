package org.mahabal.optigrader.api.handler;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import com.google.gson.JsonElement;
import org.apache.commons.validator.EmailValidator;
import org.jdbi.v3.core.Jdbi;
import org.mahabal.optigrader.api.dao.SessionDao;
import org.mahabal.optigrader.api.dao.UserDao;
import org.mahabal.optigrader.api.gson.RegisterRequest;
import org.mahabal.optigrader.api.model.Session;
import org.mahabal.optigrader.api.model.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Matthew
 */
public class RegistrationHandler extends AbstractHandler {

    private static final EmailValidator EMAIL_VALIDATOR = EmailValidator.getInstance();

    private static final String ERROR_DESERIALIZE_FAIL = "Unable to construct user from payload!";
    public static final String ERROR_INVALID_EMAIL = "Invalid email address!";
    private static final String ERROR_EMAIL_EXISTS = "Email is already in use";

    public RegistrationHandler(final Jdbi dbi) {
        super(dbi, "RegistrationHandler");
    }

    protected void handleRequest(HttpServletRequest req, HttpServletResponse resp, String ip, JsonElement payload)
            throws Exception {
        try {
            // deserialize the json to a registration request and validate it
            final RegisterRequest registrant = gson.fromJson(payload, RegisterRequest.class);
            if (registrant == null || !registrant.validate()) {

                // not a full registration request, but it might be an email check...
                if (registrant != null) {
                    if (EMAIL_VALIDATOR.isValid(registrant.getLogin())) {
                        log.trace("E-mail usage check: getUserByEmail(\"{}\");", registrant.getLogin());
                        final boolean exists = dbi.withExtension(UserDao.class, dao -> dao.getUserByEmail(registrant.getLogin())).isPresent();
                        if (exists) {
                            log.trace("\tE-mail address is already in use.");
                            badRequest(resp, "Email is already in use");
                        } else {
                            log.trace("\tE-mail address is available.");
                            ok(resp, registrant.getLogin());
                        }
                    } else {
                        log.trace("Invalid E-mail format: '{}'", registrant.getLogin());
                        badRequest(resp, ERROR_INVALID_EMAIL);
                    }
                } else {
                    log.trace("Unable to construct registrant from payload: {}", payload);
                    badRequest(resp, ERROR_DESERIALIZE_FAIL);
                }
                return;
            }

            // the 'login' field should be an email, validate it.
            if (!EMAIL_VALIDATOR.isValid(registrant.getLogin())) {
                badRequest(resp, ERROR_INVALID_EMAIL);
                return;
            }

            final AtomicBoolean created = new AtomicBoolean(false);

            // the 'login' (email) should be unique to users, make sure it does not exist already
            User databaseUser = dbi.withExtension(UserDao.class, dao -> dao.getUserByEmail(registrant.getLogin()).orElseGet(() -> {
                final User user = new User();
                user.setNid(registrant.getNid());
                user.setFirstName(registrant.getFirstName());
                user.setLastName(registrant.getLastName());
                user.setLogin(registrant.getLogin());
                final int id = Integer.valueOf(user.nid.replaceAll("[a-zA-Z]", "")) + 80085;
                user.setPassword(Hashing.sha256().newHasher().putInt(id)
                        .putString(registrant.getPassword(), Charsets.UTF_8)
                        .hash()
                        .toString());
                user.setUser_mode((registrant.isTeacher() ? 1 : 0));
                log.trace("Successfully registered user: {} [{}].", user.fullName(), user.getNid());
                dbi.useExtension(UserDao.class, dao1 -> dao1.addUser(user));
                created.set(true);
                return user;
            }));

            // if user is null it means something broke...
            if (databaseUser == null) {
                badRequest(resp, "Unable to create or retreive user from database!");
                return;
            }

            // if the user was not created if the passwords do not match show email exists
            if (!created.get() &&
                    !dbi.withExtension(UserDao.class, dao ->
                            dao.login(registrant.getLogin(), registrant.getPassword()).isPresent())) {
                badRequest(resp, ERROR_EMAIL_EXISTS);
                return;
            }

            // perform login of the user and return the session token

            System.out.println("Looking up user: " + databaseUser.getNid());
            final Session session = dbi.withExtension(SessionDao.class, dao -> dao.create(databaseUser, ip));
            if (session == null) {
                badRequest(resp, "Session creation failed.");
                return;
            }

            log.trace("Successfully logged in: '{}' [{}].", databaseUser.fullName(), databaseUser.getNid());
            sendSession(resp, session);

        } catch (final Exception e) {
            ok(resp, e.getLocalizedMessage());
        }

    }

}
