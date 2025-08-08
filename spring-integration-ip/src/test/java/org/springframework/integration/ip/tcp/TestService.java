/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.ip.tcp;

/**
 * Simple echo service.
 *
 * @author Gary Russell
 *
 */
public class TestService {

	public String test(byte[] bytes) {
		return "echo:" + new String(bytes);
	}

	public String test(String s) {
		return "echo:" + s;
	}

}
