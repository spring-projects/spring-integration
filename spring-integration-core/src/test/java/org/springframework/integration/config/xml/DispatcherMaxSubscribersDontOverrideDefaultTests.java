/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import org.junit.jupiter.api.Test;

import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.2
 *
 */
@SpringJUnitConfig
public class DispatcherMaxSubscribersDontOverrideDefaultTests extends DispatcherMaxSubscribersTests {

	@Test
	public void test() {
		doTestUnicast(Integer.MAX_VALUE, Integer.MAX_VALUE, 123, Integer.MAX_VALUE, 234);
		doTestMulticast(Integer.MAX_VALUE, 2);
	}

}
