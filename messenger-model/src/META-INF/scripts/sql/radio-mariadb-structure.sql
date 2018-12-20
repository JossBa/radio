-- MariaDB structure definition script for the radio schema
-- best import using MariaDB client command "source <path to this file>"

SET CHARACTER SET utf8;
DROP DATABASE IF EXISTS radio;
CREATE DATABASE radio CHARACTER SET utf8;
USE radio;

-- define tables, indices, etc.
CREATE TABLE BaseEntity (
	identity BIGINT NOT NULL AUTO_INCREMENT,
	discriminator ENUM("Document", "Person", "Album", "Track") NOT NULL,
	version INTEGER NOT NULL DEFAULT 1,
	creationTimestamp BIGINT NOT NULL,
	PRIMARY KEY (identity),
	KEY (discriminator)
);

CREATE TABLE Document (
	documentIdentity BIGINT NOT NULL,
	contentHash BINARY(32) NOT NULL,
	contentType VARCHAR(63) NOT NULL,
	content LONGBLOB NOT NULL,
	PRIMARY KEY (documentIdentity),
	UNIQUE KEY (contentHash),
	FOREIGN KEY (documentIdentity) REFERENCES BaseEntity (identity) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE Person (
	personIdentity BIGINT NOT NULL,
	avatarReference BIGINT NOT NULL,
	email CHAR(128) NOT NULL,
	passwordHash BINARY(32) NOT NULL,
	groupAlias ENUM("USER", "ADMIN") NOT NULL,
	surname VARCHAR(31) NOT NULL,
	forename VARCHAR(31) NOT NULL,
	lastTransmissionTimestamp BIGINT NULL,
	lastTransmissionAddress VARCHAR(63) NULL,
	PRIMARY KEY (personIdentity),
	UNIQUE KEY (email),
	FOREIGN KEY (personIdentity) REFERENCES BaseEntity (identity) ON DELETE CASCADE ON UPDATE CASCADE,
	FOREIGN KEY (avatarReference) REFERENCES Document (documentIdentity) ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE TABLE Album (
	albumIdentity BIGINT NOT NULL,
	coverReference BIGINT NOT NULL,
	title VARCHAR(127) NOT NULL,
	releaseYear SMALLINT NOT NULL,
	trackCount TINYINT NOT NULL,
	PRIMARY KEY (albumIdentity),
	FOREIGN KEY (albumIdentity) REFERENCES BaseEntity (identity) ON DELETE CASCADE ON UPDATE CASCADE,
	FOREIGN KEY (coverReference) REFERENCES Document (documentIdentity) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE Track (
	trackIdentity BIGINT NOT NULL,
	albumReference BIGINT NOT NULL,
	ownerReference BIGINT NOT NULL,
	recordingReference BIGINT NOT NULL,
	name VARCHAR(127) NOT NULL,
	artist VARCHAR(127) NOT NULL,
	genre VARCHAR(31) NOT NULL,
	ordinal TINYINT NOT NULL, 
	PRIMARY KEY (trackIdentity),
	FOREIGN KEY (trackIdentity) REFERENCES BaseEntity (identity) ON DELETE CASCADE ON UPDATE CASCADE,
	FOREIGN KEY (albumReference) REFERENCES Album (albumIdentity) ON DELETE CASCADE ON UPDATE CASCADE,
	FOREIGN KEY (ownerReference) REFERENCES Person (personIdentity) ON DELETE CASCADE ON UPDATE CASCADE,
	FOREIGN KEY (recordingReference) REFERENCES Document (documentIdentity) ON DELETE CASCADE ON UPDATE CASCADE,
	KEY (artist),
	KEY (genre)
);

-- define views
CREATE ALGORITHM=MERGE VIEW JoinedEntity AS
SELECT *
FROM BaseEntity
LEFT OUTER JOIN Document ON documentIdentity = identity
LEFT OUTER JOIN Person ON personIdentity = identity
LEFT OUTER JOIN Album ON albumIdentity = identity
LEFT OUTER JOIN Track ON trackIdentity = identity;
