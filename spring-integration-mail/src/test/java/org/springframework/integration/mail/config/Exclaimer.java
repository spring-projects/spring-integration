/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.mail.config;

/**
 * @author Mark Fisher
 */
public class Exclaimer {

	public String exclaim(String input) {
		return input.toUpperCase() + "!!!";
	}

}
