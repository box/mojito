create unique index UK__BRANCH_NOTIFICATION__BRANCH_ID__SENDER_TYPE
	on branch_notification (branch_id, sender_type);

drop index UK__BRANCH_NOTIFICATION__BRANCH_ID on branch_notification;
