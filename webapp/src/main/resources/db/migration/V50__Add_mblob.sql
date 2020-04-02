create table mblob (id bigint not null auto_increment, created_date datetime, content longblob, expire_after_seconds bigint, name varchar(255), primary key (id));
alter table mblob add constraint UK__MBLOB__NAME unique (name);
