package db.migration;

import com.box.l10n.mojito.service.WordCountService;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 *
 * @author jaurambault
 */
public class V9__Compute_Word_Count { // implements SpringJdbcMigration {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(V9__Compute_Word_Count.class);

    static final int MAX_ROW = 500;
    
    WordCountService  wordCountService = new WordCountService();
        
//    @Override
    public void migrate(JdbcTemplate jdbcTemplate) throws Exception {
       
        logger.info("Update word count");

        List<TmTextUnit> tmTextUnits;

        do {
            tmTextUnits = getTmTextUnits(jdbcTemplate);
            updateTmTextUnits(jdbcTemplate, tmTextUnits);
        } while (!tmTextUnits.isEmpty());

    }

    private List<TmTextUnit> getTmTextUnits(JdbcTemplate jdbcTemplate) {
     
        jdbcTemplate.setMaxRows(MAX_ROW);
        
        List<TmTextUnit> tmTextUnits = jdbcTemplate.query(
                "select id, content from tm_text_unit tu where tu.word_count is null",
                new RowMapper<TmTextUnit>() {
                    
            public TmTextUnit mapRow(ResultSet rs, int rowNum) throws SQLException {
                TmTextUnit tmTextUnit = new TmTextUnit();
                tmTextUnit.id = rs.getLong("id");
                tmTextUnit.content = rs.getString("content");
                return tmTextUnit;
            }
        });

        return tmTextUnits;
    }

    private void updateTmTextUnits(JdbcTemplate jdbcTemplate, final List<TmTextUnit> tmTextUnits) {
       int[] updated = jdbcTemplate.batchUpdate("update tm_text_unit set word_count = ? where id = ?",
            new BatchPreparedStatementSetter() {
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                        ps.setInt(1, wordCountService.getEnglishWordCount(tmTextUnits.get(i).content));
                        ps.setLong(2, tmTextUnits.get(i).id);
                    }

                    public int getBatchSize() {
                        return tmTextUnits.size();
                    }
                });
       
       logger.info("TmTextUnit update count: {}", updated.length);
    }

    class TmTextUnit {
        Long id;
        String content;
    }

}
