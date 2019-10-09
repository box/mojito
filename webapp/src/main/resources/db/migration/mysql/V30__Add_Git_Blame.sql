create table git_blame (id bigint not null auto_increment, created_date datetime, last_modified_date datetime, author_email varchar(255), author_name varchar(255), commit_name varchar(255), commit_time varchar(255), tm_text_unit_id bigint not null, primary key (id));
alter table git_blame add constraint UK__GIT_BLAME__TM_TEXT_UNIT_ID unique (tm_text_unit_id);
create index I__GIT_BLAME__AUTHOR_EMAIL on git_blame (author_email);
alter table git_blame add constraint FK__GIT_BLAME__TM_TEXT_UNIT__ID foreign key (tm_text_unit_id) references tm_text_unit (id);
