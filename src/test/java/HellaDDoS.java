import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.hash.Hashing;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Run this to test the API in a deployed instance
 */
public class HellaDDoS {


    private static final Map<String, String> SESSIONS = Maps.newHashMap();
    private static final List<String> FIRST_NAMES = Lists.newArrayList();
    private static final List<String> LAST_NAMES = Lists.newArrayList();

    private static final String DEFAULT_USER_PASSWORD = "Password1";

    public static void loadNames() {
        try {
            Path path = Paths.get(RegistrationTest.class.getResource("first.txt").toURI());
            if (Files.exists(path)) FIRST_NAMES.addAll(Files.readAllLines(path));
            path = Paths.get(RegistrationTest.class.getResource("last.txt").toURI());
            if (Files.exists(path)) LAST_NAMES.addAll(Files.readAllLines(path));
        } catch (final Exception ignored) {
        }
    }

    private static String getRandomEntry(final List<String> list) {
        final String string = list.get(new Random().nextInt(list.size()));
        return string.substring(0, 1).toUpperCase() + string.substring(1).toLowerCase();
    }

    /**
     * Generates a pseudorandom email based on the passed in User's name
     *
     * @param firstName     the first name of the user
     * @param lastName      the last name of the user
     * @return          a pseudo-random email
     */
    private static String generateEmail(final String firstName, final String lastName) {
        final boolean lastNameFirst = new Random().nextInt(99) % 2 == 0;
        final boolean usePeriod = new Random().nextInt(99) % 3 == 0;
        final boolean addYear = new Random().nextInt(90) % 2 == 0;
        String email = "";
        email += (lastNameFirst ? lastName : firstName);
        email += usePeriod ? "." : "";
        email += lastNameFirst ? firstName : lastName;
        email += addYear ? new Random().nextInt(9) + 90 : "";
        email += "@knights.ucf.edu";
        return email.toLowerCase();
    }

    public static void main(String[] args) {

        final String HASHED_PASSWORD = Hashing.sha256().newHasher().putString(DEFAULT_USER_PASSWORD, Charsets.UTF_8)
                .hash().toString();

        System.out.println(HASHED_PASSWORD);
        loadNames();

        final JsonObject teacherResponse = submitRegistration("Teacher", "Teachington", "te@cher.com", HASHED_PASSWORD, true);
        SESSIONS.put("te@cher.com", teacherResponse.get("token").getAsString());


        for (int i = 0; i < 10; ++i) {

            final String firstName = getRandomEntry(FIRST_NAMES);
            final String lastName = getRandomEntry(LAST_NAMES);
            final String email = generateEmail(firstName, lastName);

            final JsonObject response = submitRegistration(firstName, lastName, email, HASHED_PASSWORD, false);
            if (response.isJsonObject() && response.has("token")) {
                SESSIONS.put(email, response.get("token").getAsString());
                System.out.println("Created user: " + email);
            }

        }

        // Create 3 exams
        for (int i = 0; i < 3; ++i) {

            final JsonObject payload = new JsonObject();
            payload.addProperty("token", SESSIONS.get("te@cher.com"));

        }

    }

    static String TEACHER_LOGIN = null;

    private static JsonObject submitRegistration(final String firstName, final String lastName,
                                           final String login, final String password, final boolean teacher) {
        final JsonObject payload = new JsonObject();
        payload.addProperty("firstName", firstName);
        payload.addProperty("lastName", lastName);
        payload.addProperty("login", login);
        payload.addProperty("password", password);
        payload.addProperty("teacher", teacher);
        try {
            final Document document = Jsoup.connect("https://optigrader.mahabal.org:8080/register")
                    .requestBody(payload.toString())
                    .ignoreContentType(true)
                    .ignoreHttpErrors(true)
                    .post();
            //ensure that the response is not null
            final String response = document.text();
            // ensure that the response is  json object
            final JsonObject object = new JsonParser().parse(response).getAsJsonObject();
            return object;
        } catch (final Exception e) {
            return new JsonObject();
        }
    }


}
