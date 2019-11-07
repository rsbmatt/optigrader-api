package org.mahabal.optigrader.api.dao;

import com.google.common.base.Charsets;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindFields;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.stringtemplate4.UseStringTemplateSqlLocator;
import org.mahabal.optigrader.api.model.Session;
import org.mahabal.optigrader.api.model.User;

import java.util.List;
import java.util.Optional;

/**
 * @author Matthew
 */
@UseStringTemplateSqlLocator
public interface SessionDao {

    //TODO: Move this to string template
    @SqlQuery
    @RegisterBeanMapper(Session.class)
    Session getSessionByNid(@Bind("nid") final String nid, @Bind("ip") final String ip);

    @SqlQuery
    @RegisterBeanMapper(Session.class)
    Optional<Session> getSessionByToken(@Bind("token") final String token, @Bind("ip") final String ip);

    //TODO: Move this to string template
    @SqlUpdate
    void insertSession(@BindFields final Session session);

    @SqlQuery
    List<String> getAllNids();

    /**
     * Creates a session for the provided user
     *
     * @param user the user to create the session for, the userId is part of the token
     * @param ip   the IP of the user creating the session
     * @return a {@link Session} object if no issues are encountered
     * <tt>null</tt> if there is some issue
     */
    default Session create(final User user, final String ip) {

        // make sure this is a real user
        final String nid = user.getNid();
        if (nid == null || nid.isEmpty()) return null;

        // lookup session table
        Session session = getSessionByNid(nid, ip);
        // if a session already exists for this user... just return it
        if (session != null) return session;


        final HashFunction function = Hashing.sha256();
        final String token = function.newHasher()
                .putString(nid, Charsets.UTF_8)
                .putUnencodedChars(ip)
                .hash()
                .toString();

        // no session was found for the userId and ip, so create a new one
        session = new Session();
        session.setIp(ip);
        session.setToken(token);
        session.setNid(nid);
        insertSession(session);
        return session;

    }

    default boolean validateSession(final String token, final String ip) {
        return getSessionByToken(token, ip).isPresent();
    }

    @SqlUpdate
    void createTable();

    @SqlUpdate
    void dropTable();

}
