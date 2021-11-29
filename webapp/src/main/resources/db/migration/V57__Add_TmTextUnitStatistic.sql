-- New table to store text unit usage statistics.
-- Note: v56 is a JAVA-based migration, found here: db/migration/V56__TUCVAddAssetIdUpdater.java

CREATE TABLE tm_text_unit_statistic (
    id bigint(20) NOT NULL AUTO_INCREMENT,
    created_date datetime DEFAULT NULL,
    last_modified_date datetime DEFAULT NULL,
    last_day_usage_count double precision DEFAULT NULL,
    last_period_usage_count double precision DEFAULT NULL,
    last_seen_date datetime DEFAULT NULL,
    tm_text_unit_id bigint NOT NULL,
    primary key (id)
);

ALTER TABLE tm_text_unit_statistic ADD CONSTRAINT UK__TM_TEXT_UNIT_STATISTIC__BRANCH_ID UNIQUE (tm_text_unit_id);
ALTER TABLE tm_text_unit_statistic ADD CONSTRAINT FK__TM_TEXT_UNIT_STATISTIC__BRANCH_ID FOREIGN KEY (tm_text_unit_id) REFERENCES tm_text_unit (id);
