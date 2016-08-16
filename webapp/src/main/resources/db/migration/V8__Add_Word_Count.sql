alter table tm_text_unit add column word_count integer;
alter table repository_locale_statistic add column include_in_file_word_count bigint;
alter table repository_locale_statistic add column review_needed_word_count bigint;
alter table repository_locale_statistic add column translated_word_count bigint;
alter table repository_locale_statistic add column translation_needed_word_count bigint;
