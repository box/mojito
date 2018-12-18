create index I__ASSET_TEXT_UNIT__BRANCH_ID on asset_text_unit (branch_id);

create table branch_statistic (id bigint not null auto_increment, for_translation_count bigint, total_count bigint, branch_id bigint not null, primary key (id));
create table branch_text_unit_statistic (id bigint not null auto_increment, for_translation_count bigint, total_count bigint, branch_statistic_id bigint, tm_text_unit_id bigint, primary key (id));
alter table branch_statistic add constraint UK__BRANCH_STATISTIC__BRANCH_ID unique (branch_id);
alter table branch_text_unit_statistic add constraint UK__BTU_STAT__BTU_STAT_ID__TM_TEXT_UNIT_ID unique (branch_statistic_id, tm_text_unit_id);
alter table branch_statistic add constraint FK__BRANCH_STATISTIC__BRANCH__ID foreign key (branch_id) references branch (id);
alter table branch_text_unit_statistic add constraint FK__BTU_STAT__BRANCH_STATISTIC__ID foreign key (branch_statistic_id) references branch_statistic (id);
alter table branch_text_unit_statistic add constraint FK__BTU_STAT_BRANCH__TM_TEXT_UNIT__ID foreign key (tm_text_unit_id) references tm_text_unit (id);
