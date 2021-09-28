/*
 * Copyright 2002-2021 the original author or authors.
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

import org.springframework.util.Assert;

/**
 * This Enumeration provides a handy wrapper around {@link Types}. This makes it
 * possible to look up String representation of the enum constant's name, and thus
 * looking up the respective JDBC Type (int-value).
 *
 * @author Gunnar Hillert
 *
 * @since 2.1
 *
 */
public enum JdbcTypesEnum {
	BIT(Types.BIT),
	TINYINT(Types.TINYINT),
	SMALLINT(Types.SMALLINT),
	INTEGER(Types.INTEGER),
	BIGINT(Types.BIGINT),
	FLOAT(Types.FLOAT),
	REAL(Types.REAL),
	DOUBLE(Types.DOUBLE),
	NUMERIC(Types.NUMERIC),
	DECIMAL(Types.DECIMAL),
	CHAR(Types.CHAR),
	VARCHAR(Types.VARCHAR),
	LONGVARCHAR(Types.LONGVARCHAR),
	DATE(Types.DATE),
	TIME(Types.TIME),
	TIMESTAMP(Types.TIMESTAMP),
	BINARY(Types.BINARY),
	VARBINARY(Types.VARBINARY),
	LONGVARBINARY(Types.LONGVARBINARY),
	NULL(Types.NULL),
	OTHER(Types.OTHER),
	JAVA_OBJECT(Types.JAVA_OBJECT),
	DISTINCT(Types.DISTINCT),
	STRUCT(Types.STRUCT),
	ARRAY(Types.ARRAY),
	BLOB(Types.BLOB),
	CLOB(Types.CLOB),
	REF(Types.REF),
	DATALINK(Types.DATALINK),
	BOOLEAN(Types.BOOLEAN),
	ROWID(Types.ROWID),
	NCHAR(Types.NCHAR),
	NVARCHAR(Types.NVARCHAR),
	LONGNVARCHAR(Types.LONGNVARCHAR),
	NCLOB(Types.NCLOB),
	SQLXML(Types.SQLXML);

	private final int code;

	JdbcTypesEnum(int code) {
		this.code = code;
	}

	/**
	 * Get the numerical representation of the JDBC Type enum. The numerical value
	 * matches exactly the equivalent value in {@link Types}
	 * @return The numerical representation of the constant.
	 */
	public int getCode() {
		return this.code;
	}

	/**
	 * Retrieve the matching enum constant for a provided String representation
	 * of the SQL Types. The provided name must match exactly the identifier as
	 * used to declare the enum constant.
	 * @param sqlTypeAsString Name of the enum to convert. Must be not null and not empty.
	 * @return The enumeration that matches. Returns Null of no match was found.
	 *
	 */
	public static JdbcTypesEnum convertToJdbcTypesEnum(String sqlTypeAsString) {
		Assert.hasText(sqlTypeAsString, "Parameter sqlTypeAsString, must not be null nor empty");
		for (JdbcTypesEnum jdbcType : JdbcTypesEnum.values()) {
			if (jdbcType.name().equalsIgnoreCase(sqlTypeAsString)) {
				return jdbcType;
			}
		}
		return null;
	}
}
