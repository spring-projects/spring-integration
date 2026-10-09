/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.jdbc.storedproc.h2;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke tests for the H2 aliases that replace the former Derby stored procedures.
 *
 * @author Pratap Chandra Deo
 */
class H2StoredProceduresTests {

	private static EmbeddedDatabase embeddedDatabase;

	private static JdbcTemplate jdbcTemplate;

	@BeforeAll
	static void setUp() {
		embeddedDatabase = new EmbeddedDatabaseBuilder()
				.setType(EmbeddedDatabaseType.H2)
				.addScript("classpath:h2-stored-procedures.sql")
				.build();
		jdbcTemplate = new JdbcTemplate(embeddedDatabase);
	}

	@AfterAll
	static void tearDown() {
		embeddedDatabase.shutdown();
	}

	@AfterEach
	void cleanup() {
		jdbcTemplate.execute("DELETE FROM USERS");
		jdbcTemplate.execute("DELETE FROM JSON_MESSAGE");
	}

	@Test
	void createUserInsertsRow() {
		jdbcTemplate.update("CALL CREATE_USER(?, ?, ?)", "u", "p", "e");

		Map<String, Object> row = jdbcTemplate.queryForMap("SELECT * FROM USERS WHERE USERNAME=?", "u");
		assertThat(row)
				.containsEntry("USERNAME", "u")
				.containsEntry("PASSWORD", "p")
				.containsEntry("EMAIL", "e");
	}

	@Test
	void createUserAndReturnAllReturnsResultSet() {
		jdbcTemplate.update("CALL CREATE_USER(?, ?, ?)", "first", "pw1", "e1");
		jdbcTemplate.update("CALL CREATE_USER(?, ?, ?)", "second", "pw2", "e2");

		List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM USERS");
		assertThat(rows).hasSize(2);
		assertThat(rows)
				.extracting(row -> row.get("USERNAME"))
				.containsExactlyInAnyOrder("first", "second");
		assertThat(rows.get(0).keySet()).contains("USERNAME", "PASSWORD", "EMAIL");
	}

	@Test
	void getMessageReturnsJson() {
		jdbcTemplate.update("INSERT INTO JSON_MESSAGE VALUES (?, ?)", "id-1", "{\"foo\":\"bar\"}");

		String json = jdbcTemplate.queryForObject("SELECT MESSAGE_JSON FROM GET_MESSAGE(?)", String.class, "id-1");
		assertThat(json).isEqualTo("{\"foo\":\"bar\"}");

		Integer missing = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM GET_MESSAGE(?)", Integer.class, "missing");
		assertThat(missing).isZero();
	}

}
