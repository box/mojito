alter table repository add column source_locale_id bigint;
alter table repository_aud add column source_locale_id bigint;
alter table repository add constraint FK__REPOSITORY__LOCALE__ID foreign key (source_locale_id) references locale (id);
