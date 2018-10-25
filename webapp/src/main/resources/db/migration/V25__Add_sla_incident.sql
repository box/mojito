create table sla_incident (id bigint not null auto_increment, created_date datetime, last_modified_date datetime, closed_date datetime, primary key (id));
create table sla_incident_repositories (sla_incident_id bigint not null, repository_id bigint not null, primary key (sla_incident_id, repository_id));
alter table sla_incident_repositories add constraint FK__SLA_INCIDENT_REPOSITORIES__REPOSITORY__ID foreign key (repository_id) references repository (id);
alter table sla_incident_repositories add constraint FK__SLA_INCIDENT_REPOSITORIES__SLA_INCIDENT__ID foreign key (sla_incident_id) references sla_incident (id);
