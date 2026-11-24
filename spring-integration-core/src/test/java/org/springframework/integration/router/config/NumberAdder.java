/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
