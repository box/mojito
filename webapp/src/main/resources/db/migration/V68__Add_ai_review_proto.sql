create table ai_review_proto (id bigint not null auto_increment, created_date datetime, last_modified_date datetime, json_review JSON, tm_text_unit_variant_id bigint, run_name varchar(32), primary key (id));
alter table ai_review_proto add constraint UK__AI_REVIEW_PROTO__RUN_NAME__TM_TEXT_UNIT_VARIANT_ID unique (run_name, tm_text_unit_variant_id);
alter table ai_review_proto add constraint FK__AI_REVIEW_PROTO__TM_TEXT_UNIT_VARIANT__ID foreign key (tm_text_unit_variant_id) references tm_text_unit_variant (id);

