

CREATE TABLE review_project_request (
    id BIGINT(20) NOT NULL AUTO_INCREMENT,
    created_date datetime DEFAULT NULL,
    last_modified_date datetime DEFAULT NULL,
    name varchar(255) DEFAULT NULL,
    notes longtext,
    PRIMARY KEY (id)
);

CREATE TABLE review_project (
    id BIGINT(20) NOT NULL AUTO_INCREMENT,
    created_date datetime DEFAULT NULL,
    last_modified_date datetime DEFAULT NULL,
    created_by_user_id bigint(20) DEFAULT NULL,
    locale_id bigint(20) NOT NULL,
    review_project_request_id bigint(20) DEFAULT NULL,
    type varchar(32) NOT NULL,
    status varchar(32) NOT NULL,
    due_date datetime NOT NULL,
    close_reason varchar(512) DEFAULT NULL,
    text_unit_count int NOT NULL DEFAULT 0,
    word_count int NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

ALTER TABLE review_project
    ADD CONSTRAINT FK__REVIEW_PROJECT__CREATED_BY FOREIGN KEY (created_by_user_id) REFERENCES user (id);

ALTER TABLE review_project
    ADD CONSTRAINT FK__REVIEW_PROJECT__LOCALE FOREIGN KEY (locale_id) REFERENCES locale (id);

ALTER TABLE review_project
    ADD CONSTRAINT FK__REVIEW_PROJECT__REQUEST FOREIGN KEY (review_project_request_id) REFERENCES review_project_request (id);

CREATE TABLE review_project_text_unit (
    id bigint(20) NOT NULL AUTO_INCREMENT,
    review_project_id bigint(20) NOT NULL,
    tm_text_unit_variant_id bigint(20) DEFAULT NULL,
    tm_text_unit_id bigint(20) NOT NULL,
    created_date datetime DEFAULT NULL,
    PRIMARY KEY (id)
);

ALTER TABLE review_project_text_unit
    ADD CONSTRAINT FK__REVIEW_PROJECT_TEXT_UNIT__PROJECT FOREIGN KEY (review_project_id) REFERENCES review_project (id);

ALTER TABLE review_project_text_unit
    ADD CONSTRAINT FK__REVIEW_PROJECT_TEXT_UNIT__VARIANT FOREIGN KEY (tm_text_unit_variant_id) REFERENCES tm_text_unit_variant (id);

ALTER TABLE review_project_text_unit
    ADD CONSTRAINT FK__REVIEW_PROJECT_TEXT_UNIT__TM_TEXT_UNIT FOREIGN KEY (tm_text_unit_id) REFERENCES tm_text_unit (id);

ALTER TABLE review_project_text_unit
    ADD CONSTRAINT UK__REVIEW_PROJECT_TEXT_UNIT__PROJECT_VARIANT UNIQUE (review_project_id, tm_text_unit_variant_id);

CREATE TABLE review_project_text_unit_decision (
    id bigint(20) NOT NULL AUTO_INCREMENT,
    review_project_text_unit_id bigint(20) NOT NULL,
    notes varchar(4000),
    variant_id bigint(20) DEFAULT NULL,
    reviewed_variant_id bigint(20) DEFAULT NULL,
    decision_state varchar(16) NOT NULL DEFAULT 'PENDING',
    created_date datetime DEFAULT NULL,
    last_modified_date datetime DEFAULT NULL,
    created_by_user_id bigint(20) DEFAULT NULL,
    last_modified_by_user_id bigint(20) DEFAULT NULL,
    PRIMARY KEY (id)
);

ALTER TABLE review_project_text_unit_decision
    ADD CONSTRAINT FK__REVIEW_PROJECT_TEXT_UNIT_DECISION__REVIEW_PROJECT_TEXT_UNIT FOREIGN KEY (review_project_text_unit_id) REFERENCES review_project_text_unit (id);

ALTER TABLE review_project_text_unit_decision
    ADD CONSTRAINT FK__REVIEW_PROJECT_TEXT_UNIT_DECISION__VARIANT FOREIGN KEY (variant_id) REFERENCES tm_text_unit_variant (id);

ALTER TABLE review_project_text_unit_decision
    ADD CONSTRAINT FK__REVIEW_PROJECT_TEXT_UNIT_DECISION__REVIEWED_VARIANT FOREIGN KEY (reviewed_variant_id) REFERENCES tm_text_unit_variant (id);

ALTER TABLE review_project_text_unit_decision
    ADD CONSTRAINT FK__REVIEW_PROJECT_TEXT_UNIT_DECISION__CREATED_BY_USER FOREIGN KEY (created_by_user_id) REFERENCES user (id);

ALTER TABLE review_project_text_unit_decision
    ADD CONSTRAINT FK__REVIEW_PROJECT_TEXT_UNIT_DECISION__LAST_MODIFIED_BY_USER FOREIGN KEY (last_modified_by_user_id) REFERENCES user (id);

ALTER TABLE review_project_text_unit_decision
    ADD CONSTRAINT UK__REVIEW_PROJECT_TEXT_UNIT_DECISION__TEXT_UNIT UNIQUE (review_project_text_unit_id);

CREATE TABLE review_project_screenshot (
    id bigint(20) NOT NULL AUTO_INCREMENT,
    review_project_request_id bigint(20) DEFAULT NULL,
    image_name varchar(255) NOT NULL,
    created_date datetime DEFAULT NULL,
    last_modified_date datetime DEFAULT NULL,
    PRIMARY KEY (id)
);

ALTER TABLE review_project_screenshot
    ADD CONSTRAINT FK__REVIEW_PROJECT_SCREENSHOT__REQUEST FOREIGN KEY (review_project_request_id) REFERENCES review_project_request (id);

CREATE INDEX IDX__REVIEW_PROJECT__STATUS ON review_project (status);
