/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config;

import java.util.List;

/**
 * @author Marius Bogoevici
 */
public class Adder {

	public Long add(List<Long> results) {
		long total = 0L;
		for (long partialResult : results) {
			total += partialResult;
		}
		return total;
	}

}
