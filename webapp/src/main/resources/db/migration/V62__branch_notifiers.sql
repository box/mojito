alter table branch_notification add column notifier_id varchar(255);
create table branch_notifiers (branch_id bigint not null, notifiers varchar(255));
alter table branch_notifiers add constraint FK__BRANCH_NOTIFIERS__BRANCH__ID foreign key (branch_id) references branch (id);
