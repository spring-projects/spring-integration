/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.xml.propertyplaceholder;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 *
 * @author Iwein Fuld
 * @author Artem Bilan
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
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
