/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config;

import java.util.Collection;
import java.util.List;

/**
 * @author Marius Bogoevici
 * @author Dave Syer
 * @author Artem Bilan
 */
public class MaxValueReleaseStrategy {

	private final long maxValue;

	public MaxValueReleaseStrategy(long maxValue) {
		this.maxValue = maxValue;
	}

	public boolean checkCompletenessAsList(List<Long> numbers) {
		long sum = 0;
		for (long number : numbers) {
			sum += number;
		}
		return sum >= this.maxValue;
	}

	public boolean checkCompletenessAsCollection(Collection<Long> numbers) {
		long sum = 0;
		for (long number : numbers) {
			sum += number;
		}
		return sum >= this.maxValue;
	}

}
