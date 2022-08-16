## check DB size

SELECT table_schema “DB Name”,
ROUND(SUM(data_length + index_length) / 1024 / 1024, 1) “DB Size in MB”
FROM information_schema.tables
GROUP BY table_schema;


## log query on the server side for review

SET GLOBAL log_output = 'TABLE';
SET GLOBAL general_log = 'ON';

select * from mysql.general_log

