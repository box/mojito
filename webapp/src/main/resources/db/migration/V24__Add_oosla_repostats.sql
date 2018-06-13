alter table repository_statistic add column oosla_created_before datetime;
alter table repository_statistic add column oosla_text_unit_count bigint default 0;
alter table repository_statistic add column oosla_text_unit_word_count bigint default 0;