/*
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.jdbc.storedproc.h2;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.h2.tools.SimpleResultSet;

/**
 *
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Pratap Chandra Deo
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

	public static void createUser(Connection conn, String username, String password, String email)
			throws SQLException {

		try (PreparedStatement stmt = conn.prepareStatement(
				"INSERT INTO USERS (USERNAME, PASSWORD, EMAIL) VALUES (?,?,?)")) {
			stmt.setString(1, username);
			stmt.setString(2, password);
			stmt.setString(3, email);
			stmt.executeUpdate();
		}
	}

	public static ResultSet createUserAndReturnAll(Connection conn, String username, String password, String email)
			throws SQLException {

		createUser(conn, username, password, email);

		SimpleResultSet rs = new SimpleResultSet();
		rs.addColumn("USERNAME", Types.VARCHAR, 100, 0);
		rs.addColumn("PASSWORD", Types.VARCHAR, 100, 0);
		rs.addColumn("EMAIL", Types.VARCHAR, 100, 0);
		rs.addRow(username, password, email);
		return rs;
	}

	public static ResultSet getMessage(Connection conn, String messageId) throws SQLException {
		SimpleResultSet rs = new SimpleResultSet();
		rs.addColumn("MESSAGE_JSON", Types.VARCHAR, 0, 0);
		try (PreparedStatement stmt = conn.prepareStatement(
				"SELECT MESSAGE_JSON FROM JSON_MESSAGE WHERE MESSAGE_ID = ?")) {
			stmt.setString(1, messageId);
			try (ResultSet results = stmt.executeQuery()) {
				if (results.next()) {
					rs.addRow(results.getString(1));
				}
			}
		}
		return rs;
	}

}
