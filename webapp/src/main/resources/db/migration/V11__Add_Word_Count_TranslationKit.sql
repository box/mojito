alter table translation_kit add column word_count bigint;
update translation_kit set word_count = 0;
