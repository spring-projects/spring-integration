/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.router.config;

/**
 * @author Mark Fisher
 */
public class TestSplitterBean {

	public String[] split(String input) {
		return input.split("\\.");
	}

}
