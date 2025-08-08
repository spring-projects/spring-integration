/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.router.config;

import org.junit.Test;

import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.RouterFactoryBean;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @since 4.2.5
 *
 */
public class RouterFactoryBeanTests {

	private boolean routeAttempted;

	@Test
	public void testOutputChannelName() throws Exception {
		TestUtils.TestApplicationContext testApplicationContext = TestUtils.createTestApplicationContext();
		testApplicationContext.refresh();
		RouterFactoryBean fb = new RouterFactoryBean();
		fb.setTargetObject(this);
		fb.setTargetMethodName("foo");
		fb.setDefaultOutputChannelName("bar");
		QueueChannel bar = new QueueChannel();
		testApplicationContext.registerBean("bar", bar);
		fb.setBeanFactory(testApplicationContext);
		MessageHandler handler = fb.getObject();
		this.routeAttempted = false;
		handler.handleMessage(new GenericMessage<>("foo"));
		assertThat(bar.receive(10000)).isNotNull();
		assertThat(this.routeAttempted).isTrue();
		testApplicationContext.close();
	}

	public String foo() {
		routeAttempted = true;
		return null;
	}

}
