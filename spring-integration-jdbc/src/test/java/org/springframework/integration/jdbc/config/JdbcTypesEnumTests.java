/*
 * Copyright 2016-2024 the original author or authors.
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
