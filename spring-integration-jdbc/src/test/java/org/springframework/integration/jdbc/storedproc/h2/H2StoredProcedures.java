/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.jdbc.storedproc.h2;

import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.h2.tools.SimpleResultSet;

/**
 *
 * @author Gunnar Hillert
 * @author Gary Russell
 *
 */
public final class H2StoredProcedures {

	private H2StoredProcedures() {
		super();
	}

	public static ResultSet getPrimes(int beginRange, int endRange) throws SQLException {

		SimpleResultSet rs = new SimpleResultSet();
		rs.addColumn("PRIME", Types.INTEGER, 10, 0);

		for (int i = beginRange; i <= endRange; i++) {

			if (new BigInteger(String.valueOf(i)).isProbablePrime(100)) {
				rs.addRow(i);
			}
		}

		return rs;
	}

	public static Integer random() {
		return 1 + (int) (Math.random() * 100);
	}

}
