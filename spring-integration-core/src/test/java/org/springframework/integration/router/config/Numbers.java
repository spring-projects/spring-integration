/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.router.config;

import java.util.List;

/**
 * @author Mark Fisher
 */
public class Numbers {

	private final List<Integer> values;

	public Numbers(List<Integer> values) {
		this.values = values;
	}

	public List<Integer> getValues() {
		return this.values;
	}

}
