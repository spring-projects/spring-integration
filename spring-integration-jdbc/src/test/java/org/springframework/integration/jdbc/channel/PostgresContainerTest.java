/*
 * Copyright 2022-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.jdbc.channel;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;

import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * The base contract for JUnit tests based on the container for Postgres.
 * The Testcontainers 'reuse' option must be disabled,so, Ryuk container is started
 * and will clean all the containers up from this test suite after JVM exit.
 * Since the Postgres container instance is shared via static property, it is going to be
 * started only once per JVM, therefore the target Docker container is reused automatically.
 *
 * @author Rafael Winterhalter
 * @since 6.0
 */
@Testcontainers(disabledWithoutDocker = true)
public interface PostgresContainerTest {

	@SuppressWarnings({"unchecked", "rawtypes"})
	PostgreSQLContainer<?> POSTGRES_CONTAINER = new PostgreSQLContainer("postgres:11") {{
		this.setPortBindings(Collections.singletonList("5432:5432"));
	}};

	@BeforeAll
	static void startContainer() throws SQLException {
		POSTGRES_CONTAINER.start();
		try (Connection conn = DriverManager.getConnection(POSTGRES_CONTAINER.getJdbcUrl(),
				POSTGRES_CONTAINER.getUsername(),
				POSTGRES_CONTAINER.getPassword()); Statement stmt = conn.createStatement()) {
			stmt.execute("CREATE SEQUENCE INT_MESSAGE_SEQ START WITH 1 INCREMENT BY 1 NO CYCLE");
			stmt.execute("CREATE TABLE INT_CHANNEL_MESSAGE (MESSAGE_ID CHAR(36) NOT NULL," +
					"GROUP_KEY CHAR(36) NOT NULL," +
					"CREATED_DATE BIGINT NOT NULL," +
					"MESSAGE_PRIORITY BIGINT," +
					"MESSAGE_SEQUENCE BIGINT NOT NULL DEFAULT nextval('INT_MESSAGE_SEQ')," +
					"MESSAGE_BYTES BYTEA," +
					"REGION VARCHAR(100) NOT NULL," +
					"constraint INT_CHANNEL_MESSAGE_PK primary key (REGION, GROUP_KEY, CREATED_DATE, MESSAGE_SEQUENCE))");
			stmt.execute("CREATE FUNCTION INT_CHANNEL_MESSAGE_NOTIFY_FCT() " +
					"RETURNS TRIGGER AS " +
					"$BODY$ " +
					"BEGIN" +
					" PERFORM pg_notify('int_channel_message_notify', NEW.REGION || ' ' || NEW.GROUP_KEY);" +
					" RETURN NEW; " +
					"END; " +
					"$BODY$ " +
					"LANGUAGE PLPGSQL");
			stmt.execute("CREATE TRIGGER INT_CHANNEL_MESSAGE_NOTIFY_TRG " +
					"AFTER INSERT ON INT_CHANNEL_MESSAGE " +
					"FOR EACH ROW " +
					"EXECUTE PROCEDURE INT_CHANNEL_MESSAGE_NOTIFY_FCT()");
		}
	}

	static String getDriverClassName() {
		return POSTGRES_CONTAINER.getDriverClassName();
	}

	static String getJdbcUrl() {
		return POSTGRES_CONTAINER.getJdbcUrl();
	}

	static String getUsername() {
		return POSTGRES_CONTAINER.getUsername();
	}

	static String getPassword() {
		return POSTGRES_CONTAINER.getPassword();
	}

}
