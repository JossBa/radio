SET CHARACTER SET utf8;
USE radio;


ALTER TABLE Person DROP IF  EXISTS lastTransmissionTimestamp;
ALTER TABLE Person DROP IF  EXISTS lastTransmissionAddress;

ALTER TABLE Person ADD IF NOT EXISTS lastTransmissionTimestamp BIGINT NULL;
ALTER TABLE Person ADD IF NOT EXISTS lastTransmissionAddress VARCHAR(63) NULL;
