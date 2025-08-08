/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
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
