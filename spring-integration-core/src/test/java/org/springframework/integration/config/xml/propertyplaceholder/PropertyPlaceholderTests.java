/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
