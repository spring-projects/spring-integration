/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.jdbc.storedproc;

import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.SQLException;

import org.springframework.jdbc.core.SqlReturnType;

/**
 * @author Artem Bilan
 * @since 3.0
 */
public class ClobSqlReturnType implements SqlReturnType {

	@Override
	public Object getTypeValue(CallableStatement cs, int paramIndex, int sqlType, String typeName) throws SQLException {
		Clob clob = cs.getClob(paramIndex);
		return clob != null ? clob.getSubString(1, (int) clob.length()) : null;
	}

}
