/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.2
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class DispatcherMaxSubscribersDefaultConfigurationTests extends DispatcherMaxSubscribersTests {

	@Test
	public void test() {
		doTestUnicast(Integer.MAX_VALUE, Integer.MAX_VALUE, 123, Integer.MAX_VALUE, 234);
		doTestMulticast(Integer.MAX_VALUE, 2);
	}

}
