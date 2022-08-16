package com.box.l10n.mojito.service.pullrun;

import com.google.common.collect.Lists;
import com.p6spy.engine.spy.P6SpyOptions;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

public class PullRunAssetServiceTest {

    static Logger logger = LoggerFactory.getLogger(PullRunAssetServiceTest.class);

//    @Test
    public void test() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.jdbc.Driver");
//        dataSource.setDriverClassName("com.p6spy.engine.spy.P6SpyDriver");
        dataSource.setUrl("jdbc:p6spy:mysql://localhost:3306/mojito_dev");
        dataSource.setUsername("mojito");
        dataSource.setPassword("mojito");

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        DataSourceTransactionManager dataSourceTransactionManager = new DataSourceTransactionManager(dataSource);

        TransactionTemplate transactionTemplate = new TransactionTemplate();
        transactionTemplate.setTransactionManager(dataSourceTransactionManager);

        int LIMIT = 1000000;

        IntStream.range(0, 5).forEach(i -> {
            List<Long> tmTextUnitVariantIds = withTime("select ids", () -> {
                MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();
                return jdbcTemplate.queryForList("select id from tm_text_unit_variant limit " + LIMIT, Long.class);
            });

            System.out.printf("size: %s\n", tmTextUnitVariantIds.size());
            withTimeInsertSeq("total", tmTextUnitVariantIds.size(), () -> {
                AtomicInteger j = new AtomicInteger();
                int batchSize = 1000;
                Lists.partition(tmTextUnitVariantIds, batchSize).stream().forEach(ids -> {
//                    withTimeInsertSeq(String.format("insert batch %d", j.getAndIncrement()), batchSize, () -> {
//                        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
//                            @Override
//                            protected void doInTransactionWithoutResult(TransactionStatus status) {
//                                jdbcTemplate.batchUpdate("insert into pull_run_text_unit_variant(pull_run_asset_id, tm_text_unit_variant_id) values (?, ?)", new BatchPreparedStatementSetter() {
//                                    @Override
//                                    public void setValues(PreparedStatement ps, int i) throws SQLException {
//                                        ps.setLong(1, 1L);
//                                        ps.setLong(2, ids.get(i));
//                                    }
//
//                                    @Override
//                                    public int getBatchSize() {
//                                        return ids.size();
//                                    }
//                                });
//                            }
//                        });
//                        return null;
//                    });
                    withTimeInsertSeq(String.format("insert batch %d", j.getAndIncrement()), batchSize, () -> {
                        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                            @Override
                            protected void doInTransactionWithoutResult(TransactionStatus status) {
                                String sql = "insert into pull_run_text_unit_variant(pull_run_asset_id, tm_text_unit_variant_id) values "
                                        + ids.stream().map(id -> String.format("(%s, %s) ", "1", id)).collect(Collectors.joining(","));
                                jdbcTemplate.execute(sql);
                            }
                        });
                        return null;
                    });
                });
                return null;
            });
        });
    }

    private List<Long> withTime(String select_ids, Object o) {
        return null;
    }


    static <T> T withTime(String text, Supplier<T> supplier) {
        long start = System.nanoTime();
        T t = supplier.get();
        double elapsed = (System.nanoTime() - start) / 1000000000.0;
        System.out.printf("%s - time in seconds: %s\n", text, elapsed);
        return t;
    }

    static <T> T withTimeInsertSeq(String text, int numInsert, Supplier<T> supplier) {
        long start = System.nanoTime();
        T t = supplier.get();
        double elapsed = (System.nanoTime() - start) / 1000000000.0;
        System.out.printf("%s - time in seconds: %s; insert/s: %s \n", text, elapsed, numInsert / elapsed);
        return t;
    }
}