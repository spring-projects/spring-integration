/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
