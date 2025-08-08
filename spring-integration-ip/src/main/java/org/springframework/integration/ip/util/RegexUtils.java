/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.ip.util;

/**
 * Regular Expression Utilities.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
public final class RegexUtils {

	/**
	 * Escapes (precedes with \) any characters in the parameter in the set
	 * <p>
	 * <code>.$[]^*+{}()\?|</code>
	 * <p>
	 * Used to escape a string that is used as a regular expression pattern, to remove
	 * the special meaning of these characters.
	 * @param stringToEscape The string to escape.
	 * @return The escaped string.
	 */
	public static String escapeRegexSpecials(String stringToEscape) {
		// In the following, we look for all the specials and any we find
		// are escaped in the output string, allowing that string to
		// be used as a pattern containing the literal specials.
		return stringToEscape.replaceAll("([.$\\[\\]^*+{}()\\\\?|])", "\\\\$1");
	}

	// Non-instantiable utility class
	private RegexUtils() {
		throw new AssertionError("Class Instantiation not allowed.");
	}

}
