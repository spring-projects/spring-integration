/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.jdbc.config;

import java.sql.Types;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class JdbcTypesEnumTests {

	@Test
	public void testGetCode() {

		JdbcTypesEnum jdbcTypesEnum = JdbcTypesEnum.convertToJdbcTypesEnum("VARCHAR");
		assertThat(jdbcTypesEnum).as("Expected not null jdbcTypesEnum.").isNotNull();
		assertThat(Integer.valueOf(jdbcTypesEnum.getCode())).isEqualTo(Integer.valueOf(Types.VARCHAR));

	}

	@Test
	public void testConvertToJdbcTypesEnumWithInvalidParameter() {

		JdbcTypesEnum jdbcTypesEnum = JdbcTypesEnum.convertToJdbcTypesEnum("KENNY4JDBC");
		assertThat(jdbcTypesEnum).as("Expected null return value.").isNull();

	}

	@Test
	public void testConvertToJdbcTypesEnumWithNullParameter() {

		try {
			JdbcTypesEnum.convertToJdbcTypesEnum(null);
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("Parameter sqlTypeAsString, must not be null nor empty");
			return;
		}

		fail("Expected Exception");

	}

	@Test
	public void testConvertToJdbcTypesEnumWithEmptyParameter() {

		try {
			JdbcTypesEnum.convertToJdbcTypesEnum("   ");
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("Parameter sqlTypeAsString, must not be null nor empty");
			return;
		}

		fail("Expected Exception");

	}

}
