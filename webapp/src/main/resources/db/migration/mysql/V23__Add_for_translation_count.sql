alter table repository_locale_statistic add column for_translation_count bigint default 0;
alter table repository_locale_statistic add column for_translation_word_count bigint default 0;
