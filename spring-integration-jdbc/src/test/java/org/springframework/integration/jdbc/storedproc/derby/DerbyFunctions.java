/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jdbc.storedproc.derby;

import java.util.Locale;

/**
 *
 * @author Gunnar Hillert
 * @author Gary Russell
 *
 */
public final class DerbyFunctions {

	private DerbyFunctions() {
		super();
	}

	public static String convertStringToUpperCase(String invalue) {
		return invalue.toUpperCase(Locale.ENGLISH);
	}

}
