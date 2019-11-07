package dev.mahabal.optigrader.api;

import com.zaxxer.hikari.HikariDataSource;
import dev.mahabal.optigrader.api.dao.TestDao;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import dev.mahabal.optigrader.api.dao.SessionDao;
import dev.mahabal.optigrader.api.dao.SubmissionDao;
import dev.mahabal.optigrader.api.dao.UserDao;
import dev.mahabal.optigrader.api.handler.AdminHandler;
import dev.mahabal.optigrader.api.handler.LoginHandler;
import dev.mahabal.optigrader.api.handler.RegistrationHandler;
import dev.mahabal.optigrader.api.handler.TestHandler;


/**
 * @author Matthew
 */
public class ApiServer {

    private static final String DATABASE_HOST = "localhost";
    private static final String TEST_DATABASE_HOST = "mariadb";   // just docker things
    private static final String DATABASE_USERNAME = "root";
    private static final String DATABASE_PASSWORD = "notgonnahappen!";
    private static final String DATABASE_NAME = "optigrader";
    private static Logger log = LogManager.getLogger("ApiServer");

    public final ServletHandler handler = new ServletHandler();

    public ApiServer(final boolean test) {

        Configurator.setAllLevels("main", Level.ALL);
        Configurator.setRootLevel(Level.ALL);

        log.info("Starting API Server...");

        // set the data source and create the connection
        final HikariDataSource ds = new HikariDataSource();
        final String connectionURI = "jdbc:mysql://" + (test ? TEST_DATABASE_HOST : DATABASE_HOST) + ":3306/" + DATABASE_NAME;
        ds.setJdbcUrl(connectionURI);
        log.info("Connecting to: " + connectionURI);
        ds.setUsername(DATABASE_USERNAME);
        ds.setPassword(DATABASE_PASSWORD);

        // create the access point for JDBI and set the data source
        final Jdbi dbi = Jdbi.create(ds);
        log.info("Successfully connected to database");
        dbi.installPlugin(new SqlObjectPlugin());

        // create tables
        dbi.useExtension(UserDao.class, UserDao::createTable);
        dbi.useExtension(SessionDao.class, SessionDao::createTable);
        dbi.useExtension(TestDao.class, TestDao::createTable);
        dbi.useExtension(SubmissionDao.class, SubmissionDao::createTable);

        handler.addServletWithMapping(new ServletHolder(new RegistrationHandler(dbi)), "/register");
        handler.addServletWithMapping(new ServletHolder(new LoginHandler(dbi)), "/login");
        handler.addServletWithMapping(new ServletHolder(new TestHandler(dbi)), "/test");
        handler.addServletWithMapping(new ServletHolder(new AdminHandler(dbi)), "/admin");

    }

    public static void main(String[] args) throws Exception {

        // create an instance of the Jetty server
        final Server server = new Server();

        boolean test = false;
        if (args.length == 1 && "test".equals(args[0])) {
            test = true;
        }

        final ServerConnector connector;

        if (test) {
            connector = new ServerConnector(server);
            connector.setPort(8080);
        } else {
            final HttpConfiguration https = new HttpConfiguration();
            https.addCustomizer(new SecureRequestCustomizer());

            final SslContextFactory sslContextFactory = new SslContextFactory();
            sslContextFactory.setKeyStorePath(ApiServer.class.getResource("/optigrader.p12").toExternalForm());
            sslContextFactory.setKeyStoreType("pkcs12");
            sslContextFactory.setKeyStorePassword("123456");
            sslContextFactory.setKeyManagerPassword("123456");

            connector = new ServerConnector(server,
                    new SslConnectionFactory(sslContextFactory, "http/1.1"),
                    new HttpConnectionFactory(https));
            connector.setPort(8080);
        }

        server.setConnectors(new Connector[]{connector});

        // create an instance of this ApiServer and set the server's handler
        final ApiServer api = new ApiServer(test);
        server.setHandler(api.handler);

        // start the server
        server.start();
        log.info("Server is now listening on port: 8080");
        server.join();

    }


}
