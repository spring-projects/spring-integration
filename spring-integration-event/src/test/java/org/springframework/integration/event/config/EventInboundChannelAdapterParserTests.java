/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.integration.event.config;

import java.util.Date;
import java.util.Properties;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.ResolvableType;
import org.springframework.expression.Expression;
import org.springframework.integration.event.inbound.ApplicationEventListeningMessageProducer;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Gary Russell
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 * @since 2.0
 */
@SpringJUnitConfig
@ContextConfiguration
@TestExecutionListeners(DependencyInjectionTestExecutionListener.class)
public class EventInboundChannelAdapterParserTests {

	@Autowired
	private ApplicationContext context;

	@Autowired
	MessageChannel errorChannel;

	@Autowired
	MessageChannel autoChannel;

	@Autowired
	@Qualifier("autoChannel.adapter")
	ApplicationEventListeningMessageProducer eventListener;

	@Test
	public void validateEventParser() {
		Object adapter = context.getBean("eventAdapterSimple");
		assertThat(adapter).isNotNull();
		assertThat(adapter instanceof ApplicationEventListeningMessageProducer).isTrue();
		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapter);
		assertThat(adapterAccessor.getPropertyValue("outputChannel")).isEqualTo(context.getBean("input"));
		assertThat(adapterAccessor.getPropertyValue("errorChannel")).isSameAs(errorChannel);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void validateEventParserWithEventTypes() {
		Object adapter = context.getBean("eventAdapterFiltered");
		assertThat(adapter).isNotNull();
		assertThat(adapter instanceof ApplicationEventListeningMessageProducer).isTrue();
		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapter);
		assertThat(adapterAccessor.getPropertyValue("outputChannel")).isEqualTo(context.getBean("inputFiltered"));
		Set<ResolvableType> eventTypes = (Set<ResolvableType>) adapterAccessor.getPropertyValue("eventTypes");
		assertThat(eventTypes).isNotNull();
		assertThat(eventTypes.size() == 3).isTrue();
		assertThat(eventTypes.contains(ResolvableType.forClass(SampleEvent.class))).isTrue();
		assertThat(eventTypes.contains(ResolvableType.forClass(AnotherSampleEvent.class))).isTrue();
		assertThat(eventTypes.contains(ResolvableType.forClass(Date.class))).isTrue();
		assertThat(adapterAccessor.getPropertyValue("errorChannel")).isNull();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void validateEventParserWithEventTypesAndPlaceholder() {
		Object adapter = context.getBean("eventAdapterFilteredPlaceHolder");
		assertThat(adapter).isNotNull();
		assertThat(adapter instanceof ApplicationEventListeningMessageProducer).isTrue();
		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapter);
		assertThat(adapterAccessor.getPropertyValue("outputChannel"))
				.isEqualTo(context.getBean("inputFilteredPlaceHolder"));
		Set<ResolvableType> eventTypes = (Set<ResolvableType>) adapterAccessor.getPropertyValue("eventTypes");
		assertThat(eventTypes).isNotNull();
		assertThat(eventTypes.size() == 2).isTrue();
		assertThat(eventTypes.contains(ResolvableType.forClass(SampleEvent.class))).isTrue();
		assertThat(eventTypes.contains(ResolvableType.forClass(AnotherSampleEvent.class))).isTrue();
	}

	@Test
	public void validateUsageWithHistory() {
		PollableChannel channel = context.getBean("input", PollableChannel.class);
		assertThat(channel.receive(0).getPayload().getClass()).isEqualTo(ContextRefreshedEvent.class);
		context.publishEvent(new SampleEvent("hello"));
		Message<?> message = channel.receive(0);
		MessageHistory history = MessageHistory.read(message);
		assertThat(history).isNotNull();
		Properties componentHistoryRecord = TestUtils.locateComponentInHistory(history, "eventAdapterSimple", 0);
		assertThat(componentHistoryRecord).isNotNull();
		assertThat(componentHistoryRecord.get("type")).isEqualTo("event:inbound-channel-adapter");
		assertThat(message).isNotNull();
		assertThat(message.getPayload().getClass()).isEqualTo(SampleEvent.class);
	}

	@Test
	public void validatePayloadExpression() {
		Object adapter = context.getBean("eventAdapterSpel");
		assertThat(adapter).isNotNull();
		assertThat(adapter instanceof ApplicationEventListeningMessageProducer).isTrue();
		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapter);
		Expression expression = (Expression) adapterAccessor.getPropertyValue("payloadExpression");
		assertThat(expression.getExpressionString()).isEqualTo("source + '-test'");
	}

	@Test
	public void testAutoCreateChannel() {
		assertThat(TestUtils.getPropertyValue(eventListener, "outputChannel")).isSameAs(autoChannel);
	}

	@SuppressWarnings("serial")
	public static class SampleEvent extends ApplicationEvent {

		public SampleEvent(Object source) {
			super(source);
		}

	}

	@SuppressWarnings("serial")
	public static class AnotherSampleEvent extends ApplicationEvent {

		public AnotherSampleEvent(Object source) {
			super(source);
		}

	}

}
