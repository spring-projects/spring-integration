/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.handler;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;

import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Iwein Fuld
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Marius Bogoevici
 * @author Artem Bilan
 * @author Oleg Zhurakousky
 */
@RunWith(MockitoJUnitRunner.class)
public class AbstractReplyProducingMessageHandlerTests {

	private final AbstractReplyProducingMessageHandler handler = new AbstractReplyProducingMessageHandler() {

		@Override
		protected Object handleRequestMessage(Message<?> requestMessage) {
			return requestMessage;
		}

	};

	private final Message<?> message = MessageBuilder.withPayload("test").build();

	@Mock
	private final MessageChannel channel = null;


	@Test
	public void errorMessageShouldContainChannelName() {
		this.handler.setOutputChannel(this.channel);
		given(this.channel.send(this.message)).willReturn(false);
		given(this.channel.toString()).willReturn("testChannel");
		try {
			this.handler.handleMessage(this.message);
			fail("Expected a MessagingException");
		}
		catch (MessagingException e) {
			assertThat(e.getMessage(), containsString("'testChannel'"));
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testNotPropagate() {
		AbstractReplyProducingMessageHandler handler = new AbstractReplyProducingMessageHandler() {

			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				return new GenericMessage<>("world", Collections.singletonMap("bar", "RAB"));
			}

		};
		assertThat(handler.getNotPropagatedHeaders(), emptyCollectionOf(String.class));
		handler.setNotPropagatedHeaders("f*", "*r");
		handler.setOutputChannel(this.channel);
		assertThat(handler.getNotPropagatedHeaders(), containsInAnyOrder("f*", "*r"));
		ArgumentCaptor<Message<?>> captor = ArgumentCaptor.forClass(Message.class);
		willReturn(true).given(this.channel).send(captor.capture());
		handler.handleMessage(MessageBuilder.withPayload("hello")
				.setHeader("foo", "FOO")
				.setHeader("bar", "BAR")
				.setHeader("baz", "BAZ")
				.build());
		Message<?> out = captor.getValue();
		assertThat(out, notNullValue());
		assertThat(out.getHeaders().get("foo"), nullValue());
		assertThat(out.getHeaders().get("bar"), equalTo("RAB"));
		assertThat(out.getHeaders().get("baz"), equalTo("BAZ"));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testNotPropagateAddWhenNonExist() {
		AbstractReplyProducingMessageHandler handler = new AbstractReplyProducingMessageHandler() {

			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				return new GenericMessage<>("world", Collections.singletonMap("bar", "RAB"));
			}

		};
		handler.addNotPropagatedHeaders("boom");
		assertThat(handler.getNotPropagatedHeaders(), containsInAnyOrder("boom"));
		handler.setOutputChannel(this.channel);
		ArgumentCaptor<Message<?>> captor = ArgumentCaptor.forClass(Message.class);
		willReturn(true).given(this.channel).send(captor.capture());
		handler.handleMessage(MessageBuilder.withPayload("hello")
				.setHeader("boom", "FOO")
				.setHeader("bar", "BAR")
				.setHeader("baz", "BAZ")
				.build());
		Message<?> out = captor.getValue();
		assertThat(out, notNullValue());
		assertThat(out.getHeaders().get("boom"), nullValue());
		assertThat(out.getHeaders().get("bar"), equalTo("RAB"));
		assertThat(out.getHeaders().get("baz"), equalTo("BAZ"));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testNotPropagateAdd() {
		AbstractReplyProducingMessageHandler handler = new AbstractReplyProducingMessageHandler() {

			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				return new GenericMessage<>("world", Collections.singletonMap("bar", "RAB"));
			}

		};
		assertThat(handler.getNotPropagatedHeaders(), emptyCollectionOf(String.class));
		handler.setNotPropagatedHeaders("foo");
		handler.addNotPropagatedHeaders("b*r");
		handler.setOutputChannel(this.channel);
		assertThat(handler.getNotPropagatedHeaders(), containsInAnyOrder("foo", "b*r"));
		ArgumentCaptor<Message<?>> captor = ArgumentCaptor.forClass(Message.class);
		willReturn(true).given(this.channel).send(captor.capture());
		handler.handleMessage(
				MessageBuilder.withPayload("hello")
						.setHeader("foo", "FOO")
						.setHeader("bar", "BAR")
						.setHeader("baz", "BAZ")
						.build());
		Message<?> out = captor.getValue();
		assertThat(out, notNullValue());
		assertThat(out.getHeaders().get("foo"), nullValue());
		assertThat(out.getHeaders().get("bar"), equalTo("RAB"));
		assertThat(out.getHeaders().get("baz"), equalTo("BAZ"));
	}

}
