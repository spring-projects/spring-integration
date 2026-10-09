/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Glenn Renfro
 */
@SpringJUnitConfig
@DirtiesContext
public class ErrorChannelAutoCreationTests {

	@Autowired
	private MessageChannel errorChannel;

	@Test
	public void testErrorChannelIsPubSub() {
		assertThat(this.errorChannel).isInstanceOf(PublishSubscribeChannel.class);
		assertThat(TestUtils.<Boolean>getPropertyValue(this.errorChannel, "dispatcher.requireSubscribers"))
				.isTrue();
		assertThat(TestUtils.<Boolean>getPropertyValue(this.errorChannel, "dispatcher.ignoreFailures"))
				.isTrue();

		@SuppressWarnings("unchecked")
		Set<MessageHandler> handlers =
				TestUtils.getPropertyValue(this.errorChannel, "dispatcher.handlers");

		assertThat(handlers).first()
				.isInstanceOf(LoggingHandler.class)
				.extracting("order")
				.isEqualTo(Ordered.LOWEST_PRECEDENCE - 100);
	}

}
