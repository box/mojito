create table tm_xliff (id bigint not null auto_increment, created_date datetime, last_modified_date datetime, content longtext, asset_id bigint not null, export_pollable_task_id bigint, locale_id bigint, primary key (id));
alter table tm_xliff add constraint FK__TM_XLIFF__ASSET__ID foreign key (asset_id) references asset (id);
alter table tm_xliff add constraint FK__TM_XLIFF__EXPORT_POLLABLE_TASK__ID foreign key (export_pollable_task_id) references pollable_task (id);
alter table tm_xliff add constraint FK__TM_XLIFF__LOCALE__ID foreign key (locale_id) references locale (id);
