create table update_statistics (id bigint not null auto_increment, time_to_update datetime, repository_id bigint not null, primary key (id));
create index I__UPDATE_STATISTICS__REPOSITORY_ID on update_statistics (repository_id);
alter table update_statistics add constraint FK__UPDATE_STATISTICS__REPOSITORY_ID foreign key (repository_id) references repository (id);
