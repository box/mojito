create table `drop` (id bigint not null auto_increment, created_date datetime, last_modified_date datetime, canceled bit, drop_exporter_config varchar(255), drop_exporter_type varchar(255), last_imported_date datetime, name varchar(255), created_by_user_id bigint, export_pollable_task_id bigint, import_pollable_task_id bigint, repository_id bigint, primary key (id));
create table asset (id bigint not null auto_increment, created_date datetime, last_modified_date datetime, content longtext not null, content_md5 varchar(32), path varchar(255) not null, created_by_user_id bigint, last_successful_asset_extraction_id bigint, repository_id bigint not null, primary key (id));
create table asset_extraction (id bigint not null auto_increment, created_date datetime, last_modified_date datetime, content_md5 varchar(32), asset_id bigint, created_by_user_id bigint, pollable_task_id bigint, primary key (id));
create table asset_integrity_checker (id bigint not null auto_increment, asset_extension varchar(255) not null, integrity_checker_type varchar(255) not null, repository_id bigint not null, primary key (id));
create table asset_integrity_checker_aud (id bigint not null, rev integer not null, revtype tinyint, revend integer, asset_extension varchar(255), integrity_checker_type varchar(255), repository_id bigint, primary key (id, rev));
create table asset_text_unit (id bigint not null auto_increment, created_date datetime, last_modified_date datetime, comment longtext, content longtext, content_md5 varchar(32), md5 varchar(32), name longtext, asset_extraction_id bigint, created_by_user_id bigint, primary key (id));
create table asset_text_unit_to_tm_text_unit (id bigint not null auto_increment, asset_extraction_id bigint, asset_text_unit_id bigint, tm_text_unit_id bigint, primary key (id));
create table authority (id bigint not null auto_increment, created_date datetime, last_modified_date datetime, authority varchar(255), created_by_user_id bigint, user_id bigint not null, primary key (id));
create table group_authorities (id bigint not null auto_increment, created_date datetime, last_modified_date datetime, authority varchar(255) not null, created_by_user_id bigint, group_id bigint not null, primary key (id));
create table group_members (id bigint not null auto_increment, created_date datetime, last_modified_date datetime, created_by_user_id bigint, group_id bigint not null, username bigint not null, primary key (id));
create table `groups` (id bigint not null auto_increment, created_date datetime, last_modified_date datetime, group_name varchar(255) not null, created_by_user_id bigint, primary key (id));
create table locale (id bigint not null auto_increment, bcp47_tag varchar(255) not null, primary key (id));
create table pollable_task (id bigint not null auto_increment, created_date datetime, last_modified_date datetime, error_message longtext, error_stacks longtext, expected_sub_task_number integer not null, finished_date datetime, message longtext, name varchar(255) not null, timeout bigint, created_by_user_id bigint, parent_task_id bigint, primary key (id));
create table repository (id bigint not null auto_increment, created_date datetime, last_modified_date datetime, description varchar(255), drop_exporter_type varchar(255), name varchar(255) not null, created_by_user_id bigint, repository_statistic_id bigint, tm_id bigint, primary key (id));
create table repository_aud (id bigint not null, rev integer not null, revtype tinyint, revend integer, description varchar(255), drop_exporter_type varchar(255), name varchar(255), created_by_user_id bigint, repository_statistic_id bigint, tm_id bigint, primary key (id, rev));
create table repository_locale (id bigint not null auto_increment, to_be_fully_translated bit, locale_id bigint not null, parent_locale bigint, repository_id bigint not null, primary key (id));
create table repository_locale_aud (id bigint not null, rev integer not null, revtype tinyint, revend integer, to_be_fully_translated bit, locale_id bigint, parent_locale bigint, repository_id bigint, primary key (id, rev));
create table repository_locale_statistic (id bigint not null auto_increment, include_in_file_count bigint, review_needed_count bigint, translated_count bigint, translation_needed_count bigint, locale_id bigint not null, repository_statistic_id bigint, primary key (id));
create table repository_statistic (id bigint not null auto_increment, created_date datetime, last_modified_date datetime, uncommented_text_unit_count bigint, unused_text_unit_count bigint, unused_text_unit_word_count bigint, used_text_unit_count bigint, used_text_unit_word_count bigint, created_by_user_id bigint, primary key (id));
create table revchanges (rev integer not null, entityname varchar(255));
create table revinfo (rev integer not null auto_increment, revtstmp bigint, primary key (rev));
create table tm (id bigint not null auto_increment, created_date datetime, last_modified_date datetime, created_by_user_id bigint, primary key (id));
create table tm_text_unit (id bigint not null auto_increment, created_date datetime, comment longtext, content longtext, content_md5 varchar(32), md5 varchar(32), name longtext, asset_id bigint not null, created_by_user_id bigint, tm_id bigint, primary key (id));
create table tm_text_unit_current_variant (id bigint not null auto_increment, created_date datetime, last_modified_date datetime, created_by_user_id bigint, locale_id bigint, tm_id bigint, tm_text_unit_id bigint, tm_text_unit_variant_id bigint, primary key (id));
create table tm_text_unit_current_variant_aud (id bigint not null, rev integer not null, revtype tinyint, revend integer, created_by_user_id bigint, locale_id bigint, tm_id bigint, tm_text_unit_id bigint, tm_text_unit_variant_id bigint, primary key (id, rev));
create table tm_text_unit_variant (id bigint not null auto_increment, created_date datetime, comment longtext, content longtext, content_md5 varchar(32), included_in_localized_file bit, status varchar(255) not null, created_by_user_id bigint, locale_id bigint, tm_text_unit_id bigint, primary key (id));
create table tm_text_unit_variant_comment (id bigint not null auto_increment, created_date datetime, last_modified_date datetime, content longtext, severity varchar(255), type varchar(255), created_by_user_id bigint, tm_text_unit_variant_id bigint, primary key (id));
create table translation_kit (id bigint not null auto_increment, created_date datetime, last_modified_date datetime, num_bad_language_detections integer, num_source_equals_target integer, num_translated_translation_kit_units integer, num_translation_kit_units integer, type integer, created_by_user_id bigint, drop_id bigint, locale_id bigint, primary key (id));
create table translation_kit_not_found_text_unit_ids (translation_kit_id bigint not null, not_found_text_unit_ids varchar(255));
create table translation_kit_text_unit (id bigint not null auto_increment, created_date datetime, last_modified_date datetime, detected_language varchar(255), detected_language_exception varchar(255), detected_language_expected varchar(255), detected_language_probability double precision, source_equals_target bit, created_by_user_id bigint, tm_text_unit_id bigint, tm_text_unit_variant_id bigint, translation_kit_id bigint, primary key (id));
create table user (id bigint not null auto_increment, created_date datetime, last_modified_date datetime, common_name varchar(255), enabled bit, given_name varchar(255), password varchar(255), surname varchar(255), username varchar(255) not null, created_by_user_id bigint, primary key (id));
alter table asset add constraint UK__ASSET__REPOSITORY_ID__PATH unique (repository_id, path);
create index I__ASSET__CONTENT_MD5 on asset (content_md5);
alter table asset_integrity_checker add constraint UK__ASSET_INTEGRITY_CHECKER__REPOSITORY_ID__ASSET_EXTENSION unique (repository_id, asset_extension);
alter table asset_text_unit add constraint UK__ASSET_TEXT_UNIT__MD5__ASSET_EXTRACTION_ID unique (md5, asset_extraction_id);
alter table asset_text_unit_to_tm_text_unit add constraint UK__ASSET_TEXT_UNIT_TO_TM_TEXT_UNIT__ASSET_TEXT_UNIT_ID unique (asset_text_unit_id);
alter table authority add constraint UK__AUTHORITIES__USER_ID__AUTHORITY unique (user_id, authority);
alter table locale add constraint UK__LOCALE__BCP47_TAG unique (bcp47_tag);
create index I__POLLABLE_TASK__NAME on pollable_task (name);
create index I__POLLABLE_TASK__FINISHED_DATE on pollable_task (finished_date);
alter table repository add constraint UK__REPOSITORY__NAME unique (name);
alter table repository_locale add constraint UK__REPOSITORY_LOCALE__REPOSITORY_ID__LOCALE_ID unique (repository_id, locale_id);
alter table tm_text_unit add constraint UK__TM_TEXT_UNIT__MD5__TM_ID__ASSET_ID unique (md5, tm_id, asset_id);
create index I__TM_TEXT_UNIT__NAME on tm_text_unit (name(25));
create index I__TM_TEXT_UNIT__CONTENT_MD5 on tm_text_unit (content_md5);
alter table tm_text_unit_current_variant add constraint UK__TM_TEXT_UNIT_ID__LOCALE_ID unique (tm_text_unit_id, locale_id);
alter table user add constraint I__USERS__USERNAME unique (username);
alter table `drop` add constraint FK__DROP__USER__ID foreign key (created_by_user_id) references user (id);
alter table `drop` add constraint FK__DROP__EXPORT_POLLABLE_TASK__ID foreign key (export_pollable_task_id) references pollable_task (id);
alter table `drop` add constraint FK__DROP__IMPORT_POLLABLE_TASK__ID foreign key (import_pollable_task_id) references pollable_task (id);
alter table `drop` add constraint FK__DROP__REPOSITORY__ID foreign key (repository_id) references repository (id);
alter table asset add constraint FK__ASSET__USER__ID foreign key (created_by_user_id) references user (id);
alter table asset add constraint FK__ASSET__ASSET_EXTRACTION__ID foreign key (last_successful_asset_extraction_id) references asset_extraction (id);
alter table asset add constraint FK__ASSET__REPOSITORY__ID foreign key (repository_id) references repository (id);
alter table asset_extraction add constraint FK__ASSET_EXTRACTION__ASSET__ID foreign key (asset_id) references asset (id);
alter table asset_extraction add constraint FK__ASSET_EXTRACTION__USER__ID foreign key (created_by_user_id) references user (id);
alter table asset_extraction add constraint FK__ASSET_EXTRACTION__POLLABLE_TASK__ID foreign key (pollable_task_id) references pollable_task (id);
alter table asset_integrity_checker add constraint FK__ASSET_INTEGRITY_CHECKER__REPOSITORY__ID foreign key (repository_id) references repository (id);
alter table asset_integrity_checker_aud add constraint FK_97kgwh7m16y9gi0p44cbt0j5n foreign key (rev) references revinfo (rev);
alter table asset_integrity_checker_aud add constraint FK_lkpprvc29x9x4wko484gfqtr0 foreign key (revend) references revinfo (rev);
alter table asset_text_unit add constraint FK__ASSET_TEXT_UNIT__ASSET_EXTRACTION__ID foreign key (asset_extraction_id) references asset_extraction (id);
alter table asset_text_unit add constraint FK__ASSET_TEXT_UNIT__USER__ID foreign key (created_by_user_id) references user (id);
alter table asset_text_unit_to_tm_text_unit add constraint FK__ASSET_TEXT_UNIT_TO_TM_TEXT_UNIT__ASSET_EXTRACTION__ID foreign key (asset_extraction_id) references asset_extraction (id);
alter table asset_text_unit_to_tm_text_unit add constraint FK__ASSET_TEXT_UNIT_TO_TM_TEXT_UNIT__ASSET_TEXT_UNIT__ID foreign key (asset_text_unit_id) references asset_text_unit (id);
alter table asset_text_unit_to_tm_text_unit add constraint FK__ASSET_TEXT_UNIT_TO_TM_TEXT_UNIT__TM_TEXT_UNIT__ID foreign key (tm_text_unit_id) references tm_text_unit (id);
alter table authority add constraint FK__AUTHORITY__USER__ID foreign key (created_by_user_id) references user (id);
alter table authority add constraint FK__AUTHORITY__USER__USER_ID foreign key (user_id) references user (id);
alter table group_authorities add constraint FK__GROUP_AUTHORITY__USER__ID foreign key (created_by_user_id) references user (id);
alter table group_authorities add constraint FK__GROUP_AUTHORITY__GROUP__ID foreign key (group_id) references `groups` (id);
alter table group_members add constraint FK__GROUP_MEMBER__USER__ID foreign key (created_by_user_id) references user (id);
alter table group_members add constraint FK__GROUP_MEMBER__GROUP__ID foreign key (group_id) references `groups` (id);
alter table group_members add constraint FK__GROUP_MEMBER__USER__USERNAME foreign key (username) references user (id);
alter table `groups` add constraint FK__GROUP__USER__ID foreign key (created_by_user_id) references user (id);
alter table pollable_task add constraint FK__POLLABLE_TASK__USER__ID foreign key (created_by_user_id) references user (id);
alter table pollable_task add constraint FK__POLLABLE_TASK__POLLABLE_TASK__ID foreign key (parent_task_id) references pollable_task (id);
alter table repository add constraint FK__REPOSITORY__USER__ID foreign key (created_by_user_id) references user (id);
alter table repository add constraint FK__REPOSITORY__REPOSITORY_STATISTIC__ID foreign key (repository_statistic_id) references repository_statistic (id);
alter table repository add constraint FK__REPOSITORY__TM__ID foreign key (tm_id) references tm (id);
alter table repository_aud add constraint FK_31jpp35j7dg5bu5s3a3a4spxg foreign key (rev) references revinfo (rev);
alter table repository_aud add constraint FK_6r1g89l9dmh7q2m63irb0uba5 foreign key (revend) references revinfo (rev);
alter table repository_locale add constraint FK__REPOSITORY_LOCALE__LOCALE__ID foreign key (locale_id) references locale (id);
alter table repository_locale add constraint FK__REPOSITORY_LOCALE__PARENT_LOCALE__ID foreign key (parent_locale) references repository_locale (id);
alter table repository_locale add constraint FK__REPOSITORY_LOCALE__REPOSITORY__ID foreign key (repository_id) references repository (id);
alter table repository_locale_aud add constraint FK_r28c64aa7r4fiwai15xh7t5nr foreign key (rev) references revinfo (rev);
alter table repository_locale_aud add constraint FK_dyn8f3leo4try12vjsio3k0p0 foreign key (revend) references revinfo (rev);
alter table repository_locale_statistic add constraint FK__REPOSITORY_LOCALE_STATISTIC__LOCALE__ID foreign key (locale_id) references locale (id);
alter table repository_locale_statistic add constraint FK__REPOSITORY_LOCALE_STATISTIC__REPOSITORY__ID foreign key (repository_statistic_id) references repository_statistic (id);
alter table repository_statistic add constraint FK__REPOSITORY_STATISTIC__USER__ID foreign key (created_by_user_id) references user (id);
alter table revchanges add constraint FK_et6b2lrkqkab5mhvxkv861n8h foreign key (rev) references revinfo (rev);
alter table tm add constraint FK__TM__USER__ID foreign key (created_by_user_id) references user (id);
alter table tm_text_unit add constraint FK__TM_TEXT_UNIT__ASSET__ID foreign key (asset_id) references asset (id);
alter table tm_text_unit add constraint FK__TM_TEXT_UNIT__USER__ID foreign key (created_by_user_id) references user (id);
alter table tm_text_unit add constraint FK__TM_TEXT_UNIT__TM__ID foreign key (tm_id) references tm (id);
alter table tm_text_unit_current_variant add constraint FK__TM_TEXT_UNIT_CURRENT_VARIANT__USER__ID foreign key (created_by_user_id) references user (id);
alter table tm_text_unit_current_variant add constraint FK__TM_TEXT_UNIT_CURRENT_VARIANT__LOCALE__ID foreign key (locale_id) references locale (id);
alter table tm_text_unit_current_variant add constraint FK__TM_TEXT_UNIT_CURRENT_VARIANT__TM__ID foreign key (tm_id) references tm (id);
alter table tm_text_unit_current_variant add constraint FK__TM_TEXT_UNIT_CURRENT_VARIANT__TM_TEXT_UNIT__ID foreign key (tm_text_unit_id) references tm_text_unit (id);
alter table tm_text_unit_current_variant add constraint FK__TM_TEXT_UNIT_CURRENT_VARIANT__TM_TEXT_UNIT_VARIANT__ID foreign key (tm_text_unit_variant_id) references tm_text_unit_variant (id);
alter table tm_text_unit_current_variant_aud add constraint FK_lprpa781tusygcuxir1umv5vb foreign key (rev) references revinfo (rev);
alter table tm_text_unit_current_variant_aud add constraint FK_nnkhapalm3l0qxhoa5t7hbjyw foreign key (revend) references revinfo (rev);
alter table tm_text_unit_variant add constraint FK__TM_TEXT_UNIT_VARIANT__USER__ID foreign key (created_by_user_id) references user (id);
alter table tm_text_unit_variant add constraint FK__TM_TEXT_UNIT_VARIANT__LOCALE__ID foreign key (locale_id) references locale (id);
alter table tm_text_unit_variant add constraint FK__TM_TEXT_UNIT_VARIANT__TM_TEXT_UNIT__ID foreign key (tm_text_unit_id) references tm_text_unit (id);
alter table tm_text_unit_variant_comment add constraint FK__TM_TEXT_UNIT_VARIANT_COMMENT__USER__ID foreign key (created_by_user_id) references user (id);
alter table tm_text_unit_variant_comment add constraint FK__TM_TEXT_UNIT_VARIANT_COMMENT__TM_TEXT_UNIT_VARIANT__ID foreign key (tm_text_unit_variant_id) references tm_text_unit_variant (id);
alter table translation_kit add constraint FK__TRANSLATION_KIT__USER__ID foreign key (created_by_user_id) references user (id);
alter table translation_kit add constraint FK__TRANSLATION_KIT__DROP__ID foreign key (drop_id) references `drop` (id);
alter table translation_kit add constraint FK__TRANSLATION_KIT__LOCALE__ID foreign key (locale_id) references locale (id);
alter table translation_kit_not_found_text_unit_ids add constraint FK_aw1j6y6fkkgf1tsgqw45o95am foreign key (translation_kit_id) references translation_kit (id);
alter table translation_kit_text_unit add constraint FK__TRANSLATION_KIT_TEXT_UNIT__USER__ID foreign key (created_by_user_id) references user (id);
alter table translation_kit_text_unit add constraint FK__TRANSLATION_KIT_TEXT_UNIT__TM_TEXT_UNIT__ID foreign key (tm_text_unit_id) references tm_text_unit (id);
alter table translation_kit_text_unit add constraint FK__TRANSLATION_KIT_TEXT_UNIT__TM_TEXT_UNIT_VARIANT__ID foreign key (tm_text_unit_variant_id) references tm_text_unit_variant (id);
alter table translation_kit_text_unit add constraint FK__TRANSLATION_KIT_TEXT_UNIT__TRANSLATION_KIT__ID foreign key (translation_kit_id) references translation_kit (id);
alter table user add constraint FK__USER__USER__ID foreign key (created_by_user_id) references user (id);


insert into locale (id, bcp47_tag) values (157, 'af-ZA'); 
insert into locale (id, bcp47_tag) values (161, 'ar-AE'); 
insert into locale (id, bcp47_tag) values (165, 'ar-BH'); 
insert into locale (id, bcp47_tag) values (169, 'ar-DZ'); 
insert into locale (id, bcp47_tag) values (173, 'ar-EG'); 
insert into locale (id, bcp47_tag) values (177, 'ar-IQ'); 
insert into locale (id, bcp47_tag) values (181, 'ar-JO'); 
insert into locale (id, bcp47_tag) values (185, 'ar-KW'); 
insert into locale (id, bcp47_tag) values (189, 'ar-LB'); 
insert into locale (id, bcp47_tag) values (193, 'ar-LY'); 
insert into locale (id, bcp47_tag) values (197, 'ar-MA'); 
insert into locale (id, bcp47_tag) values (201, 'ar-OM'); 
insert into locale (id, bcp47_tag) values (205, 'ar-QA'); 
insert into locale (id, bcp47_tag) values (209, 'ar-SA'); 
insert into locale (id, bcp47_tag) values (213, 'ar-SY'); 
insert into locale (id, bcp47_tag) values (217, 'ar-TN'); 
insert into locale (id, bcp47_tag) values (221, 'ar-YE'); 
insert into locale (id, bcp47_tag) values (225, 'az-AZ'); 
insert into locale (id, bcp47_tag) values (1, 'be-BY'); 
insert into locale (id, bcp47_tag) values (237, 'bg-BG'); 
insert into locale (id, bcp47_tag) values (241, 'bs-BA'); 
insert into locale (id, bcp47_tag) values (245, 'ca-ES'); 
insert into locale (id, bcp47_tag) values (249, 'cs-CZ'); 
insert into locale (id, bcp47_tag) values (253, 'cy-GB'); 
insert into locale (id, bcp47_tag) values (9, 'da-DK'); 
insert into locale (id, bcp47_tag) values (261, 'de-AT'); 
insert into locale (id, bcp47_tag) values (265, 'de-CH'); 
insert into locale (id, bcp47_tag) values (17, 'de-DE'); 
insert into locale (id, bcp47_tag) values (273, 'de-LI'); 
insert into locale (id, bcp47_tag) values (277, 'de-LU'); 
insert into locale (id, bcp47_tag) values (281, 'dv-MV'); 
insert into locale (id, bcp47_tag) values (285, 'el-GR'); 
insert into locale (id, bcp47_tag) values (21, 'en'); 
insert into locale (id, bcp47_tag) values (33, 'en-AU'); 
insert into locale (id, bcp47_tag) values (297, 'en-BZ'); 
insert into locale (id, bcp47_tag) values (29, 'en-CA'); 
insert into locale (id, bcp47_tag) values (305, 'en-CB'); 
insert into locale (id, bcp47_tag) values (25, 'en-GB'); 
insert into locale (id, bcp47_tag) values (313, 'en-IE'); 
insert into locale (id, bcp47_tag) values (317, 'en-JM'); 
insert into locale (id, bcp47_tag) values (321, 'en-NZ'); 
insert into locale (id, bcp47_tag) values (325, 'en-PH'); 
insert into locale (id, bcp47_tag) values (329, 'en-TT'); 
insert into locale (id, bcp47_tag) values (333, 'en-US'); 
insert into locale (id, bcp47_tag) values (337, 'en-ZA'); 
insert into locale (id, bcp47_tag) values (341, 'en-ZW'); 
insert into locale (id, bcp47_tag) values (345, 'es-AR'); 
insert into locale (id, bcp47_tag) values (349, 'es-BO'); 
insert into locale (id, bcp47_tag) values (353, 'es-CL'); 
insert into locale (id, bcp47_tag) values (357, 'es-CO'); 
insert into locale (id, bcp47_tag) values (361, 'es-CR'); 
insert into locale (id, bcp47_tag) values (365, 'es-DO'); 
insert into locale (id, bcp47_tag) values (369, 'es-EC'); 
insert into locale (id, bcp47_tag) values (41, 'es-ES'); 
insert into locale (id, bcp47_tag) values (381, 'es-GT'); 
insert into locale (id, bcp47_tag) values (385, 'es-HN'); 
insert into locale (id, bcp47_tag) values (389, 'es-MX'); 
insert into locale (id, bcp47_tag) values (393, 'es-NI'); 
insert into locale (id, bcp47_tag) values (397, 'es-PA'); 
insert into locale (id, bcp47_tag) values (401, 'es-PE'); 
insert into locale (id, bcp47_tag) values (405, 'es-PR'); 
insert into locale (id, bcp47_tag) values (409, 'es-PY'); 
insert into locale (id, bcp47_tag) values (413, 'es-SV'); 
insert into locale (id, bcp47_tag) values (417, 'es-UY'); 
insert into locale (id, bcp47_tag) values (421, 'es-VE'); 
insert into locale (id, bcp47_tag) values (425, 'et-EE'); 
insert into locale (id, bcp47_tag) values (429, 'eu-ES'); 
insert into locale (id, bcp47_tag) values (433, 'fa-IR'); 
insert into locale (id, bcp47_tag) values (49, 'fi-FI'); 
insert into locale (id, bcp47_tag) values (441, 'fo-FO'); 
insert into locale (id, bcp47_tag) values (445, 'fr-BE'); 
insert into locale (id, bcp47_tag) values (61, 'fr-CA'); 
insert into locale (id, bcp47_tag) values (453, 'fr-CH'); 
insert into locale (id, bcp47_tag) values (57, 'fr-FR'); 
insert into locale (id, bcp47_tag) values (461, 'fr-LU'); 
insert into locale (id, bcp47_tag) values (465, 'fr-MC'); 
insert into locale (id, bcp47_tag) values (469, 'gl-ES'); 
insert into locale (id, bcp47_tag) values (473, 'gu-IN'); 
insert into locale (id, bcp47_tag) values (477, 'he-IL'); 
insert into locale (id, bcp47_tag) values (481, 'hi-IN'); 
insert into locale (id, bcp47_tag) values (485, 'hr-BA'); 
insert into locale (id, bcp47_tag) values (489, 'hr-HR'); 
insert into locale (id, bcp47_tag) values (493, 'hu-HU'); 
insert into locale (id, bcp47_tag) values (497, 'hy-AM'); 
insert into locale (id, bcp47_tag) values (501, 'id-ID'); 
insert into locale (id, bcp47_tag) values (505, 'is-IS'); 
insert into locale (id, bcp47_tag) values (509, 'it-CH'); 
insert into locale (id, bcp47_tag) values (69, 'it-IT'); 
insert into locale (id, bcp47_tag) values (77, 'ja-JP'); 
insert into locale (id, bcp47_tag) values (521, 'ka-GE'); 
insert into locale (id, bcp47_tag) values (525, 'kk-KZ'); 
insert into locale (id, bcp47_tag) values (529, 'kn-IN'); 
insert into locale (id, bcp47_tag) values (85, 'ko-KR'); 
insert into locale (id, bcp47_tag) values (537, 'kok-IN'); 
insert into locale (id, bcp47_tag) values (541, 'ky-KG'); 
insert into locale (id, bcp47_tag) values (545, 'lt-LT'); 
insert into locale (id, bcp47_tag) values (549, 'lv-LV'); 
insert into locale (id, bcp47_tag) values (553, 'mi-NZ'); 
insert into locale (id, bcp47_tag) values (557, 'mk-MK'); 
insert into locale (id, bcp47_tag) values (561, 'mn-MN'); 
insert into locale (id, bcp47_tag) values (565, 'mr-IN'); 
insert into locale (id, bcp47_tag) values (569, 'ms-BN'); 
insert into locale (id, bcp47_tag) values (573, 'ms-MY'); 
insert into locale (id, bcp47_tag) values (577, 'mt-MT'); 
insert into locale (id, bcp47_tag) values (93, 'nb-NO'); 
insert into locale (id, bcp47_tag) values (585, 'nl-BE'); 
insert into locale (id, bcp47_tag) values (101, 'nl-NL'); 
insert into locale (id, bcp47_tag) values (593, 'nn-NO'); 
insert into locale (id, bcp47_tag) values (597, 'ns-ZA'); 
insert into locale (id, bcp47_tag) values (601, 'pa-IN'); 
insert into locale (id, bcp47_tag) values (117, 'pl-PL'); 
insert into locale (id, bcp47_tag) values (609, 'ps-AR'); 
insert into locale (id, bcp47_tag) values (109, 'pt-BR'); 
insert into locale (id, bcp47_tag) values (617, 'pt-PT'); 
insert into locale (id, bcp47_tag) values (621, 'qu-BO'); 
insert into locale (id, bcp47_tag) values (625, 'qu-EC'); 
insert into locale (id, bcp47_tag) values (629, 'qu-PE'); 
insert into locale (id, bcp47_tag) values (633, 'ro-RO'); 
insert into locale (id, bcp47_tag) values (125, 'ru-RU'); 
insert into locale (id, bcp47_tag) values (641, 'sa-IN'); 
insert into locale (id, bcp47_tag) values (645, 'se-FI'); 
insert into locale (id, bcp47_tag) values (657, 'se-NO'); 
insert into locale (id, bcp47_tag) values (669, 'se-SE'); 
insert into locale (id, bcp47_tag) values (681, 'sk-SK'); 
insert into locale (id, bcp47_tag) values (685, 'sl-SI'); 
insert into locale (id, bcp47_tag) values (689, 'sq-AL'); 
insert into locale (id, bcp47_tag) values (693, 'sr-BA'); 
insert into locale (id, bcp47_tag) values (701, 'sr-SP'); 
insert into locale (id, bcp47_tag) values (709, 'sv-FI'); 
insert into locale (id, bcp47_tag) values (133, 'sv-SE'); 
insert into locale (id, bcp47_tag) values (717, 'sw-KE'); 
insert into locale (id, bcp47_tag) values (721, 'syr-SY'); 
insert into locale (id, bcp47_tag) values (725, 'ta-IN'); 
insert into locale (id, bcp47_tag) values (729, 'te-IN'); 
insert into locale (id, bcp47_tag) values (733, 'th-TH'); 
insert into locale (id, bcp47_tag) values (737, 'tl-PH'); 
insert into locale (id, bcp47_tag) values (741, 'tn-ZA'); 
insert into locale (id, bcp47_tag) values (141, 'tr-TR'); 
insert into locale (id, bcp47_tag) values (749, 'tt-RU'); 
insert into locale (id, bcp47_tag) values (753, 'uk-UA'); 
insert into locale (id, bcp47_tag) values (757, 'ur-PK'); 
insert into locale (id, bcp47_tag) values (761, 'uz-UZ'); 
insert into locale (id, bcp47_tag) values (765, 'vi-VN'); 
insert into locale (id, bcp47_tag) values (769, 'xh-ZA'); 
insert into locale (id, bcp47_tag) values (149, 'zh-CN'); 
insert into locale (id, bcp47_tag) values (777, 'zh-HK'); 
insert into locale (id, bcp47_tag) values (781, 'zh-MO'); 
insert into locale (id, bcp47_tag) values (785, 'zh-SG'); 
insert into locale (id, bcp47_tag) values (153, 'zh-TW'); 
insert into locale (id, bcp47_tag) values (793, 'zu-ZA'); 