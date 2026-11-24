/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.endpoint;

/**
 * @author Mark Fisher
 */
public class TestBean {

	public String duplicate(String input) {
		return input + input;
	}

}
