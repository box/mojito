alter table repository add column repo_type_id bigint;
alter table repository add constraint FK__REPOSITORY__REPO_TYPE__ID foreign key (repo_type_id) references repo_type (id) on delete restrict;
alter table repository_aud add column repo_type_id bigint;
