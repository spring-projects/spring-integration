/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Gary Russell
 * @since 2.2
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class DispatcherMaxSubscribersOverrideDefaultTests extends DispatcherMaxSubscribersTests {

	@Autowired
	private SubscribableChannel oneSub;

	@Test
	public void test() {
		this.doTestUnicast(456, 456, 123, 456, 234);
		doTestMulticast(789, 2);
	}

	@Test
	public void testExceed() {
		oneSub.subscribe(message -> {
		});
		try {
			oneSub.subscribe(message -> {
			});
			fail("Expected Exception");
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("Maximum subscribers exceeded");
		}
	}

}
