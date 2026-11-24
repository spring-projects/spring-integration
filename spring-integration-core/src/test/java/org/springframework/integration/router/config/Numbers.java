/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
