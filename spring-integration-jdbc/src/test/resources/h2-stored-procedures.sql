CREATE ALIAS IF NOT EXISTS GET_PRIME_NUMBERS FOR "org.springframework.integration.jdbc.storedproc.h2.H2StoredProcedures.getPrimes";
CREATE ALIAS IF NOT EXISTS GET_RANDOM_NUMBER FOR "org.springframework.integration.jdbc.storedproc.h2.H2StoredProcedures.random";

CREATE TABLE IF NOT EXISTS USERS (
	USERNAME VARCHAR(100),
	PASSWORD VARCHAR(100),
	EMAIL VARCHAR(100)
);
CREATE TABLE IF NOT EXISTS JSON_MESSAGE (
	MESSAGE_ID CHAR(36),
	MESSAGE_JSON CLOB
);

CREATE ALIAS IF NOT EXISTS CREATE_USER FOR "org.springframework.integration.jdbc.storedproc.h2.H2StoredProcedures.createUser";
CREATE ALIAS IF NOT EXISTS CREATE_USER_RETURN_ALL FOR "org.springframework.integration.jdbc.storedproc.h2.H2StoredProcedures.createUserAndReturnAll";
CREATE ALIAS IF NOT EXISTS GET_MESSAGE FOR "org.springframework.integration.jdbc.storedproc.h2.H2StoredProcedures.getMessage";
