package org.mahabal.optigrader.api.handler;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jdbi.v3.core.Jdbi;
import org.mahabal.optigrader.api.dao.SessionDao;
import org.mahabal.optigrader.api.dao.SubmissionDao;
import org.mahabal.optigrader.api.dao.TestDao;
import org.mahabal.optigrader.api.dao.UserDao;
import org.mahabal.optigrader.api.model.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Matthew
 */
public class AdminHandler extends AbstractHandler {

    public AdminHandler(final Jdbi dbi) {
        super(dbi, "AdminHandler");
    }

    protected void handleRequest(HttpServletRequest req, HttpServletResponse resp, String ip, JsonElement payload) throws Exception {

        try {

            final JsonObject json = payload.getAsJsonObject();
            if (!json.has("action")) {
                badRequest(resp, "no action specified");
                return;
            }

            final JsonObject response = new JsonObject();

            switch (json.get("action").getAsString()) {

                case "userCounts":
                    final JsonObject counts = new JsonObject();
                    counts.addProperty("total", dbi.withExtension(UserDao.class, UserDao::totalCount));
                    counts.addProperty("students", dbi.withExtension(UserDao.class, UserDao::studentCount));
                    counts.addProperty("teachers", dbi.withExtension(UserDao.class, UserDao::teacherCount));
                    counts.addProperty("dailyActiveTotal", dbi.withExtension(UserDao.class, UserDao::dailyActiveUsers));
                    response.add("counts", counts);
                    break;

                case "fullWipe":
                    this.dropAllTables();
                    this.createTables();
//                    this.createFakeTeacher();
//                    this.createFakeStudent();
                    response.addProperty("status", "success");
                    break;

                case "listUsers":
                    final JsonArray array = new JsonArray();
                    for (final User user : dbi.withExtension(UserDao.class, UserDao::allUsers)) {
                        final JsonObject usr = new JsonObject();
                        usr.addProperty("nid", user.getNid());
                        usr.addProperty("firstName", user.getFirstName());
                        usr.addProperty("lastName", user.getLastName());
                        usr.addProperty("login", user.getLogin());
                        array.add(usr);
                    }
                    response.addProperty("userCount", array.size());
                    response.add("users", array);
                    break;

                default:
                    break;

            }

            ok(resp, response);

        } catch (final Throwable t) {
            error(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, t.getMessage());
        }

    }

    public void dropAllTables() {
        dbi.useExtension(SubmissionDao.class, SubmissionDao::dropTable);
        dbi.useExtension(TestDao.class, TestDao::dropTable);
        dbi.useExtension(SessionDao.class, SessionDao::dropTable);
        dbi.useExtension(UserDao.class, UserDao::dropTable);
    }

    public void createTables() {
        dbi.useExtension(UserDao.class, UserDao::createTable);
        dbi.useExtension(SessionDao.class, SessionDao::createTable);
        dbi.useExtension(TestDao.class, TestDao::createTable);
        dbi.useExtension(SubmissionDao.class, SubmissionDao::createTable);
    }

    public void createFakeStudent() {

        final User student = new User();
        student.nid = "jo000001";
        student.firstName = "John";
        student.lastName = "Smith";
        student.user_mode = 0;
        student.login = "john.smith@gmail.com";
        student.password = Hashing.sha256().newHasher().putString("Password1", Charsets.UTF_8).hash().toString();
        dbi.useExtension(UserDao.class, dao -> dao.addUser(student));

    }

    public void createFakeTeacher() {

        final User teacher = new User();
        teacher.nid = "te000001";
        teacher.firstName = "Teacher";
        teacher.lastName = "Teachington";
        teacher.user_mode = 1;
        teacher.login = "teacher@gmail.com";
        teacher.password = Hashing.sha256().newHasher().putString("Password1", Charsets.UTF_8).hash().toString();
        dbi.useExtension(UserDao.class, dao -> dao.addUser(teacher));

    }

}
