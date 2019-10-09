CREATE TABLE box_sdk_service_config (
  id                   BIGINT NOT NULL AUTO_INCREMENT,
  created_date         DATETIME,
  last_modified_date   DATETIME,
  app_user_id          VARCHAR(255),
  client_id            VARCHAR(255),
  client_secret        VARCHAR(255),
  drops_folder_id      VARCHAR(255),
  enterprise_id        VARCHAR(255),
  private_key          LONGTEXT,
  private_key_password VARCHAR(255),
  public_key_id        VARCHAR(255),
  root_folder_id       VARCHAR(255),
  PRIMARY KEY (id)
);
