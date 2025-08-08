/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.router.config;

import java.util.List;

/**
 * @author Mark Fisher
 */
public class NumberSplitter {

	public List<Integer> split(Numbers numbers) {
		return numbers.getValues();
	}

}
