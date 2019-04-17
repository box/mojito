alter table `drop` add column partially_imported bit not null default false;
alter table translation_kit add column imported bit not null default false;
