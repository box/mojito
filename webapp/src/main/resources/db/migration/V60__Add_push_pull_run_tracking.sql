-- New tables to versioning information for text units being pushed to and pulled from Mojito

-- Track commit (for file assets) or version information (for virtual assets)
CREATE TABLE commit
(
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    repository_id        BIGINT       NOT NULL,
    author_email         VARCHAR(255) NULL,
    author_name          VARCHAR(255) NULL,
    name                 VARCHAR(255) NOT NULL, -- commit ID or abstract/DB entity version ID
    source_creation_date DATETIME     NOT NULL, -- the datetime when the source version was created in the upstream system
    created_date         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
ALTER TABLE commit
    ADD CONSTRAINT UK__COMMIT__NAME unique (name);
ALTER TABLE commit
    ADD CONSTRAINT FK__COMMIT__NAME__REPOSITORY_ID FOREIGN KEY (repository_id) REFERENCES repository (id);

-- Information about a specific instance of a source push.
CREATE TABLE push_run
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    name          VARCHAR(255) NOT NULL, -- server-generated UUID
    repository_id BIGINT       NOT NULL,
    created_date  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
ALTER TABLE push_run
    ADD CONSTRAINT UK__PUSH_RUN__NAME unique (name);
ALTER TABLE push_run
    ADD CONSTRAINT FK__PUSH_RUN__REPOSITORY_ID FOREIGN KEY (repository_id) REFERENCES repository (id);


CREATE TABLE commit_to_push_run
(
    id           BIGINT   NOT NULL AUTO_INCREMENT,
    commit_id    BIGINT   NOT NULL,
    push_run_id  BIGINT   NOT NULL,
    created_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
ALTER TABLE commit_to_push_run
    ADD CONSTRAINT UK__COMMIT_TO_PUSH_RUN__COMMIT_ID unique (commit_id);
ALTER TABLE commit_to_push_run
    ADD CONSTRAINT FK__COMMIT_TO_PUSH_RUN__COMMIT_ID FOREIGN KEY (commit_id) REFERENCES commit (id);
ALTER TABLE commit_to_push_run
    ADD CONSTRAINT FK__COMMIT_TO_PUSH_RUN__PUSH_RUN_ID FOREIGN KEY (push_run_id) REFERENCES push_run (id);


-- Information about a specific instance of a translation pull. The [name] column is used to disambiguate between
-- multiple pulls done against the same commit (e.g.: as a result of extractor, push asset WS or translation changes)
-- This information should only be recorded only after translations are committed into the third party system that will consume them.
CREATE TABLE pull_run
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    name          VARCHAR(255) NOT NULL, -- server-generated UUID
    repository_id BIGINT       NOT NULL,
    created_date  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
ALTER TABLE pull_run
    ADD CONSTRAINT UK__PULL_RUN__NAME unique (name);
ALTER TABLE pull_run
    ADD CONSTRAINT FK__PULL_RUN__REPOSITORY_ID FOREIGN KEY (repository_id) REFERENCES repository (id);


CREATE TABLE commit_to_pull_run
(
    id           BIGINT   NOT NULL AUTO_INCREMENT,
    commit_id    BIGINT   NOT NULL,
    pull_run_id  BIGINT   NOT NULL,
    created_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
ALTER TABLE commit_to_pull_run
    ADD CONSTRAINT UK__COMMIT_TO_PULL_RUN__COMMIT_ID unique (commit_id);
ALTER TABLE commit_to_pull_run
    ADD CONSTRAINT FK__COMMIT_TO_PULL_RUN__COMMIT_ID FOREIGN KEY (commit_id) REFERENCES commit (id);
ALTER TABLE commit_to_pull_run
    ADD CONSTRAINT FK__COMMIT_TO_PULL_RUN__PULL_RUN_ID FOREIGN KEY (pull_run_id) REFERENCES push_run (id);


-- Map an push instance to a specific asset
CREATE TABLE push_run_asset
(
    id           BIGINT   NOT NULL AUTO_INCREMENT,
    push_run_id  BIGINT   NOT NULL,
    asset_id     BIGINT   NOT NULL,
    created_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
ALTER TABLE push_run_asset
    ADD CONSTRAINT UK__PUSH_RUN_ASSET__PUSH_RUN_ID__ASSET_ID unique (push_run_id, asset_id);
ALTER TABLE push_run_asset
    ADD CONSTRAINT FK__PUSH_RUN_ASSET__PUSH_RUN_ID FOREIGN KEY (push_run_id) REFERENCES push_run (id);
ALTER TABLE push_run_asset
    ADD CONSTRAINT FK__PUSH_RUN_ASSET__ASSET_ID FOREIGN KEY (asset_id) REFERENCES asset (id);


-- Map the asset for a specific push to a concrete list of text units
CREATE TABLE push_run_asset_tm_text_unit
(
    id                BIGINT   NOT NULL AUTO_INCREMENT,
    push_run_asset_id BIGINT   NOT NULL,
    tm_text_unit_id   BIGINT   NOT NULL,
    created_date      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
ALTER TABLE push_run_asset_tm_text_unit
    ADD CONSTRAINT FK__PUSH_RUN_ASSET_ID FOREIGN KEY (push_run_asset_id) REFERENCES push_run_asset (id);
ALTER TABLE push_run_asset_tm_text_unit
    ADD CONSTRAINT FK__TM_TEXT_UNIT_ID FOREIGN KEY (tm_text_unit_id) REFERENCES tm_text_unit (id);


-- Map an pull instance to a specific asset
CREATE TABLE pull_run_asset
(
    id           BIGINT   NOT NULL AUTO_INCREMENT,
    pull_run_id  BIGINT   NOT NULL,
    asset_id     BIGINT   NOT NULL,
    created_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
ALTER TABLE pull_run_asset
    ADD CONSTRAINT UK__PULL_RUN_ASSET__PULL_RUN_ID__ASSET_ID UNIQUE (pull_run_id, asset_id);
ALTER TABLE pull_run_asset
    ADD CONSTRAINT FK__PULL_RUN_ASSET__PULL_RUN_ID FOREIGN KEY (pull_run_id) REFERENCES pull_run (id);
ALTER TABLE pull_run_asset
    ADD CONSTRAINT FK__PULL_RUN_ASSET__ASSET_ID FOREIGN KEY (asset_id) REFERENCES asset (id);


-- Map the asset for a specific pull to a concrete list of translations (text unit variants)
CREATE TABLE pull_run_text_unit_variant
(
    id                      BIGINT   NOT NULL AUTO_INCREMENT,
    pull_run_asset_id       BIGINT   NOT NULL,
    tm_text_unit_variant_id BIGINT   NOT NULL, -- joining on this gives us locale_id and (source) tm_text_unit_id
    created_date            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
ALTER TABLE pull_run_text_unit_variant
    ADD CONSTRAINT UK__PULL_RUN_TEXT_UNIT_VARIANT__EVA_ID__TM_TUV_ID UNIQUE (pull_run_asset_id, tm_text_unit_variant_id);
ALTER TABLE pull_run_text_unit_variant
    ADD CONSTRAINT FK__PULL_RUN_TEXT_UNIT_VARIANT__PULL_RUN_ASSET_ID FOREIGN KEY (pull_run_asset_id) REFERENCES pull_run_asset (id);
ALTER TABLE pull_run_text_unit_variant
    ADD CONSTRAINT FK__PULL_RUN_TEXT_UNIT_VARIANT__TM_TEXT_UNIT_VARIANT_ID FOREIGN KEY (tm_text_unit_variant_id) REFERENCES tm_text_unit_variant (id);
