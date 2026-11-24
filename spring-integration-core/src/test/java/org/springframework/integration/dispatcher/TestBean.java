/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.dispatcher;

/**
 * @author Mark Fisher
 */
public class TestBean {

	public String good(String s) {
		return s;
	}

	public String bad(String s) {
		throw new RuntimeException("intentional test failure");
	}

}
