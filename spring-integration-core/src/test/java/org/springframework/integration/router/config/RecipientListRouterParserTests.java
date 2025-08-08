/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.router.config;

import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.integration.router.RecipientListRouter;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 1.0.3
 */
@SpringJUnitConfig
@DirtiesContext
public class RecipientListRouterParserTests {

	@Autowired
	private ConfigurableApplicationContext context;

	@Autowired
	@Qualifier("routingChannelA")
	private MessageChannel channel;

	@Autowired
	private MessageChannel simpleDynamicInput;

	@Autowired
	private MessageChannel noSelectorMatchInput;

	@Test
	public void checkMessageRouting() {
		Message<?> message = new GenericMessage<>(1);
		channel.send(message);
		PollableChannel chanel1 = (PollableChannel) context.getBean("channel1");
		PollableChannel chanel2 = (PollableChannel) context.getBean("channel2");
		assertThat(chanel1.receive(0).getPayload()).isEqualTo(1);
		assertThat(chanel2.receive(0).getPayload()).isEqualTo(1);
	}

	@Test
	public void simpleRouter() {
		Object endpoint = context.getBean("simpleRouter");
		Object handler = TestUtils.getPropertyValue(endpoint, "handler");
		assertThat(handler.getClass()).isEqualTo(RecipientListRouter.class);
		RecipientListRouter router = (RecipientListRouter) handler;
		DirectFieldAccessor accessor = new DirectFieldAccessor(router);
		assertThat(TestUtils.getPropertyValue(router, "messagingTemplate.sendTimeout")).isEqualTo(45000L);
		assertThat(accessor.getPropertyValue("applySequence")).isEqualTo(Boolean.FALSE);
		assertThat(accessor.getPropertyValue("ignoreSendFailures")).isEqualTo(Boolean.FALSE);
	}

	@Test
	public void customRouter() {
		Object endpoint = context.getBean("customRouter");
		Object handler = TestUtils.getPropertyValue(endpoint, "handler");
		assertThat(handler.getClass()).isEqualTo(RecipientListRouter.class);
		RecipientListRouter router = (RecipientListRouter) handler;
		DirectFieldAccessor accessor = new DirectFieldAccessor(router);
		assertThat(new DirectFieldAccessor(
				accessor.getPropertyValue("messagingTemplate")).getPropertyValue("sendTimeout")).isEqualTo(1234L);
		assertThat(accessor.getPropertyValue("applySequence")).isEqualTo(Boolean.TRUE);
		assertThat(accessor.getPropertyValue("ignoreSendFailures")).isEqualTo(Boolean.TRUE);
	}

	@Test
	public void simpleDynamicRouter() {
		Message<?> message = new GenericMessage<>(1);
		simpleDynamicInput.send(message);
		PollableChannel chanel1 = (PollableChannel) context.getBean("channel1");
		PollableChannel chanel2 = (PollableChannel) context.getBean("channel2");
		assertThat(chanel1.receive(0).getPayload()).isEqualTo(1);
		assertThat(chanel2.receive(0)).isNull();
	}

	@Test
	public void noSelectorMatchRouter() {
		Message<?> message = new GenericMessage<>(1);
		noSelectorMatchInput.send(message);
		PollableChannel chanel1 = (PollableChannel) context.getBean("channel1");
		PollableChannel chanel2 = (PollableChannel) context.getBean("channel2");
		Message<?> output = chanel1.receive(0);
		assertThat(output).isNotNull();
		assertThat(output.getPayload()).isEqualTo(1);
		assertThat(chanel2.receive(0)).isNull();
	}

	public static class TestBean {

		public boolean accept(int number) {
			return number == 1;
		}

	}

}
