-- constraint needs to be removed since we won't save content anymore at the asset level
ALTER TABLE asset MODIFY content longtext;

-- Later, when all data are migrated to new branches, those change can be made:
-- DROP INDEX I__ASSET__CONTENT_MD5 ON asset;
-- alter table asset drop column content_md5;
-- alter table asset drop column content;

create table asset_content (id bigint not null auto_increment, created_date datetime, last_modified_date datetime, content longtext not null, content_md5 varchar(32), asset_id bigint, branch_id bigint, primary key (id));
alter table asset_extraction add column asset_content_id bigint;
create table asset_extraction_by_branch (id bigint not null auto_increment, created_date datetime, last_modified_date datetime, deleted bit not null, asset_id bigint, asset_extraction_id bigint, branch_id bigint, primary key (id));
alter table asset_text_unit add column branch bigint;
create table branch (id bigint not null auto_increment, name varchar(255), repository_id bigint not null, primary key (id));
alter table asset_extraction_by_branch add constraint UK__ASSET_EXTRACTION_BY_BRANCH__ASSET_ID__BRANCH unique (asset_id, branch_id);
alter table branch add constraint UK__BRANCH__REPOSITORY_ID__PATH unique (repository_id, name);
alter table asset_content add constraint FK__ASSET_CONTENT__ASSET__ID foreign key (asset_id) references asset (id);
alter table asset_content add constraint FK__ASSET_CONTENT__BRANCH__ID foreign key (branch_id) references branch (id);
alter table asset_extraction add constraint FK__ASSET_EXTRACTION__ASSET_CONTENT__ID foreign key (asset_content_id) references asset_content (id);
alter table asset_extraction_by_branch add constraint FK__ASSET_EXTRACTION_BY_BRANCH__ASSET__ID foreign key (asset_id) references asset (id);
alter table asset_extraction_by_branch add constraint FK__ASSET_EXTRACTION_BY_BRANCH__ASSET_EXTRACTION__ID foreign key (asset_extraction_id) references asset_extraction (id);
alter table asset_extraction_by_branch add constraint FK__ASSET_EXTRACTION_BY_BRANCH__BRANCH__ID foreign key (branch_id) references branch (id);
alter table branch add constraint FK__BRANCH__REPOSITORY__ID foreign key (repository_id) references repository (id);
