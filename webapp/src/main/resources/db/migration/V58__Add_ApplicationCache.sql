-- New tables to cache application data.

-- Details about a specific cache store instance
CREATE TABLE application_cache_type (
    id SMALLINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    PRIMARY KEY (id)
);

-- Cache entries data
CREATE TABLE application_cache (
    id BIGINT NOT NULL AUTO_INCREMENT,
    cache_type_id SMALLINT NOT NULL,
    key_md5 CHAR(32) NOT NULL,
    value LONGBLOB NULL,
    created_date datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    expiry_date datetime NULL,
    PRIMARY KEY (id)
);

ALTER TABLE application_cache ADD CONSTRAINT FK__APPLICATION_CACHE__CACHE_TYPE_ID FOREIGN KEY (cache_type_id) REFERENCES application_cache_type (id);
ALTER TABLE application_cache ADD CONSTRAINT UK__APPLICATION_CACHE__CACHE_TYPE_ID__KEY_MD5 unique (cache_type_id, key_md5);

-- Index used for Cache cleanup based on TTL
CREATE INDEX I__APPLICATION_CACHE__EXPIRY_DATE ON application_cache (expiry_date);
