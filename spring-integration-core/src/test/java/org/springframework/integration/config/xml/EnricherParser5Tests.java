/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the error-channel in an enricher to produce
 * a default object in case of downstream failure.
 *
 * @author Kris Jacyna
 * @author Artem Bilan
 *
 * @since 4.1
 */
@SpringJUnitConfig
@DirtiesContext
public class EnricherParser5Tests {

	@Autowired
	private ApplicationContext context;

	@Test
	public void errorChannelTest() {

		class ErrorThrower extends AbstractReplyProducingMessageHandler {

			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				throw new RuntimeException();
			}

		}

		class DefaultTargetProducer extends AbstractReplyProducingMessageHandler {

			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				final Target defaultTarget = new Target();
				defaultTarget.setName("Default");
				return defaultTarget;
			}

		}

		context.getBean("requestChannel", DirectChannel.class).subscribe(new ErrorThrower());
		context.getBean("errChannel", DirectChannel.class).subscribe(new DefaultTargetProducer());

		Target original = new Target();
		original.setName("John");
		Message<?> request = MessageBuilder.withPayload(original).build();

		context.getBean("inputChannel", DirectChannel.class).send(request);

		Message<?> reply = context.getBean("outputChannel", PollableChannel.class).receive(10000);
		Target enriched = (Target) reply.getPayload();
		assertThat(enriched.getName()).isEqualTo("Mr. Default");
	}

	public static class Target {

		private volatile String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

}
