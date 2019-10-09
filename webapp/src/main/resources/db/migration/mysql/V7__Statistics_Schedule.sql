create table statistics_schedule (id bigint not null auto_increment, time_to_update datetime, repository_id bigint not null, primary key (id));
create index I__STATISTICS_SCHEDULE__REPOSITORY_ID on statistics_schedule (repository_id);
alter table statistics_schedule add constraint FK__STATISTICS_SCHEDULE__REPOSITORY_ID foreign key (repository_id) references repository (id);
