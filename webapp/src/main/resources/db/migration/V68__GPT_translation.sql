CREATE TABLE repository_locale_ai_prompt (
   id bigint AUTO_INCREMENT PRIMARY KEY,
   repository_id bigint NOT NULL,
   locale_id bigint NULL,
   ai_prompt_id bigint NOT NULL,
   disabled boolean DEFAULT FALSE
);

ALTER TABLE repository_locale_ai_prompt
ADD CONSTRAINT FK__REPOSITORY_LOCALE_AI_PROMPT__REPOSITORY_ID FOREIGN KEY (repository_id) REFERENCES repository(id);

ALTER TABLE repository_locale_ai_prompt
ADD CONSTRAINT FK__REPOSITORY_LOCALE_AI_PROMPT__LOCALE_ID FOREIGN KEY (locale_id) REFERENCES locale(id);

ALTER TABLE repository_locale_ai_prompt
ADD CONSTRAINT FK__REPOSITORY_LOCALE_AI_PROMPT__AI_PROMPT_ID FOREIGN KEY (ai_prompt_id) REFERENCES ai_prompt(id);

START TRANSACTION;
# Migrate existing repository_ai_prompt mapping entries to new table
INSERT INTO repository_locale_ai_prompt (repository_id, ai_prompt_id)
SELECT repository_id, ai_prompt_id
FROM repository_ai_prompt;
COMMIT;

ALTER TABLE ai_prompt
ADD COLUMN prompt_type_id bigint NULL;

ALTER TABLE ai_prompt
ADD CONSTRAINT FK__AI_PROMPT__PROMPT_TYPE_ID FOREIGN KEY (prompt_type_id) REFERENCES ai_prompt_type(id);

START TRANSACTION;
# Update existing ai_prompt rows with prompt_type_id
UPDATE ai_prompt ap
JOIN repository_ai_prompt rap ON ap.id = rap.ai_prompt_id
SET ap.prompt_type_id = rap.prompt_type_id;
COMMIT;

# Remove old mapping table
DROP TABLE repository_ai_prompt;

INSERT INTO ai_prompt_type (name) VALUES ('TRANSLATION');

CREATE TABLE tm_text_unit_pending_mt (
    id bigint AUTO_INCREMENT PRIMARY KEY,
    tm_text_unit_id bigint NOT NULL,
    created_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE tm_text_unit_pending_mt
ADD CONSTRAINT FK__TM_TEXT_UNIT_PENDING_MT__TM_TEXT_UNIT_ID FOREIGN KEY (tm_text_unit_id) REFERENCES tm_text_unit(id);

ALTER TABLE third_party_text_unit
ADD COLUMN uploaded_file_uri varchar(255) NULL;

