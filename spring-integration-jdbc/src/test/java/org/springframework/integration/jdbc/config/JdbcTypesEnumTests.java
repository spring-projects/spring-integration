/*
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.jdbc.config;

import java.sql.Types;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

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

		assertThatIllegalArgumentException()
				.isThrownBy(() -> JdbcTypesEnum.convertToJdbcTypesEnum(null))
				.withMessage("Parameter sqlTypeAsString, must not be null nor empty");
	}

	@Test
	public void testConvertToJdbcTypesEnumWithEmptyParameter() {

		assertThatIllegalArgumentException()
				.isThrownBy(() -> JdbcTypesEnum.convertToJdbcTypesEnum("   "))
				.withMessage("Parameter sqlTypeAsString, must not be null nor empty");
	}

}
