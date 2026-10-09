/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.router.config;

/**
 * @author Mark Fisher
 */
public class TestSplitterBean {

	public String[] split(String input) {
		return input.split("\\.");
	}

}
