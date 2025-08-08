/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.ip.util;

import org.junit.Test;

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
