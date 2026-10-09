/*
 * Copyright 2002-present the original author or authors.
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
