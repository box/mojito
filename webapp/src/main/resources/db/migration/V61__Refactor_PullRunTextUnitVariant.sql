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

-- assume data has been removed first from pull_run_text_unit_variant
alter table pull_run_text_unit_variant add column locale_id bigint not null;
alter table pull_run_text_unit_variant add constraint UK__PULL_RUN_TEXT_UNIT_VARIANT__LOCALE_ID__PRA_ID__TUV_ID unique (pull_run_asset_id, locale_id, tm_text_unit_variant_id);
alter table pull_run_text_unit_variant add constraint FK__PULL_RUN_TEXT_UNIT_VARIANT__LOCALE__ID foreign key (locale_id) references locale (id);
