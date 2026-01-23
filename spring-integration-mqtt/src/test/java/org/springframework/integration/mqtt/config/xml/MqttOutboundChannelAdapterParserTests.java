/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.mqtt.config.xml;

import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.integration.mqtt.support.MqttMessageConverter;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 * @since 4.0
 */
@SpringJUnitConfig
@DirtiesContext
public class MqttOutboundChannelAdapterParserTests {

	@Autowired
	@Qualifier("withConverter")
	private EventDrivenConsumer withConverterEndpoint;

	@Autowired
	@Qualifier("withConverter.handler")
	private MessageHandler withConverterHandler;

	@Autowired
	@Qualifier("withDefaultConverter.handler")
	private MqttPahoMessageHandler withDefaultConverterHandler;

	@Autowired
	private MqttMessageConverter converter;

	@Autowired
	private DefaultMqttPahoClientFactory clientFactory;

	@SuppressWarnings("unchecked")
	@Test
	public void testWithConverter() throws Exception {
		assertThat(TestUtils.<String>getPropertyValue(withConverterHandler, "url")).isEqualTo("tcp://localhost:1883");
		assertThat(TestUtils.<String>getPropertyValue(withConverterHandler, "clientId")).isEqualTo("foo");
		assertThat(TestUtils.<String>getPropertyValue(withConverterHandler, "defaultTopic")).isEqualTo("bar");
		GenericMessage<String> message = new GenericMessage<>("foo");
		assertThat(TestUtils.<MessageProcessor<?>>getPropertyValue(withConverterHandler, "topicProcessor")
				.processMessage(message)).isEqualTo("bar");
		assertThat(TestUtils.<MessageProcessor<?>>getPropertyValue(withConverterHandler, "qosProcessor")
				.processMessage(message)).isEqualTo(2);
		assertThat(TestUtils.<MessageProcessor<?>>getPropertyValue(withConverterHandler, "retainedProcessor")
				.processMessage(message)).isEqualTo(Boolean.TRUE);
		assertThat(TestUtils.<Object>getPropertyValue(withConverterHandler, "converter")).isSameAs(converter);
		assertThat(TestUtils.<Object>getPropertyValue(withConverterHandler, "clientFactory")).isSameAs(clientFactory);
		assertThat(TestUtils.<Boolean>getPropertyValue(withConverterHandler, "async")).isFalse();
		assertThat(TestUtils.<Boolean>getPropertyValue(withConverterHandler, "asyncEvents")).isFalse();

		Object handler = TestUtils.getPropertyValue(this.withConverterEndpoint, "handler");

		assertThat(AopUtils.isAopProxy(handler)).isTrue();

		assertThat(this.withConverterHandler).isSameAs(((Advised) handler).getTargetSource().getTarget());

		assertThat(((Advised) handler).getAdvisors()[0].getAdvice()).isInstanceOf(RequestHandlerRetryAdvice.class);
	}

	@Test
	public void testWithDefaultConverter() {
		GenericMessage<String> message = new GenericMessage<>("foo");
		assertThat(TestUtils.<String>getPropertyValue(withDefaultConverterHandler, "url")).isEqualTo("tcp://localhost:1883");
		assertThat(TestUtils.<String>getPropertyValue(withDefaultConverterHandler, "clientId")).isEqualTo("foo");
		assertThat(TestUtils.<String>getPropertyValue(withDefaultConverterHandler, "defaultTopic")).isEqualTo("bar");
		assertThat(TestUtils.<Integer>getPropertyValue(withDefaultConverterHandler, "defaultQos")).isEqualTo(1);
		assertThat(TestUtils.<Boolean>getPropertyValue(withDefaultConverterHandler, "defaultRetained"))
				.isEqualTo(Boolean.TRUE);
		DefaultPahoMessageConverter defaultConverter = TestUtils.getPropertyValue(withDefaultConverterHandler, "converter");
		assertThat(defaultConverter.fromMessage(message, null).getQos()).isEqualTo(1);
		assertThat(defaultConverter.fromMessage(message, null).isRetained()).isTrue();
		assertThat(TestUtils.<Object>getPropertyValue(withDefaultConverterHandler, "clientFactory"))
				.isSameAs(clientFactory);
		assertThat(TestUtils.<Boolean>getPropertyValue(withDefaultConverterHandler, "async")).isTrue();
		assertThat(TestUtils.<Boolean>getPropertyValue(withDefaultConverterHandler, "asyncEvents")).isTrue();
	}

}
