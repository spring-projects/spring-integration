/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.xml.propertyplaceholder;

import org.junit.jupiter.api.Test;

import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 *
 * @author Iwein Fuld
 * @author Artem Bilan
 */
@SpringJUnitConfig
public class PropertyPlaceholderTests {

	@Test
	public void context() {
		//parsing and instantiating is enough
	}

	public static class SanityCheck {

		public SanityCheck(Integer i) {
			//this will throw an exception if the placeholder isn't replaced
		}

	}

}
