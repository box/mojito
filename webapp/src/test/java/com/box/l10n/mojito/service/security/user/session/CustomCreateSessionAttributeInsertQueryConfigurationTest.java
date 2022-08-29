package com.box.l10n.mojito.service.security.user.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.box.l10n.mojito.service.DBUtils;
import java.lang.reflect.Field;
import javax.sql.DataSource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.ReflectionUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(
    classes = {CustomCreateSessionAttributeInsertQueryConfiguration.class},
    properties = {
      "l10n.spring.session.use-custom-mysql-create-session-attribute-query=true",
      "spring.session.jdbc.table-name=test_table",
      "spring.datasource.url=jdbc:mysql:testDB"
    })
public class CustomCreateSessionAttributeInsertQueryConfigurationTest {

  @MockBean DataSource dataSourceMock;

  @MockBean PlatformTransactionManager platformTransactionManagerMock;

  @SpyBean DBUtils dbUtils;

  @Autowired JdbcIndexedSessionRepository jdbcIndexedSessionRepository;

  String requiredString =
      "INSERT INTO test_table_ATTRIBUTES (SESSION_PRIMARY_ID, ATTRIBUTE_NAME, ATTRIBUTE_BYTES) "
          + "VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE ATTRIBUTE_BYTES=VALUES(ATTRIBUTE_BYTES)";

  @Test
  public void testCustomSessionAttributeQueryIsSetOnSessionRepository()
      throws IllegalAccessException {
    assertNotNull(jdbcIndexedSessionRepository);
    Field createSessionAttributeQueryField =
        ReflectionUtils.findField(
            jdbcIndexedSessionRepository.getClass(), "createSessionAttributeQuery", String.class);
    createSessionAttributeQueryField.setAccessible(true);
    String actualQueryString =
        (String) createSessionAttributeQueryField.get(jdbcIndexedSessionRepository);
    verify(dbUtils, times(1)).isMysql();
    assertEquals(requiredString, actualQueryString);
  }
}
