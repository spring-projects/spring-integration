/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config;

import java.util.Collection;
import java.util.List;

public class MaxValueReleaseStrategy {

	private long maxValue;

	public MaxValueReleaseStrategy(long maxValue) {
		this.maxValue = maxValue;
	}

	public boolean checkCompletenessAsList(List<Long> numbers) {
		int sum = 0;
		for (long number : numbers) {
			sum += number;
		}
		return sum >= maxValue;
	}

	public boolean checkCompletenessAsCollection(Collection<Long> numbers) {
		int sum = 0;
		for (long number : numbers) {
			sum += number;
		}
		return sum >= maxValue;
	}

}
