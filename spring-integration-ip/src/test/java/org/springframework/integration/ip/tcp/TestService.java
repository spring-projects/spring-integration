/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.ip.tcp;

/**
 * Simple echo service.
 *
 * @author Gary Russell
 *
 */
public class TestService {

	public String test(byte[] bytes) {
		return "echo:" + new String(bytes);
	}

	public String test(String s) {
		return "echo:" + s;
	}

}
