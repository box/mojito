create table user_locale (
    id bigint(20) NOT NULL AUTO_INCREMENT,
    user_id bigint not null,
    locale_id bigint not null,
    primary key (id)
);
alter table user_locale add constraint FK__USER_LOCALE__USER__ID foreign key (user_id) references user (id);
alter table user_locale add constraint FK__USER_LOCALE__LOCALE__ID foreign key (locale_id) references locale (id);
create unique index UK__USER_LOCALE__USER_ID__LOCALE_ID on user_locale(user_id, locale_id);
alter table user add can_translate_all_locales bit not null default true;
