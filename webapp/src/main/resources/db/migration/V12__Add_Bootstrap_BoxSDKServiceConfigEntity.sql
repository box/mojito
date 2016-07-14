alter table box_sdk_service_config add column bootstrap bit;
update box_sdk_service_config set bootstrap = 0;
