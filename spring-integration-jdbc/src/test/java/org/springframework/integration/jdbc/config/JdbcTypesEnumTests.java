/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.jdbc.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.sql.Types;
import org.junit.Test;

public class JdbcTypesEnumTests {

	@Test
	public void testGetCode() {

		JdbcTypesEnum jdbcTypesEnum = JdbcTypesEnum.convertToJdbcTypesEnum("VARCHAR");
		assertNotNull("Expected not null jdbcTypesEnum.", jdbcTypesEnum);
		assertEquals(Integer.valueOf(Types.VARCHAR), Integer.valueOf(jdbcTypesEnum.getCode()));

	}

	@Test
	public void testConvertToJdbcTypesEnumWithInvalidParameter() {

		JdbcTypesEnum jdbcTypesEnum = JdbcTypesEnum.convertToJdbcTypesEnum("KENNY4JDBC");
		assertNull("Expected null return value.", jdbcTypesEnum);

	}

	@Test
	public void testConvertToJdbcTypesEnumWithNullParameter() {

		try {
		    JdbcTypesEnum.convertToJdbcTypesEnum(null);
		}
		catch (IllegalArgumentException e) {
			assertEquals("Parameter sqlTypeAsString, must not be null nor empty", e.getMessage());
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
			assertEquals("Parameter sqlTypeAsString, must not be null nor empty", e.getMessage());
			return;
		}

		fail("Expected Exception");

	}
}
