import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.hash.Hashing;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.validator.routines.EmailValidator;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.*;
import org.mahabal.optigrader.api.handler.RegistrationHandler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;

/**
 * @author Matthew
 */
@DisplayName("User Registration Tests")
public class RegistrationTest {

    private static final EmailValidator EMAIL_VALIDATOR = EmailValidator.getInstance();
    private static final List<String> FIRST_NAMES = Lists.newArrayList();
    private static final List<String> LAST_NAMES = Lists.newArrayList();

    private static final String DEFAULT_USER_PASSWORD = "test";

    /**
     * Populate the list of first names and last names
     */
    @BeforeAll
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

    /**
     * Attempts to submit a registration to the API with the provided information
     *
     * @param firstName     user's first name
     * @param lastName      user's last name
     * @param login         user's email
     * @param password      user's hashed password
     * @return      the JSON payload as a string
     */
    private static JsonObject submitRegistration(final String firstName, final String lastName,
                                           final String login, final String password) {
        final JsonObject payload = new JsonObject();
        payload.addProperty("firstName", firstName);
        payload.addProperty("lastName", lastName);
        payload.addProperty("login", login);
        payload.addProperty("password", password);
        try {
            final Document document = Jsoup.connect("http://localhost:8080/register")
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

    /**
     * Local test to make sure lists get populated properly
     */
    @Test
    @DisplayName("Ensure name lists were populated")
    public void randomUserGeneration() {
        Assertions.assertFalse(FIRST_NAMES.isEmpty(), "list of first names was empty");
        Assertions.assertFalse(LAST_NAMES.isEmpty(), "list of last names was empty");
        final String firstName = getRandomEntry(FIRST_NAMES);
        Assertions.assertNotNull(firstName, "first name was null");
        final String lastName = getRandomEntry(LAST_NAMES);
        Assertions.assertNotNull(lastName, "last name was null");
    }

    /**
     * Local test to make sure email generation is correct
     */
    @Test
    @DisplayName("Test valid email generation")
    public void randomEmailGeneration() {
        final String firstName = getRandomEntry(FIRST_NAMES);
        final String lastName = getRandomEntry(LAST_NAMES);
        final String email = generateEmail(firstName, lastName);
        Assertions.assertNotNull(email, "email generation failed");
        Assertions.assertTrue(EMAIL_VALIDATOR.isValid(email), "email generated was not valid: " + email);
    }

    /**
     * Create a correct registration request
     */
    @RepeatedTest(10)
    @DisplayName("Submit successful registration")
    public void submitSuccessfulRegistration() {
        final String firstName = getRandomEntry(FIRST_NAMES);
        final String lastName = getRandomEntry(LAST_NAMES);
        final String email = generateEmail(firstName, lastName);
        final String password = Hashing.sha256().newHasher().putString(DEFAULT_USER_PASSWORD, Charsets.UTF_8)
                .hash().toString();
        final JsonObject response = submitRegistration(firstName, lastName, email, password);
        Assertions.assertTrue(response.has("token"), "key: \"token\" not present in response JSON object");
        Assertions.assertEquals(64, response.get("token").getAsString().length(), "token is not 64 characters");
    }

    /**
     * Create an invalid registration request that should return the error for "invalid email"
     */
    @RepeatedTest(10)
    @DisplayName("Submit invalid email registration")
    public void submitInvalidEmailRegistration() {
        final String firstName = getRandomEntry(FIRST_NAMES);
        final String lastName = getRandomEntry(LAST_NAMES);
        final String email = "86teg76531d782g";
        final String password = Hashing.sha256().newHasher().putString(DEFAULT_USER_PASSWORD, Charsets.UTF_8)
                .hash().toString();
        final JsonObject response = submitRegistration(firstName, lastName, email, password);
        Assertions.assertTrue(response.has("error"), "JSON payload did not contain key \"error\".");
        Assertions.assertEquals(RegistrationHandler.ERROR_INVALID_EMAIL,
                response.get("error").getAsString(), "did not receive invalid e-mail message");
    }

}
