package com.box.l10n.mojito.service.security.user.session;

import com.box.l10n.mojito.service.DBUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.session.jdbc.config.annotation.web.http.JdbcHttpSessionConfiguration;
import org.springframework.util.StringUtils;


/**
 * Workaround for issue <a href="https://github.com/spring-projects/spring-session/issues/1213">Concurrent requests adding the same session attribute result in duplicate key constraint violation</a>
 * to change the Create Session Attribute query to use an MySQL UPDATE command to update the ATTRIBUTE_BYTES
 * of the existing row if a duplicate key is encountered.
 *
 * <b>NOTE:</b>This custom insert query should only be used with a MySQL database.
 */
@Configuration
@ConditionalOnProperty(value = "l10n.spring.session.use-custom-mysql-create-session-attribute-query", havingValue = "true")
public class CustomCreateSessionAttributeInsertQueryConfiguration extends JdbcHttpSessionConfiguration {

    static Logger logger = LoggerFactory.getLogger(CustomCreateSessionAttributeInsertQueryConfiguration.class);

    private static final String CREATE_SESSION_ATTRIBUTE_QUERY_ON_DUPLICATE_KEY_UPDATE =
        "INSERT INTO %TABLE_NAME%_ATTRIBUTES(SESSION_PRIMARY_ID, ATTRIBUTE_NAME,  ATTRIBUTE_BYTES) "
                + "SELECT PRIMARY_ID, ?, ? "
                + "FROM %TABLE_NAME% "
                + "WHERE SESSION_ID = ? ON DUPLICATE KEY UPDATE ATTRIBUTE_BYTES=VALUES(ATTRIBUTE_BYTES)";

    @Autowired
    DBUtils dbUtils;

    @Value("${spring.session.jdbc.table-name}")
    private String customTableName;

    @Bean
    @Override
    public JdbcIndexedSessionRepository sessionRepository() {
        logger.debug("Setting Spring Session custom session attribute query.");
        JdbcIndexedSessionRepository sessionRepository = super.sessionRepository();
        sessionRepository.setTableName(getTableName());
        updateCreateSessionAttributeQuery(sessionRepository);
        return sessionRepository;
    }

    private void updateCreateSessionAttributeQuery(JdbcIndexedSessionRepository sessionRepository) {
        logger.debug("Updating the Create Session Attribute query");
        if (dbUtils.isMysql()) {
            sessionRepository.setCreateSessionAttributeQuery(getCustomCreateSessionAttributeQuery());
        } else {
            logger.warn("The database is not MySQL, skipping query update.");
        }
    }

    private String getCustomCreateSessionAttributeQuery() {
        return StringUtils.replace(
                CREATE_SESSION_ATTRIBUTE_QUERY_ON_DUPLICATE_KEY_UPDATE,
                "%TABLE_NAME%",
                getTableName());
    }

    private String getTableName() {
        return customTableName != null ? customTableName : JdbcIndexedSessionRepository.DEFAULT_TABLE_NAME;
    }

}
