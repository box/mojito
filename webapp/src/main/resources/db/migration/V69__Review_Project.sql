

CREATE TABLE review_project_request (
    id BIGINT(20) NOT NULL AUTO_INCREMENT,
    created_date datetime DEFAULT NULL,
    last_modified_date datetime DEFAULT NULL,
    request_uuid char(36) NOT NULL,
    name varchar(255) DEFAULT NULL,
    payload_json longtext,
    PRIMARY KEY (id)
);

ALTER TABLE review_project_request
    ADD CONSTRAINT UK__REVIEW_PROJECT_REQUEST__UUID UNIQUE (request_uuid);

CREATE TABLE review_project (
    id BIGINT(20) NOT NULL AUTO_INCREMENT,
    created_date datetime DEFAULT NULL,
    last_modified_date datetime DEFAULT NULL,
    created_by_user_id bigint(20) DEFAULT NULL,
    last_modified_by_user_id bigint(20) DEFAULT NULL,
    locale_id bigint(20) NOT NULL,
    review_project_request_id bigint(20) DEFAULT NULL,
    type varchar(32) NOT NULL,
    status varchar(32) NOT NULL,
    name varchar(255) NOT NULL,
    due_date datetime NOT NULL,
    close_reason varchar(512) DEFAULT NULL,
    notes longtext,
    text_unit_count int NOT NULL DEFAULT 0,
    word_count int NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

ALTER TABLE review_project
    ADD CONSTRAINT FK__REVIEW_PROJECT__CREATED_BY FOREIGN KEY (created_by_user_id) REFERENCES user (id);

ALTER TABLE review_project
    ADD CONSTRAINT FK__REVIEW_PROJECT__LAST_MODIFIED_BY FOREIGN KEY (last_modified_by_user_id) REFERENCES user (id);

ALTER TABLE review_project
    ADD CONSTRAINT FK__REVIEW_PROJECT__LOCALE FOREIGN KEY (locale_id) REFERENCES locale (id);

ALTER TABLE review_project
    ADD CONSTRAINT FK__REVIEW_PROJECT__REQUEST FOREIGN KEY (review_project_request_id) REFERENCES review_project_request (id);

CREATE TABLE review_project_repository (
    review_project_id bigint(20) NOT NULL,
    repository_id bigint(20) NOT NULL,
    PRIMARY KEY (review_project_id, repository_id)
);

ALTER TABLE review_project_repository
    ADD CONSTRAINT FK__REVIEW_PROJECT_REPOSITORY__PROJECT FOREIGN KEY (review_project_id) REFERENCES review_project (id);

ALTER TABLE review_project_repository
    ADD CONSTRAINT FK__REVIEW_PROJECT_REPOSITORY__REPO FOREIGN KEY (repository_id) REFERENCES repository (id);

CREATE TABLE review_project_text_unit (
    id bigint(20) NOT NULL AUTO_INCREMENT,
    review_project_id bigint(20) NOT NULL,
    tm_text_unit_variant_id bigint(20) NOT NULL,
    tm_text_unit_id bigint(20) NOT NULL,
    position int DEFAULT NULL,
    selection_reason varchar(64) DEFAULT NULL,
    initial_status varchar(32) DEFAULT NULL,
    initial_variant_hash char(32) DEFAULT NULL,
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

CREATE TABLE review_project_accepted_variant (
    id bigint(20) NOT NULL AUTO_INCREMENT,
    review_project_id bigint(20) NOT NULL,
    review_project_text_unit_id bigint(20) NOT NULL,
    tm_text_unit_variant_id bigint(20) NOT NULL,
    accepted_variant_id bigint(20) NOT NULL,
    accepted_at datetime DEFAULT NULL,
    accepted_by_user_id bigint(20) DEFAULT NULL,
    is_current bit(1) DEFAULT NULL,
    PRIMARY KEY (id)
);

ALTER TABLE review_project_accepted_variant
    ADD CONSTRAINT FK__REVIEW_PROJECT_ACCEPTED_VARIANT__PROJECT FOREIGN KEY (review_project_id) REFERENCES review_project (id);

ALTER TABLE review_project_accepted_variant
    ADD CONSTRAINT FK__REVIEW_PROJECT_ACCEPTED_VARIANT__TEXT_UNIT FOREIGN KEY (review_project_text_unit_id) REFERENCES review_project_text_unit (id);

ALTER TABLE review_project_accepted_variant
    ADD CONSTRAINT FK__REVIEW_PROJECT_ACCEPTED_VARIANT__VARIANT FOREIGN KEY (tm_text_unit_variant_id) REFERENCES tm_text_unit_variant (id);

ALTER TABLE review_project_accepted_variant
    ADD CONSTRAINT FK__REVIEW_PROJECT_ACCEPTED_VARIANT__ACCEPTED_VARIANT FOREIGN KEY (accepted_variant_id) REFERENCES tm_text_unit_variant (id);

ALTER TABLE review_project_accepted_variant
    ADD CONSTRAINT FK__REVIEW_PROJECT_ACCEPTED_VARIANT__USER FOREIGN KEY (accepted_by_user_id) REFERENCES user (id);

ALTER TABLE review_project_accepted_variant
    ADD CONSTRAINT UK__REVIEW_PROJECT_ACCEPTED_VARIANT__PROJECT_VARIANT UNIQUE (review_project_id, tm_text_unit_variant_id);

ALTER TABLE review_project_accepted_variant
    ADD CONSTRAINT UK__REVIEW_PROJECT_ACCEPTED_VARIANT__TEXT_UNIT UNIQUE (review_project_text_unit_id);

CREATE TABLE review_project_screenshot (
    id bigint(20) NOT NULL AUTO_INCREMENT,
    review_project_request_id bigint(20) DEFAULT NULL,
    review_project_id bigint(20) DEFAULT NULL,
    locale_id bigint(20) DEFAULT NULL,
    image_key varchar(255) NOT NULL,
    created_date datetime DEFAULT NULL,
    PRIMARY KEY (id)
);

ALTER TABLE review_project_screenshot
    ADD CONSTRAINT FK__REVIEW_PROJECT_SCREENSHOT__REQUEST FOREIGN KEY (review_project_request_id) REFERENCES review_project_request (id);

ALTER TABLE review_project_screenshot
    ADD CONSTRAINT FK__REVIEW_PROJECT_SCREENSHOT__PROJECT FOREIGN KEY (review_project_id) REFERENCES review_project (id);

ALTER TABLE review_project_screenshot
    ADD CONSTRAINT FK__REVIEW_PROJECT_SCREENSHOT__LOCALE FOREIGN KEY (locale_id) REFERENCES locale (id);

CREATE INDEX IDX__REVIEW_PROJECT_SCREENSHOT__REQUEST ON review_project_screenshot (review_project_request_id);
CREATE INDEX IDX__REVIEW_PROJECT_SCREENSHOT__PROJECT ON review_project_screenshot (review_project_id);
CREATE INDEX IDX__REVIEW_PROJECT_SCREENSHOT__LOCALE ON review_project_screenshot (locale_id);

CREATE INDEX IDX__REVIEW_PROJECT__LOCALE ON review_project (locale_id);
CREATE INDEX IDX__REVIEW_PROJECT__REQUEST ON review_project (review_project_request_id);
CREATE INDEX IDX__REVIEW_PROJECT_TEXT_UNIT__PROJECT ON review_project_text_unit (review_project_id);
CREATE INDEX IDX__REVIEW_PROJECT_TEXT_UNIT__VARIANT ON review_project_text_unit (tm_text_unit_variant_id);
CREATE INDEX IDX__REVIEW_PROJECT_ACCEPTED_VARIANT__PROJECT ON review_project_accepted_variant (review_project_id);
CREATE INDEX IDX__REVIEW_PROJECT_ACCEPTED_VARIANT__VARIANT ON review_project_accepted_variant (tm_text_unit_variant_id);
