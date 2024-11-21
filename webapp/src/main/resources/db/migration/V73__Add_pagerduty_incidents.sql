CREATE TABLE pagerduty_incidents (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    client_name VARCHAR(255) NOT NULL,
    dedup_key VARCHAR(255) NOT NULL,
    triggered_at DATETIME NULL NOT NULL,
    resolved_at DATETIME NULL DEFAULT NULL
);

create index I__PAGERDUTY_INCIDENTS__CLIENT_NAME__DEDUP_KEY__RESOLVED_AT on pagerduty_incidents (client_name, dedup_key, resolved_at);
