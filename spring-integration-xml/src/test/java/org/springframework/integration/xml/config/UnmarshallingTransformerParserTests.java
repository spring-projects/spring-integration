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

package org.springframework.integration.xml.config;

import java.util.List;

import javax.xml.transform.dom.DOMSource;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.support.SmartLifecycleRoleController;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.xml.util.XmlTestUtil;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.MultiValueMap;
import org.springframework.xml.transform.StringSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Jonas Partner
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 * @author Glenn Renfro
 */
@SpringJUnitConfig
@DirtiesContext
public class UnmarshallingTransformerParserTests {

	@Autowired
	private ApplicationContext appContext;

	@Autowired
	private StubUnmarshaller unmarshaller;

	@Test
	public void testParse() {
		EventDrivenConsumer consumer = (EventDrivenConsumer) appContext.getBean("parseOnly");
		assertThat(TestUtils.<Integer>getPropertyValue(consumer, "handler.order")).isEqualTo(2);
		assertThat(TestUtils.<Long>getPropertyValue(consumer, "handler.messagingTemplate.sendTimeout")).isEqualTo(123L);
		assertThat(TestUtils.<Integer>getPropertyValue(consumer, "phase")).isEqualTo(-1);
		assertThat(TestUtils.<Boolean>getPropertyValue(consumer, "autoStartup")).isFalse();
		SmartLifecycleRoleController roleController = appContext.getBean(SmartLifecycleRoleController.class);
		@SuppressWarnings("unchecked")
		List<SmartLifecycle> list = (List<SmartLifecycle>) TestUtils.<MultiValueMap<?, ?>>getPropertyValue(
				roleController, "lifecycles").get("foo");
		assertThat(list).containsExactly(consumer);
	}

	@Test
	public void testDefaultUnmarshall() {
		MessageChannel input = (MessageChannel) appContext.getBean("input");
		PollableChannel output = (PollableChannel) appContext.getBean("output");
		GenericMessage<Object> message = new GenericMessage<>(new StringSource("""
				<?xml version="1.0" encoding="ISO-8859-1"?>
				<order>
					<orderItem>test</orderItem>
				</order>
				"""));
		input.send(message);
		Message<?> result = output.receive(0);
		assertThat(result.getPayload()).as("Wrong payload after unmarshalling").isEqualTo("unmarshalled");
		assertThat(unmarshaller.sourcesPassed.poll()).isInstanceOf(StringSource.class);
	}

	@Test
	public void testUnmarshallString() {
		MessageChannel input = (MessageChannel) appContext.getBean("input");
		PollableChannel output = (PollableChannel) appContext.getBean("output");
		GenericMessage<Object> message = new GenericMessage<>("""
				<?xml version="1.0" encoding="ISO-8859-1"?>
				<order>
					<orderItem>test</orderItem>
				</order>
				""");
		input.send(message);
		Message<?> result = output.receive(0);
		assertThat(result.getPayload()).as("Wrong payload after unmarshalling").isEqualTo("unmarshalled");
		assertThat(unmarshaller.sourcesPassed.poll()).isInstanceOf(StringSource.class);
	}

	@Test
	public void testUnmarshallDocument() throws Exception {
		MessageChannel input = (MessageChannel) appContext.getBean("input");
		PollableChannel output = (PollableChannel) appContext.getBean("output");
		GenericMessage<Object> message = new GenericMessage<>(
				XmlTestUtil.getDocumentForString("""
						<?xml version="1.0" encoding="ISO-8859-1"?>
						<order>
							<orderItem>test</orderItem>
						</order>
						"""));
		input.send(message);
		Message<?> result = output.receive(0);
		assertThat(result.getPayload()).as("Wrong payload after unmarshalling").isEqualTo("unmarshalled");
		assertThat(unmarshaller.sourcesPassed.poll()).isInstanceOf(DOMSource.class);
	}

	@Test
	public void testPollingUnmarshall() {
		MessageChannel input = (MessageChannel) appContext.getBean("pollableInput");
		PollableChannel output = (PollableChannel) appContext.getBean("output");
		GenericMessage<Object> message = new GenericMessage<>(new StringSource("""
				<?xml version="1.0" encoding="ISO-8859-1"?>
				<order>
					<orderItem>test</orderItem>
				</order>
				"""));
		input.send(message);
		Message<?> result = output.receive(5000);
		assertThat(result.getPayload()).as("Wrong payload after unmarshalling").isEqualTo("unmarshalled");
		assertThat(unmarshaller.sourcesPassed.poll()).isInstanceOf(StringSource.class);
	}

	@Test
	public void testUnmarshallUnsupported() {
		MessageChannel input = (MessageChannel) appContext.getBean("input");
		GenericMessage<Object> message = new GenericMessage<>(new StringBuffer("""
				<?xml version="1.0" encoding="ISO-8859-1"?>
				<order>
					<orderItem>test</orderItem>
				</order>
				"""));
		assertThatExceptionOfType(MessagingException.class)
				.isThrownBy(() -> input.send(message));
	}

}
