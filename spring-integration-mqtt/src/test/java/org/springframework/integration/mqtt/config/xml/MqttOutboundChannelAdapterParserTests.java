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
		assertThat(TestUtils.getPropertyValue(withConverterHandler, "url")).isEqualTo("tcp://localhost:1883");
		assertThat(TestUtils.getPropertyValue(withConverterHandler, "clientId")).isEqualTo("foo");
		assertThat(TestUtils.getPropertyValue(withConverterHandler, "defaultTopic")).isEqualTo("bar");
		GenericMessage<String> message = new GenericMessage<>("foo");
		assertThat(TestUtils.getPropertyValue(withConverterHandler, "topicProcessor", MessageProcessor.class)
				.processMessage(message)).isEqualTo("bar");
		assertThat(TestUtils.getPropertyValue(withConverterHandler, "qosProcessor", MessageProcessor.class)
				.processMessage(message)).isEqualTo(2);
		assertThat(TestUtils.getPropertyValue(withConverterHandler, "retainedProcessor", MessageProcessor.class)
				.processMessage(message)).isEqualTo(Boolean.TRUE);
		assertThat(TestUtils.getPropertyValue(withConverterHandler, "converter")).isSameAs(converter);
		assertThat(TestUtils.getPropertyValue(withConverterHandler, "clientFactory")).isSameAs(clientFactory);
		assertThat(TestUtils.getPropertyValue(withConverterHandler, "async", Boolean.class)).isFalse();
		assertThat(TestUtils.getPropertyValue(withConverterHandler, "asyncEvents", Boolean.class)).isFalse();

		Object handler = TestUtils.getPropertyValue(this.withConverterEndpoint, "handler");

		assertThat(AopUtils.isAopProxy(handler)).isTrue();

		assertThat(this.withConverterHandler).isSameAs(((Advised) handler).getTargetSource().getTarget());

		assertThat(((Advised) handler).getAdvisors()[0].getAdvice()).isInstanceOf(RequestHandlerRetryAdvice.class);
	}

	@Test
	public void testWithDefaultConverter() {
		GenericMessage<String> message = new GenericMessage<>("foo");
		assertThat(TestUtils.getPropertyValue(withDefaultConverterHandler, "url")).isEqualTo("tcp://localhost:1883");
		assertThat(TestUtils.getPropertyValue(withDefaultConverterHandler, "clientId")).isEqualTo("foo");
		assertThat(TestUtils.getPropertyValue(withDefaultConverterHandler, "defaultTopic")).isEqualTo("bar");
		assertThat(TestUtils.getPropertyValue(withDefaultConverterHandler, "defaultQos")).isEqualTo(1);
		assertThat(TestUtils.getPropertyValue(withDefaultConverterHandler, "defaultRetained", Boolean.class))
				.isEqualTo(Boolean.TRUE);
		DefaultPahoMessageConverter defaultConverter = TestUtils.getPropertyValue(withDefaultConverterHandler,
				"converter", DefaultPahoMessageConverter.class);
		assertThat(defaultConverter.fromMessage(message, null).getQos()).isEqualTo(1);
		assertThat(defaultConverter.fromMessage(message, null).isRetained()).isTrue();
		assertThat(TestUtils.getPropertyValue(withDefaultConverterHandler, "clientFactory")).isSameAs(clientFactory);
		assertThat(TestUtils.getPropertyValue(withDefaultConverterHandler, "async", Boolean.class)).isTrue();
		assertThat(TestUtils.getPropertyValue(withDefaultConverterHandler, "asyncEvents", Boolean.class)).isTrue();
	}

}
