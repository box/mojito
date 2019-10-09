alter table repository add column manual_screenshot_run_id bigint;
alter table repository_aud add column manual_screenshot_run_id bigint;
alter table repository add constraint FK__REPOSITORY__SCREENSHOT_RUN__ID foreign key (manual_screenshot_run_id) references screenshot_run (id);
