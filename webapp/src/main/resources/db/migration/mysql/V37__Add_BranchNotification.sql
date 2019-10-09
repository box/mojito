create table branch_notification (id bigint not null auto_increment, content_md5 varchar(255), message_id varchar(255), sender_type varchar(255), new_msg_sent_at datetime, screenshot_missing_msg_sent_at datetime, translated_msg_sent_at datetime, updated_msg_sent_at datetime, branch_id bigint not null, primary key (id));
alter table branch_notification add constraint UK__BRANCH_NOTIFICATION__BRANCH_ID unique (branch_id);
alter table branch_notification add constraint FK__BRANCH_NOTIFICATION__BRANCH__ID foreign key (branch_id) references branch (id);
