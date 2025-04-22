ALTER TABLE branch_merge_target
ADD COLUMN commit_id BIGINT;

ALTER TABLE branch_merge_target
ADD CONSTRAINT FK__BRANCH_MERGE_TARGET__COMMIT_ID FOREIGN KEY (commit_id) REFERENCES commit(id);