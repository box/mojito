alter table repository add column deleted bit not null default false;
alter table repository_aud add column deleted bit not null default false;
