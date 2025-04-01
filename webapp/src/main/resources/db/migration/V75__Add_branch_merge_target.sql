CREATE TABLE branch_merge_target(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    branch_id BIGINT NOT NULL,
    targets_main BIT
);

ALTER TABLE branch_merge_target ADD CONSTRAINT FK__BRANCH_MERGE_TARGET__BRANCH_ID FOREIGN KEY (branch_id) REFERENCES branch(id);

CREATE INDEX I__BRANCH_MERGE_TARGET__BRANCH_ID ON branch_merge_target (branch_id);