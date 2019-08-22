create table third_party_screenshot (id bigint not null auto_increment, created_date datetime, last_modified_date datetime, third_party_id varchar(255), screenshot_id bigint, primary key (id));
create table third_party_text_unit (id bigint not null auto_increment, created_date datetime, last_modified_date datetime, third_party_id varchar(255), asset_id bigint not null, tm_text_unit_id bigint, primary key (id));
alter table third_party_screenshot add constraint UK__THIRD_PARTY_SCREENSHOT__THIRD_PARTY_ID unique (third_party_id);
alter table third_party_text_unit add constraint UK__THIRD_PARTY_TEXT_UNIT__TM_TEXT_UNIT_ID unique (tm_text_unit_id);
alter table third_party_screenshot add constraint FK__THIRD_PARTY_SCREENSHOT__SCREENSHOT__ID foreign key (screenshot_id) references screenshot (id);
alter table third_party_text_unit add constraint FK__THIRD_PARTY_TEXT_UNIT__ASSET__ID foreign key (asset_id) references asset (id);
alter table third_party_text_unit add constraint FK__THIRD_PARTY_TEXT_UNIT__TM_TEXT_UNIT__ID foreign key (tm_text_unit_id) references tm_text_unit (id);
