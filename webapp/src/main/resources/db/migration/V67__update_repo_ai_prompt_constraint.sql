ALTER TABLE repository_ai_prompt
DROP FOREIGN KEY repository_ai_prompt_ibfk_1;

ALTER TABLE repository_ai_prompt
DROP FOREIGN KEY repository_ai_prompt_ibfk_2;

ALTER TABLE repository_ai_prompt
DROP FOREIGN KEY repository_ai_prompt_ibfk_3;

ALTER TABLE repository_ai_prompt
DROP CONSTRAINT UQ__REPOSITORY_AI_PROMPT__AI_PROMPT_ID_PROMPT_TYPE_ID;

ALTER TABLE repository_ai_prompt
ADD CONSTRAINT UQ__REPOSITORY_AI_PROMPT__ALL UNIQUE (repository_id, ai_prompt_id, prompt_type_id);

ALTER TABLE repository_ai_prompt
ADD CONSTRAINT FK__REPOSITORY_AI_PROMPT__PROMPT_TYPE_ID FOREIGN KEY (prompt_type_id) REFERENCES ai_prompt_type(id);

ALTER TABLE repository_ai_prompt
ADD CONSTRAINT FK__REPOSITORY_AI_PROMPT__AI_PROMPT_ID FOREIGN KEY (ai_prompt_id) REFERENCES ai_prompt(id);

ALTER TABLE repository_ai_prompt
ADD CONSTRAINT FK__REPOSITORY_AI_PROMPT__REPOSITORY_ID FOREIGN KEY (repository_id) REFERENCES repository(id);
