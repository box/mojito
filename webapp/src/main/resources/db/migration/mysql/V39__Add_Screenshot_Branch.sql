alter table screenshot add column branch_id bigint;
alter table screenshot add constraint FK__SCREENSHOT__BRANCH__ID foreign key (branch_id) references branch (id);
