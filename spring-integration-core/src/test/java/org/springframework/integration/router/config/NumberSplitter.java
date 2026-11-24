/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
