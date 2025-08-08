/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.endpoint;

/**
 * @author Mark Fisher
 */
public class TestBean {

	public String duplicate(String input) {
		return input + input;
	}

}
