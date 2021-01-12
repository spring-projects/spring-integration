/*
 * Copyright 2016-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.channel;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

import java.util.concurrent.Executor;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.MessageDispatchingException;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.support.GenericMessage;

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
		channel.subscribe(m -> { });
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
