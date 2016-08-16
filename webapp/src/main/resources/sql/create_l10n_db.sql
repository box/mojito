-- with mysql to create user 'l10n' and grant all to the 'l10n' database:
-- GRANT ALL ON l10n.* TO 'l10n'@'localhost' identified by 'l10n';
-- GRANT ALL ON l10n.* TO 'l10n'@'127.0.0.1' identified by 'l10n';
-- from everywhere: GRANT ALL ON l10n.* TO 'l10n'@'%' identified by 'l10n';
-- mysqladmin reload -u root -p
-- jdbc:mysql://localhost:3306/l10n


DROP DATABASE IF EXISTS `l10n`;
CREATE DATABASE `l10n` CHARACTER SET 'utf8' COLLATE 'utf8_bin';

use `l10n`;
