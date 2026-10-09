/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.router.config;

import java.util.List;

/**
 * @author Mark Fisher
 */
public class NumberAdder {

	public Integer sum(List<Integer> values) {
		int result = 0;
		for (Integer value : values) {
			result += value;
		}
		return result;
	}

}
