alter table user add column partially_created bit not null default false;

alter table branch add column deleted bit not null default false;
alter table branch add column created_by_user_id bigint;
alter table branch add constraint FK__BRANCH__USER__ID foreign key (created_by_user_id) references user (id);