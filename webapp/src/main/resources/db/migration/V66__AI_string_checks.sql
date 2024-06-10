CREATE TABLE ai_prompt (
    id bigint AUTO_INCREMENT PRIMARY KEY,
    created_date         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    system_prompt longtext,
    user_prompt longtext,
    prompt_temperature float,
    model_name VARCHAR(255),
    deleted boolean DEFAULT FALSE
);

CREATE TABLE ai_string_check (
    id bigint AUTO_INCREMENT PRIMARY KEY,
    created_date         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    repository_id bigint NOT NULL,
    prompt_id bigint NOT NULL,
    content longtext NOT NULL,
    comment longtext,
    name longtext,
    prompt_output longtext,
    FOREIGN KEY (repository_id) REFERENCES repository(id),
    FOREIGN KEY (prompt_id) REFERENCES ai_prompt(id)
);

CREATE TABLE ai_prompt_type (
    id bigint AUTO_INCREMENT PRIMARY KEY,
    name varchar(255) NOT NULL
);

CREATE TABLE repository_ai_prompt (
    id bigint AUTO_INCREMENT PRIMARY KEY,
    repository_id bigint NOT NULL,
    ai_prompt_id bigint NOT NULL,
    prompt_type_id bigint NOT NULL,
    FOREIGN KEY (ai_prompt_id) REFERENCES ai_prompt(id),
    FOREIGN KEY (repository_id) REFERENCES repository(id),
    FOREIGN KEY (prompt_type_id) REFERENCES ai_prompt_type(id)
);

CREATE TABLE ai_prompt_context_message (
    id bigint AUTO_INCREMENT PRIMARY KEY,
    created_date         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    message_type varchar(255) NOT NULL,
    order_index int NOT NULL,
    ai_prompt_id bigint NOT NULL,
    content longtext NOT NULL,
    deleted boolean DEFAULT FALSE,
    FOREIGN KEY (ai_prompt_id) REFERENCES ai_prompt(id)
);

ALTER TABLE repository_ai_prompt
ADD CONSTRAINT UQ__REPOSITORY_AI_PROMPT__AI_PROMPT_ID_PROMPT_TYPE_ID UNIQUE (ai_prompt_id, prompt_type_id);

CREATE INDEX I__REPOSITORY_AI_PROMPT__REPO_ID__TYPE_ID__PROMPT_ID ON repository_ai_prompt(repository_id, prompt_type_id, ai_prompt_id);

INSERT INTO ai_prompt_type (name) VALUES ('SOURCE_STRING_CHECKER');
