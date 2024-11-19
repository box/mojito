CREATE TABLE tm_text_unit_to_branch(
   id BIGINT AUTO_INCREMENT PRIMARY KEY,
   tm_text_unit_id BIGINT NOT NULL,
   branch_id BIGINT NOT NULL
);

ALTER TABLE tm_text_unit_to_branch
ADD CONSTRAINT FK__TM_TEXT_UNIT_TO_BRANCH__TM_TEXT_UNIT_ID FOREIGN KEY (tm_text_unit_id) REFERENCES tm_text_unit(id);

ALTER TABLE tm_text_unit_to_branch
ADD CONSTRAINT FK__TM_TEXT_UNIT_TO_BRANCH__BRANCH_ID FOREIGN KEY (branch_id) REFERENCES branch(id);

create index I__TEXT_UNIT_TO_BRANCH__TEXT_UNIT_ID on tm_text_unit_to_branch (tm_text_unit_id);

CREATE TABLE branch_source(
   id BIGINT AUTO_INCREMENT PRIMARY KEY,
   branch_id BIGINT NOT NULL,
   url VARCHAR(255) NOT NULL
);

ALTER TABLE branch_source
ADD CONSTRAINT FK__BRANCH_SOURCE__BRANCH_ID FOREIGN KEY (branch_id) REFERENCES branch(id);

create index I__BRANCH_SOURCE__BRANCH_ID on branch_source (branch_id);

