import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.validator.routines.EmailValidator;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import dev.mahabal.optigrader.api.dao.SessionDao;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Tests the login handler of the API
 *
 * @author Matthew
 */
@DisplayName("User Login Tests")
public class LoginTest {

    private static final EmailValidator EMAIL_VALIDATOR = EmailValidator.getInstance();
    private static final String DATABASE_USERNAME = "root";
    private static final String DATABASE_PASSWORD = "notgonnahappen!";
    private static final String DATABASE_NAME = "optigrader";

    private static final String DEFAULT_USER_PASSWORD = "test";
    private static String HASHED_PASSWORD = null;

    private static Jdbi dbi;

    /**
     * Populate the list of first names and last names
     */
    @BeforeAll
    public static void loadNames() {

        HASHED_PASSWORD = Hashing.sha256().newHasher().putString(DEFAULT_USER_PASSWORD, Charsets.UTF_8)
                .hash().toString();
        Assertions.assertNotNull(HASHED_PASSWORD, "Hashing of default password failed.");

        final HikariDataSource ds = new HikariDataSource();
        // this has to be localhost if running locally.
        ds.setJdbcUrl("jdbc:mysql://mariadb:3306/" + DATABASE_NAME);
        ds.setUsername(DATABASE_USERNAME);
        ds.setPassword(DATABASE_PASSWORD);

        // create the access point for JDBI and set the data source
        dbi = Jdbi.create(ds);
        dbi.installPlugin(new SqlObjectPlugin());
    }

    /**
     * Attempts to submit a login to the API with the provided information

     * @return      the JSON payload as a string
     */
    private static JsonObject submitLogin(final String login, final String password) {
        final JsonObject payload = new JsonObject();
        payload.addProperty("login", login);
        payload.addProperty("password", password);
        try {
            final Document document = Jsoup.connect("http://localhost:8080/login")
                    .requestBody(payload.toString())
                    .ignoreContentType(true)
                    .ignoreHttpErrors(true)
                    .post();
            //ensure that the response is not null
            final String response = document.text();
            Assertions.assertNotNull(response, "API response was a null string");
            // ensure that the response is  json object
            final JsonObject object = new JsonParser().parse(response).getAsJsonObject();
            Assertions.assertNotNull(object, "API response was not a valid JSON object.");
            return object;
        } catch (IOException e) {
            e.printStackTrace();
        }
        Assertions.fail("unable to connect to API.");
        return new JsonObject();

    }

    @RepeatedTest(10)
    @DisplayName("Logging into active sessions with default password")
    void login() {
        final List<String> nids = dbi.withExtension(SessionDao.class, SessionDao::getAllNids);
        Assertions.assertFalse(nids.isEmpty(), "Could not retreive active NIDs from the database.");
        Collections.shuffle(nids);
        final String nid = nids.get(0);
        final JsonObject response = submitLogin(nid, HASHED_PASSWORD);
        Assertions.assertTrue(response.has("token"), "Key: \"token\" not present in JSON payload.");
        Assertions.assertEquals(response.get("token").getAsString().length(), 64, "Invalid token length.");

    }

    @RepeatedTest(10)
    @DisplayName("Logging into active sessions with incorrect password")
    void badLogin() {
        final List<String> nids = dbi.withExtension(SessionDao.class, SessionDao::getAllNids);
        Assertions.assertFalse(nids.isEmpty(), "Could not retreive active NIDs from the database.");
        Collections.shuffle(nids);
        final String nid = nids.get(0);
        final JsonObject response = submitLogin(nid,
                Hashing.sha256().newHasher().putLong(System.currentTimeMillis()).hash().toString());
        Assertions.assertTrue(response.has("error"), "Key: \"error\" not present in JSON payload.");
        Assertions.assertEquals(response.get("error").getAsString(), "Invalid username/password combination.", "Was not an invalid username/password combination.");

    }

}
