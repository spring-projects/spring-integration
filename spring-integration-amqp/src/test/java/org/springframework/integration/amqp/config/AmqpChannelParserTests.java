/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.amqp.config;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.listener.DirectMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.amqp.channel.AbstractAmqpChannel;
import org.springframework.integration.amqp.channel.PointToPointSubscribableAmqpChannel;
import org.springframework.integration.amqp.channel.PollableAmqpChannel;
import org.springframework.integration.amqp.channel.PublishSubscribeAmqpChannel;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.1
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class AmqpChannelParserTests {

	@Autowired
	private ApplicationContext context;

	@Autowired
	private PollableAmqpChannel pollableWithEP;

	@Autowired
	private PointToPointSubscribableAmqpChannel withEP;

	@Autowired
	private PublishSubscribeAmqpChannel pubSubWithEP;

	@Test
	public void interceptor() {
		MessageChannel channel = context.getBean("channelWithInterceptor", MessageChannel.class);
		List<?> interceptorList = TestUtils.getPropertyValue(channel, "interceptors.interceptors", List.class);
		assertThat(interceptorList.size()).isEqualTo(1);
		assertThat(interceptorList.get(0).getClass()).isEqualTo(TestInterceptor.class);
		assertThat(TestUtils.getPropertyValue(
				TestUtils.getPropertyValue(channel, "dispatcher"), "maxSubscribers", Integer.class).intValue())
				.isEqualTo(Integer.MAX_VALUE);
		channel = context.getBean("pubSub", MessageChannel.class);
		Object mbf = context.getBean(IntegrationUtils.INTEGRATION_MESSAGE_BUILDER_FACTORY_BEAN_NAME);
		assertThat(TestUtils.getPropertyValue(channel, "container.messageListener.messageBuilderFactory"))
				.isSameAs(mbf);
		assertThat(TestUtils.getPropertyValue(channel, "container.missingQueuesFatal", Boolean.class)).isTrue();
		assertThat(TestUtils.getPropertyValue(channel, "container.transactional", Boolean.class)).isFalse();
		assertThat(TestUtils.getPropertyValue(channel, "amqpTemplate.transactional", Boolean.class)).isFalse();
		assertThat(TestUtils.getPropertyValue(channel, "container")).isInstanceOf(SimpleMessageListenerContainer.class);
	}

	@Test
	public void subscriberLimit() {
		MessageChannel channel = context.getBean("channelWithSubscriberLimit", MessageChannel.class);
		assertThat(TestUtils.getPropertyValue(
				TestUtils.getPropertyValue(channel, "dispatcher"), "maxSubscribers", Integer.class).intValue())
				.isEqualTo(1);
		assertThat(TestUtils.getPropertyValue(channel, "container.missingQueuesFatal", Boolean.class)).isFalse();
		assertThat(TestUtils.getPropertyValue(channel, "container.transactional", Boolean.class)).isFalse();
		assertThat(TestUtils.getPropertyValue(channel, "amqpTemplate.transactional", Boolean.class)).isTrue();
		assertThat(TestUtils.getPropertyValue(channel, "extractPayload", Boolean.class)).isFalse();
		assertThat(TestUtils.getPropertyValue(channel, "container")).isInstanceOf(DirectMessageListenerContainer.class);
		assertThat(TestUtils.getPropertyValue(channel, "container.consumersPerQueue")).isEqualTo(2);
	}

	@Test
	public void testMapping() {
		checkExtract(this.pollableWithEP);
		checkExtract(this.withEP);
		checkExtract(this.pubSubWithEP);
		assertThat(TestUtils.getPropertyValue(this.withEP, "defaultDeliveryMode"))
				.isEqualTo(MessageDeliveryMode.NON_PERSISTENT);
		assertThat(TestUtils.getPropertyValue(this.withEP, "headersMappedLast", Boolean.class)).isFalse();
		assertThat(TestUtils.getPropertyValue(this.pollableWithEP, "defaultDeliveryMode")).isNull();
		assertThat(TestUtils.getPropertyValue(this.pollableWithEP, "headersMappedLast", Boolean.class)).isTrue();
	}

	private void checkExtract(AbstractAmqpChannel channel) {
		assertThat(TestUtils.getPropertyValue(channel, "outboundHeaderMapper").toString())
				.contains("Mock for AmqpHeaderMapper");
		assertThat(TestUtils.getPropertyValue(channel, "inboundHeaderMapper").toString())
				.contains("Mock for AmqpHeaderMapper");
		assertThat(TestUtils.getPropertyValue(channel, "extractPayload", Boolean.class)).isTrue();
	}

	private static class TestInterceptor implements ChannelInterceptor {

	}

}
