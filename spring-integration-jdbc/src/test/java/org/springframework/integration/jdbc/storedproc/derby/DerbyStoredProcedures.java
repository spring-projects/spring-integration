/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.integration.jdbc.storedproc.derby;

import java.sql.Clob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.support.JdbcUtils;

/**
 *
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Gary Russell
 *
 */
public final class DerbyStoredProcedures {

	private DerbyStoredProcedures() {
		super();
	}

	public static void createUser(String username, String password, String email)
			throws SQLException {
		Connection conn = null;
		PreparedStatement stmt = null;
		try {
			conn = DriverManager.getConnection("jdbc:default:connection");
			String sql = "INSERT INTO USERS "
					+ "(USERNAME, PASSWORD, EMAIL) VALUES (?,?,?)";
			stmt = conn.prepareStatement(sql);
			stmt.setString(1, username);
			stmt.setString(2, password);
			stmt.setString(3, email);
			stmt.executeUpdate();
		}
		finally {
			JdbcUtils.closeStatement(stmt);
			JdbcUtils.closeConnection(conn);
		}
	}

	public static void createUserAndReturnAll(String username, String password,
			String email, ResultSet[] returnedData) throws SQLException {

		Connection conn = null;
		PreparedStatement stmt = null;
		PreparedStatement stmt2 = null;
		try {
			conn = DriverManager.getConnection("jdbc:default:connection");
			String sql = "INSERT INTO USERS "
					+ "(USERNAME, PASSWORD, EMAIL) VALUES (?,?,?)";
			stmt = conn.prepareStatement(sql);
			stmt.setString(1, username);
			stmt.setString(2, password);
			stmt.setString(3, email);
			stmt.executeUpdate();

			stmt2 = conn.prepareStatement("select * from USERS");
			returnedData[0] = stmt2.executeQuery();

		}
		finally {
			JdbcUtils.closeConnection(conn);
		}

	}

	public static void getMessage(String messageId, Clob[] returnedData) throws SQLException {

		Connection conn = DriverManager.getConnection("jdbc:default:connection");

		PreparedStatement stmt = conn.prepareStatement("select MESSAGE_JSON from JSON_MESSAGE where MESSAGE_ID = ?");
		stmt.setString(1, messageId);
		ResultSet results = stmt.executeQuery();
		if (results.next()) {
			returnedData[0] = results.getClob(1);
		}
	}

}
