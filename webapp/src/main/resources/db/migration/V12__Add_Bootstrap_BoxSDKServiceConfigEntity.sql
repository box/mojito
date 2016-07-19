alter table box_sdk_service_config add column bootstrap bit;
update box_sdk_service_config set bootstrap = 0;

alter table box_sdk_service_config add column root_folder_url varchar(255);
alter table box_sdk_service_config add column validated bit;
