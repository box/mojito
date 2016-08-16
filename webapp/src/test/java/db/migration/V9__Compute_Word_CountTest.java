package db.migration;

import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import javax.sql.DataSource;
import org.junit.Test;
import org.junit.Ignore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 *
 * @author jaurambault
 */
@Ignore("Test database migration")
public class V9__Compute_Word_CountTest extends ServiceTestBase {
    
    @Autowired
    DataSource dataSource;
    
    /**
     * Test of migrate method, of class V9__Compute_Word_Count.
     */
    @Test
    public void testMigrate() throws Exception {
        System.out.println("migrate");
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        V9__Compute_Word_Count instance = new V9__Compute_Word_Count();
        instance.migrate(jdbcTemplate);       
    }
    
}
