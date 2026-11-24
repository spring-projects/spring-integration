/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.router.config;

/**
 * @author Mark Fisher
 */
public class TestRouter {

	public String route(int input) {
		if (input == 1) {
			return "output1";
		}
		if (input == 2) {
			return "output2";
		}
		if (input == 3) {
			return "channelDoesNotExist";
		}
		return null;
	}

}
