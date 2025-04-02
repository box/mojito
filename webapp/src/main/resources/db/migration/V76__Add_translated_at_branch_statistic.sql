ALTER TABLE branch_statistic
ADD COLUMN translated_date DATETIME;

ALTER TABLE branch_statistic
ADD INDEX I__BRANCH_STATISTIC__TRANSLATED_DATE (translated_date);