/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
