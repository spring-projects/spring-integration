/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.router.config;

/**
 * @author Mark Fisher
 */
public class TestRouter {

	public String route(int input) {
		if (input == 1) {
			return "output1";
		}
		if (input == 2) {
			return "output2";
		}
		if (input == 3) {
			return "channelDoesNotExist";
		}
		return null;
	}

}
