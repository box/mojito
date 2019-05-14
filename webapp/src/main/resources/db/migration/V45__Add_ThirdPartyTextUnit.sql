create table third_party_text_unit (id bigint not null auto_increment, created_date datetime, last_modified_date datetime, mapping_key longtext, third_party_text_unit_id varchar(255), tm_text_unit_id bigint, primary key (id));
alter table third_party_text_unit add constraint UK__THIRD_PARTY_TEXT_UNIT__THIRD_PARTY_TEXT_UNIT_ID unique (third_party_text_unit_id);
alter table third_party_text_unit add constraint FK__THIRD_PARTY_TEXT_UNIT__TM_TEXT_UNIT__ID foreign key (tm_text_unit_id) references tm_text_unit (id);
