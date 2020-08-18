alter table tm_text_unit_current_variant add column asset_id bigint;
alter table tm_text_unit_current_variant_aud add column asset_id bigint;
alter table tm_text_unit_current_variant add constraint FK__TM_TEXT_UNIT_CURRENT_VARIANT__ASSET__ID foreign key (asset_id) references asset (id);
