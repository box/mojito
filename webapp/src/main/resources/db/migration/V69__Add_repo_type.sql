create table repo_type (id bigint not null auto_increment, created_date datetime, last_modified_date datetime, name varchar(255) not null, description varchar(255), ai_prompt longtext, primary key (id));
alter table repo_type add constraint UK__REPO_TYPE__NAME unique (name);

create table repo_type_integrity_checker (repo_type_id bigint not null, asset_extension varchar(255) not null, integrity_checker_type varchar(255) not null, primary key (repo_type_id, asset_extension, integrity_checker_type));
alter table repo_type_integrity_checker add constraint FK__REPO_TYPE_INTEGRITY_CHECKER__REPO_TYPE__ID foreign key (repo_type_id) references repo_type (id);
