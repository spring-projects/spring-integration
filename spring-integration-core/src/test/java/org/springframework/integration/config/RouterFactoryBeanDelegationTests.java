/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.router.AbstractMappingMessageRouter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class RouterFactoryBeanDelegationTests {

	@Autowired
	@Qualifier("input")
	private MessageChannel input;

	@Autowired
	@Qualifier("strings")
	private PollableChannel strings;

	@Autowired
	@Qualifier("discard")
	private PollableChannel discard;

	@Autowired
	@Qualifier("org.springframework.integration.config.RouterFactoryBean#0")
	private AbstractMappingMessageRouter router;

	@Test
	public void checkResolutionRequiredConfiguredOnTargetRouter() {
		@SuppressWarnings("unchecked")
		boolean resolutionRequired = (Boolean) new DirectFieldAccessor(router).getPropertyValue("resolutionRequired");
		assertThat(resolutionRequired).as("The 'resolutionRequired' property should be 'true'").isTrue();
	}

	@Test
	public void routeWithMappedType() {
		input.send(new GenericMessage<>("test"));
		assertThat(discard.receive(0)).isNull();
		assertThat(strings.receive(0)).isNotNull();
	}

	@Test
	public void routeWithUnmappedType() {
		input.send(new GenericMessage<>(123));
		assertThat(strings.receive(0)).isNull();
		assertThat(discard.receive(0)).isNotNull();
	}

}
