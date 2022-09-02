CREATE TABLE INT_MESSAGE  (
	MESSAGE_ID CHAR(36) NOT NULL,
	REGION VARCHAR(100) NOT NULL,
	CREATED_DATE DATETIME(6) NOT NULL,
	MESSAGE_BYTES BLOB,
	constraint INT_MESSAGE_PK primary key (MESSAGE_ID, REGION)
) ENGINE=InnoDB;

CREATE INDEX INT_MESSAGE_IX1 ON INT_MESSAGE (CREATED_DATE);

CREATE TABLE INT_GROUP_TO_MESSAGE  (
	GROUP_KEY CHAR(36) NOT NULL,
	MESSAGE_ID CHAR(36) NOT NULL,
	REGION VARCHAR(100) NOT NULL,
	constraint INT_GROUP_TO_MESSAGE_PK primary key (GROUP_KEY, MESSAGE_ID, REGION)
) ENGINE=InnoDB;

CREATE TABLE INT_MESSAGE_GROUP  (
	GROUP_KEY CHAR(36) NOT NULL,
	REGION VARCHAR(100) NOT NULL,
    GROUP_CONDITION VARCHAR(255),
	COMPLETE BIGINT,
	LAST_RELEASED_SEQUENCE BIGINT,
	CREATED_DATE DATETIME(6) NOT NULL,
	UPDATED_DATE DATETIME(6) DEFAULT NULL,
	constraint INT_MESSAGE_GROUP_PK primary key (GROUP_KEY, REGION)
) ENGINE=InnoDB;

CREATE TABLE INT_LOCK  (
	LOCK_KEY CHAR(36) NOT NULL,
	REGION VARCHAR(100) NOT NULL,
	CLIENT_ID CHAR(36),
	CREATED_DATE DATETIME(6) NOT NULL,
	constraint INT_LOCK_PK primary key (LOCK_KEY, REGION)
) ENGINE=InnoDB;



CREATE TABLE INT_CHANNEL_MESSAGE (
	MESSAGE_ID CHAR(36) NOT NULL,
	GROUP_KEY CHAR(36) NOT NULL,
	CREATED_DATE BIGINT NOT NULL,
	MESSAGE_PRIORITY BIGINT,
	MESSAGE_SEQUENCE BIGINT NOT NULL AUTO_INCREMENT UNIQUE,
	MESSAGE_BYTES BLOB,
	REGION VARCHAR(100) NOT NULL,
	constraint INT_CHANNEL_MESSAGE_PK primary key (REGION, GROUP_KEY, CREATED_DATE, MESSAGE_SEQUENCE)
) ENGINE=InnoDB;

CREATE INDEX INT_CHANNEL_MSG_DELETE_IDX ON INT_CHANNEL_MESSAGE (REGION, GROUP_KEY, MESSAGE_ID);
-- This is only needed if the message group store property 'priorityEnabled' is true
-- CREATE UNIQUE INDEX INT_CHANNEL_MSG_PRIORITY_IDX ON INT_CHANNEL_MESSAGE (REGION, GROUP_KEY, MESSAGE_PRIORITY DESC, CREATED_DATE, MESSAGE_SEQUENCE);


CREATE TABLE INT_METADATA_STORE  (
	METADATA_KEY VARCHAR(255) NOT NULL,
	METADATA_VALUE VARCHAR(4000),
	REGION VARCHAR(100) NOT NULL,
	constraint INT_METADATA_STORE_PK primary key (METADATA_KEY, REGION)
) ENGINE=InnoDB;
