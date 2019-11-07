package dev.mahabal.optigrader.api.dao;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import dev.mahabal.optigrader.api.model.User;
import org.apache.commons.validator.EmailValidator;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindFields;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transaction;
import org.jdbi.v3.stringtemplate4.UseStringTemplateSqlLocator;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * @author Matthew
 */
@UseStringTemplateSqlLocator
public interface UserDao {

    /**
     * Searches the users table for any user with the provided email address. It
     * will construct a user object from the first resulting row and return it
     *
     * @param email the email to lookup
     * @return A {@link User} object if one exists with the provided email
     * <tt>null</tt> if no user is found
     */
    @SqlQuery
    @RegisterBeanMapper(User.class)
    Optional<User> getUserByEmail(@Bind("email") final String email);

    /**
     * Searches the users table for any user with the provided nid address. It
     * will construct a user object from the first resulting row and return it
     *
     * @param nid the nid to lookup
     * @return A {@link User} object if one exists with the provided nid
     * <tt>null</tt> if no user is found
     */
    @SqlQuery
    @RegisterBeanMapper(User.class)
    Optional<User> getUserByNid(@Bind("nid") final String nid);

    /**
     * Searches the users table for any user with the provided email address
     *
     * @param email     the email to search for
     * @return          <tt>true</tt> if there is at least one match
     *                  <tt>false</tt> if no matches found
     */
    default boolean lookupEmail(final String email) {
        return getUserByEmail(email).isPresent();
    }

    /**
     * Inserts the user into the database
     *
     * @param user the user to insert into the database
     * @return the created ID (auto-increment) of the user
     */
    @SqlUpdate
    void addUser(@BindFields final User user);

    Predicate<String> NID_PATTERN = Pattern.compile("[a-z]([a-z])?[0-9]{1,6}").asMatchPredicate();
    EmailValidator EMAIL_VALIDATOR = EmailValidator.getInstance();

    /**
     * Looks up into the database for an matching email and password pair. If one is found, it will return
     * the {@link User} object. If not, it will return <tt>null</tt>.
     *
     * @param email    the email to lookup
     * @param password the password corresponding to the email
     * @return User if a matching pair is found
     * <tt>null</tt> if no matching user found
     */
    //TODO: Verify this SQL
    @SqlQuery
    @RegisterBeanMapper(User.class)
    Optional<User> validateEmailLogin(@Bind("email") final String email, @Bind("password") final String password);

    /**
     * Looks up into the database for an matching NID and password pair. If one is found, it will return
     * the {@link User} object. If not, it will return <tt>null</tt>.
     *
     * @param nid      the NID to lookup
     * @param password the password corresponding to the NID
     * @return User if a matching pair is found
     * <tt>null</tt> if no matching user found
     */
    //TODO: Verify this SQL
    @SqlQuery
    @RegisterBeanMapper(User.class)
    Optional<User> validateNidLogin(@Bind("nid") final String nid, @Bind("password") final String password);

    @SqlQuery
    Optional<String> nidForEmail(@Bind("email") final String email);

    @SqlQuery
    @RegisterBeanMapper(User.class)
    List<User> allUsers();

    @SqlQuery
    int totalCount();

    @SqlQuery
    int studentCount();

    @SqlQuery
    int teacherCount();

    @SqlQuery
    int dailyActiveUsers();

    /**
     * Updates the last logged in time for the specific user. Meant to be called EVERY time
     * that {@link #validateEmailLogin(String, String)} is called.
     *
     * @param user  the user that successfully logged in
     */
    //TODO: Add this SQL
    @SqlUpdate
    void loggedIn(@BindFields final User user);

    /**
     * Performs a login
     *
     * @param email     email to attempt to login with
     * @param password  password to attempt to login with
     *
     * @return A {@link User} object on successful login
     *                  <tt>null</tt> if login fails
     */
    @Transaction
    default Optional<User> loginWithEmail(final String email, final String password) {

        final Optional<User> user = validateEmailLogin(email, password);
        user.ifPresent(this::loggedIn);
        return user;

    }

    /**
     * Performs a login
     *
     * @param login    email to attempt to login with
     * @param password password to attempt to login with
     * @return A {@link User} object on successful login
     * <tt>null</tt> if login fails
     */
    @Transaction
    default Optional<User> login(final String login, final String password) {

        final Optional<User> user;
        if (EMAIL_VALIDATOR.isValid(login)) {
            final String nid = nidForEmail(login).orElse(null);
            if (nid == null) {
                return Optional.empty();
            }
            // salty 80085
            final int id = Integer.parseInt(nid.replaceAll("[a-zA-Z]", "")) + 80085;
            user = validateEmailLogin(login,
                    Hashing.sha256().newHasher().putInt(id).putString(password, Charsets.UTF_8)
                            .hash()
                            .toString());
        } else if (NID_PATTERN.test(login)) {
            final int id = Integer.parseInt(login.replaceAll("[a-zA-Z]", "")) + 80085;
            user = validateNidLogin(login,
                    Hashing.sha256().newHasher().putInt(id).putString(password, Charsets.UTF_8)
                            .hash()
                            .toString());
        } else {
            return Optional.empty();
        }
        user.ifPresent(this::loggedIn);
        return user;

    }


    /**
     * Creates the user table if it does not already exist
     */
    @SqlUpdate
    void createTable();

    /**
     * Drops the User table
     */
    @SqlUpdate
    void dropTable();

}
