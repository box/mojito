-- show index from pull_run_text_unit_variant;
-- show create table pull_run_text_unit_variant;

ALTER TABLE pull_run_text_unit_variant
    DROP FOREIGN KEY FK__PULL_RUN_TEXT_UNIT_VARIANT__PULL_RUN_ASSET_ID;

ALTER TABLE pull_run_text_unit_variant
    DROP FOREIGN KEY FK__PULL_RUN_TEXT_UNIT_VARIANT__TM_TEXT_UNIT_VARIANT_ID;

ALTER TABLE pull_run_text_unit_variant
    DROP INDEX UK__PULL_RUN_TEXT_UNIT_VARIANT__EVA_ID__TM_TUV_ID;

ALTER TABLE pull_run_text_unit_variant
    ADD CONSTRAINT FK__PULL_RUN_TEXT_UNIT_VARIANT__PULL_RUN_ASSET_ID FOREIGN KEY (pull_run_asset_id) REFERENCES pull_run_asset (id);
ALTER TABLE pull_run_text_unit_variant
    ADD CONSTRAINT FK__PULL_RUN_TEXT_UNIT_VARIANT__TM_TEXT_UNIT_VARIANT_ID FOREIGN KEY (tm_text_unit_variant_id) REFERENCES tm_text_unit_variant (id);
