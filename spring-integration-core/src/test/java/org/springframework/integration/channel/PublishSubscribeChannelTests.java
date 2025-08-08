/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.channel;

import java.util.concurrent.Executor;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.MessageDispatchingException;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.0
 *
 */
public class PublishSubscribeChannelTests {

	@Test
	public void testEarlySubscribe() {
		PublishSubscribeChannel channel = new PublishSubscribeChannel(mock(Executor.class));
		channel.subscribe(m -> {
		});
		channel.setBeanFactory(mock(BeanFactory.class));
		assertThatIllegalStateException()
				.isThrownBy(channel::afterPropertiesSet)
				.withMessage("When providing an Executor, you cannot subscribe() until the channel "
						+ "bean is fully initialized by the framework. Do not subscribe in a @Bean definition");
	}

	@Test
	public void testRequireSubscribers() {
		PublishSubscribeChannel channel = new PublishSubscribeChannel(true);
		assertThatExceptionOfType(MessageDeliveryException.class)
				.isThrownBy(() -> channel.send(new GenericMessage<>("test")))
				.withCauseInstanceOf(MessageDispatchingException.class)
				.withMessageContaining("Dispatcher has no subscribers");
	}

}
