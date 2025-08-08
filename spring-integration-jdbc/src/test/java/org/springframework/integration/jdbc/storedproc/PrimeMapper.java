/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jdbc.storedproc;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

/**
 *
 * @author Gunnar Hillert
 *
 */
public class PrimeMapper implements RowMapper<Integer> {

	public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
		return rs.getInt("PRIME");
	}

}
