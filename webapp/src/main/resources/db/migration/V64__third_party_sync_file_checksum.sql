create table third_party_sync_file_checksum (
    id bigint(20) NOT NULL AUTO_INCREMENT,
    repository_id bigint not null,
    locale_id bigint not null,
    md5 char(32) not null,
    file_name varchar(255) not null,
    created_date datetime not null,
    last_modified_date datetime,
    primary key (id)
);
alter table third_party_sync_file_checksum add constraint FK__TPS_FILE_CHECKSUM__REPO__ID foreign key (repository_id) references repository (id);
alter table third_party_sync_file_checksum add constraint FK__TPS_FILE_CHECKSUM__LOCALE__ID foreign key (locale_id) references locale (id);
create unique index I__TPS_FILE_CHECKSUM__REPO_ID__LOCALE_ID__FILE_NAME on third_party_sync_file_checksum(repository_id, locale_id, file_name);