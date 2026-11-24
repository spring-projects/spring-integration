/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.ip.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 */
public class RegexUtilsTests {

	/**
	 * Verify that we properly escape all special characters for matching regex
	 */
	@Test
	public void testRegex() {
		String s = "xxx$^[]{()}+*\\?|.xxx";
		assertThat(RegexUtils.escapeRegexSpecials(s)).isEqualTo("xxx\\$\\^\\[\\]\\{\\(\\)\\}\\+\\*\\\\\\?\\|\\.xxx");
	}

}
