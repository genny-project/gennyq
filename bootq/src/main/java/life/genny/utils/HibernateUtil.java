package life.genny.utils;


import java.util.Optional;
import java.util.Properties;
import javax.persistence.criteria.CriteriaBuilder;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;

@SuppressWarnings("unused")
public class HibernateUtil {

    private static final SessionFactory SESSION_FACTORY;
    private static final String FULL_MYSQL_URL;
    private static final String MYSQL_USER;
    private static final String MYSQL_PASSWORD;

    static {

        Optional<String> fullMysqlUrl =
                Optional.ofNullable(System.getenv("FULL_MYSQL_URL"));
        Optional<String> mysqlUser =
                Optional.ofNullable(System.getenv("MYSQL_USER"));
        Optional<String> mysqlPassword =
                Optional.ofNullable(System.getenv("MYSQL_PASSWORD"));

        FULL_MYSQL_URL =
                fullMysqlUrl.orElse("jdbc:mysql://127.0.0.1:3310/gennydb");
        MYSQL_USER = mysqlUser.orElse("genny");
        MYSQL_PASSWORD = mysqlPassword.orElse("password");

        Configuration configuration = new Configuration();
        configuration.setProperty("hibernate.connection.url",
                FULL_MYSQL_URL);
        configuration.setProperty("hibernate.connection.username",
                MYSQL_USER);
        configuration.setProperty("hibernate.connection.password",
                MYSQL_PASSWORD);

        configuration.setProperty("hibernate.connection.provider_class", "org.hibernate.connection.C3P0ConnectionProvider");
        configuration.setProperty("hibernate.c3p0.max_size", "100");
        configuration.setProperty("hibernate.c3p0.min_size", "20");
        configuration.setProperty("hibernate.c3p0.timeout", "600");
        configuration.setProperty("hibernate.c3p0.max_statements", "75");
        configuration.setProperty("hibernate.c3p0.idle_test_period", "5000");
        configuration.setProperty("hibernate.c3p0.testConnectionOnCheckout", "true");

        configuration.setProperty("hibernate.connection.autocommit", "true");
        configuration.setProperty("hibernate.connection.pool_size", "100");

        configuration.setProperty("hibernate.jdbc.batch_size", "20");

        configuration.setProperty("hibernate.connection.release_mode", "on_close");
        configuration.setProperty("hibernate.connection.handling_mode", "DELAYED_ACQUISITION_AND_RELEASE_AFTER_TRANSACTION");

        configuration.setProperty("hibernate.max_fetch_depth", "3");
        configuration.setProperty("hibernate.order_updates", "true");
        configuration.setProperty("hibernate.connection.isolation", "2");
        configuration.setProperty("hibernate.order_inserts", "true");
        configuration.setProperty("current_session_context_class", "thread");

        configuration.setProperty("hibernate.discriminator.ignore_explicit_for_joined", "true");

        // not working

/*

 */

        System.out.println("MySQL details, FULL_MYSQL_URL:" + FULL_MYSQL_URL
                + ", MYSQL_USER:" + MYSQL_USER + ",MYSQL_PASSWORD: " + MYSQL_PASSWORD);

        SESSION_FACTORY = configuration.configure().buildSessionFactory();
        CriteriaBuilder criteriaBuilder =
                SESSION_FACTORY.getSessionFactory().openSession().getSession()
                        .getCriteriaBuilder();

    }

    private HibernateUtil() {
    }

    public static SessionFactory getSessionFactory() {
        return SESSION_FACTORY;
    }

    public static void shutdown() {
        getSessionFactory().close();
    }

    private static SessionFactory buildSessionFactory() {
        try {
            Configuration configuration = new Configuration().configure();
            Properties properties = configuration.getProperties();
            ServiceRegistry serviceRegistry =
                    new StandardServiceRegistryBuilder()
                            .applySettings(properties).build();
            return configuration.buildSessionFactory(serviceRegistry);
        } catch (Throwable ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }
}
