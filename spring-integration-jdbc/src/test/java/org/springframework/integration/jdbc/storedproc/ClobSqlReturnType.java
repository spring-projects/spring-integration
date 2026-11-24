/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
