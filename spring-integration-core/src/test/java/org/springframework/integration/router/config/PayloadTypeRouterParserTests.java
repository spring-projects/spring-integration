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

package org.springframework.integration.router.config;

import java.io.ByteArrayInputStream;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.InputStreamResource;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 */
@SpringJUnitConfig
@DirtiesContext
public class PayloadTypeRouterParserTests {

	@Autowired
	private ConfigurableApplicationContext context;

	@Autowired
	private TestService testService;

	@Test
	public void testPayloadTypeRouter() {
		context.start();
		Message<?> message1 = MessageBuilder.withPayload("Hello").build();
		Message<?> message2 = MessageBuilder.withPayload(25).build();
		Message<?> message3 = MessageBuilder.withPayload(new Integer[] {23, 24, 34}).build();
		Message<?> message4 = MessageBuilder.withPayload(new Long[] {23L, 24L, 34L}).build();
		testService.foo(message1);
		testService.foo(message2);
		testService.foo(message3);
		testService.foo(message4);
		PollableChannel chanel1 = (PollableChannel) context.getBean("channel1");
		PollableChannel chanel2 = (PollableChannel) context.getBean("channel2");
		PollableChannel chanel3 = (PollableChannel) context.getBean("channel3");
		PollableChannel chanel4 = (PollableChannel) context.getBean("channel4");
		assertThat(chanel1.receive(100).getPayload()).isInstanceOf(String.class);
		assertThat(chanel2.receive(100).getPayload()).isInstanceOf(Integer.class);
		assertThat(chanel3.receive(100).getPayload().getClass().isArray()).isTrue();
		assertThat(chanel4.receive(100).getPayload().getClass().isArray()).isTrue();
	}

	@Test
	public void testNoMappingElement() {
		ByteArrayInputStream stream = new ByteArrayInputStream(routerConfigNoMapping.getBytes());
		GenericApplicationContext ac = new GenericApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(ac);
		reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
		assertThatExceptionOfType(BeanDefinitionStoreException.class)
				.isThrownBy(() -> reader.loadBeanDefinitions(new InputStreamResource(stream)));
	}

	private static final String routerConfigNoMapping =
			"""
					<?xml version="1.0" encoding="UTF-8"?>
					<beans:beans xmlns="http://www.springframework.org/schema/integration"
						xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
						xmlns:beans="http://www.springframework.org/schema/beans"
						xsi:schemaLocation="http://www.springframework.org/schema/beans
							https://www.springframework.org/schema/beans/spring-beans.xsd
							http://www.springframework.org/schema/integration
							https://www.springframework.org/schema/integration/spring-integration.xsd">

						<channel id="routingChannel" />
						<payload-type-router input-channel="routingChannel"/>
					</beans:beans>
					""";

	public interface TestService {

		void foo(Message<?> message);

	}

}
