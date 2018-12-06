-- 21 is "en"
alter table repository add column source_locale_id bigint not null default 21;
alter table repository_aud add column source_locale_id bigint;
alter table repository add constraint FK__REPOSITORY__LOCALE__ID foreign key (source_locale_id) references locale (id);
