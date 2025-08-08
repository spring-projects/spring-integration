/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.dispatcher;

/**
 * @author Mark Fisher
 */
public class TestBean {

	public String good(String s) {
		return s;
	}

	public String bad(String s) {
		throw new RuntimeException("intentional test failure");
	}

}
