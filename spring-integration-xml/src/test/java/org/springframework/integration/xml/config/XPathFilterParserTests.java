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

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.SmartLifecycleRoleController;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.xml.util.XmlTestUtil;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 * @since 2.1
 */
@SpringJUnitConfig
@DirtiesContext
public class XPathFilterParserTests {

	@Autowired
	private ApplicationContext context;

	@Test
	public void testParse() {
		EventDrivenConsumer consumer = (EventDrivenConsumer) context.getBean("parseOnly");
		assertThat(TestUtils.<Integer>getPropertyValue(consumer, "handler.order")).isEqualTo(2);
		assertThat(TestUtils.<Long>getPropertyValue(consumer, "handler.messagingTemplate.sendTimeout")).isEqualTo(123L);
		assertThat(TestUtils.<Integer>getPropertyValue(consumer, "phase")).isEqualTo(-1);
		assertThat(TestUtils.<Boolean>getPropertyValue(consumer, "autoStartup")).isFalse();
		SmartLifecycleRoleController roleController = context.getBean(SmartLifecycleRoleController.class);
		@SuppressWarnings("unchecked")
		List<SmartLifecycle> list = (List<SmartLifecycle>) TestUtils.<MultiValueMap<?, ?>>getPropertyValue(
				roleController, "lifecycles").get("foo");
		assertThat(list).containsExactly(consumer);
	}

	@Test
	public void simpleStringExpressionBoolean() {
		MessageChannel inputChannel = context.getBean("booleanFilterInput", MessageChannel.class);
		QueueChannel replyChannel = new QueueChannel();
		PollableChannel discardChannel = context.getBean("booleanFilterRejections", PollableChannel.class);
		Message<?> shouldBeAccepted =
				MessageBuilder.withPayload("<name>outputOne</name>")
						.setReplyChannel(replyChannel)
						.build();
		Message<?> shouldBeRejected =
				MessageBuilder.withPayload("<other>outputOne</other>")
						.setReplyChannel(replyChannel)
						.build();
		inputChannel.send(shouldBeAccepted);
		inputChannel.send(shouldBeRejected);
		assertThat(replyChannel.receive(0)).isEqualTo(shouldBeAccepted);
		assertThat(discardChannel.receive(0)).isEqualTo(shouldBeRejected);
		assertThat(replyChannel.receive(0)).isNull();
		assertThat(discardChannel.receive(0)).isNull();
	}

	@Test
	public void stringExpressionWithNamespaceBoolean() throws Exception {
		MessageChannel inputChannel = context.getBean("booleanFilterWithNamespaceInput", MessageChannel.class);
		QueueChannel replyChannel = new QueueChannel();
		PollableChannel discardChannel = context.getBean("booleanFilterWithNamespaceRejections", PollableChannel.class);
		var docToAccept = XmlTestUtil.getDocumentForString("<ns1:name xmlns:ns1='www.example.org'>outputOne</ns1:name>");
		var docToReject = XmlTestUtil.getDocumentForString("<name>outputOne</name>");
		Message<?> shouldBeAccepted = MessageBuilder.withPayload(docToAccept).setReplyChannel(replyChannel).build();
		Message<?> shouldBeRejected = MessageBuilder.withPayload(docToReject).setReplyChannel(replyChannel).build();
		inputChannel.send(shouldBeAccepted);
		inputChannel.send(shouldBeRejected);
		assertThat(replyChannel.receive(0)).isEqualTo(shouldBeAccepted);
		assertThat(discardChannel.receive(0)).isEqualTo(shouldBeRejected);
		assertThat(replyChannel.receive(0)).isNull();
		assertThat(discardChannel.receive(0)).isNull();
	}

	@Test
	public void stringExpressionWithNestedMapBoolean() throws Exception {
		MessageChannel inputChannel = context.getBean("nestedNamespaceMapFilterInput", MessageChannel.class);
		QueueChannel replyChannel = new QueueChannel();
		PollableChannel discardChannel = context.getBean("nestedNamespaceMapFilterRejections", PollableChannel.class);
		var docToAccept = XmlTestUtil.getDocumentForString("<ns1:name xmlns:ns1='www.example.org'>outputOne</ns1:name>");
		var docToReject = XmlTestUtil.getDocumentForString("<name>outputOne</name>");
		Message<?> shouldBeAccepted = MessageBuilder.withPayload(docToAccept).setReplyChannel(replyChannel).build();
		Message<?> shouldBeRejected = MessageBuilder.withPayload(docToReject).setReplyChannel(replyChannel).build();
		inputChannel.send(shouldBeAccepted);
		inputChannel.send(shouldBeRejected);
		assertThat(replyChannel.receive(0)).isEqualTo(shouldBeAccepted);
		assertThat(discardChannel.receive(0)).isEqualTo(shouldBeRejected);
		assertThat(replyChannel.receive(0)).isNull();
		assertThat(discardChannel.receive(0)).isNull();
	}

	@Test
	public void stringExpressionWithNamespaceString() throws Exception {
		MessageChannel inputChannel = context.getBean("stringFilterWithNamespaceInput", MessageChannel.class);
		QueueChannel replyChannel = new QueueChannel();
		PollableChannel discardChannel = context.getBean("stringFilterWithNamespaceRejections", PollableChannel.class);
		var docToAccept = XmlTestUtil.getDocumentForString("<ns1:name xmlns:ns1='www.example.org'>outputOne</ns1:name>");
		var docToReject = XmlTestUtil.getDocumentForString("<name>outputOne</name>");
		Message<?> shouldBeAccepted = MessageBuilder.withPayload(docToAccept).setReplyChannel(replyChannel).build();
		Message<?> shouldBeRejected = MessageBuilder.withPayload(docToReject).setReplyChannel(replyChannel).build();
		inputChannel.send(shouldBeAccepted);
		inputChannel.send(shouldBeRejected);
		assertThat(replyChannel.receive(0)).isEqualTo(shouldBeAccepted);
		assertThat(discardChannel.receive(0)).isEqualTo(shouldBeRejected);
		assertThat(replyChannel.receive(0)).isNull();
		assertThat(discardChannel.receive(0)).isNull();
	}

	@Test
	public void stringExpressionIgnoresCase() throws Exception {
		MessageChannel inputChannel = context.getBean("stringFilterIgnoresCaseInput", MessageChannel.class);
		QueueChannel replyChannel = new QueueChannel();
		PollableChannel discardChannel = context.getBean("stringFilterIgnoresCaseRejections", PollableChannel.class);
		Document docToAccept1 = XmlTestUtil.getDocumentForString("<name>OUTPUTONE</name>");
		Document docToAccept2 = XmlTestUtil.getDocumentForString("<name>outputOne</name>");
		Document docToReject = XmlTestUtil.getDocumentForString("<name>outputTwo</name>");
		Message<?> shouldBeAccepted1 = MessageBuilder.withPayload(docToAccept1).setReplyChannel(replyChannel).build();
		Message<?> shouldBeAccepted2 = MessageBuilder.withPayload(docToAccept2).setReplyChannel(replyChannel).build();
		Message<?> shouldBeRejected = MessageBuilder.withPayload(docToReject).setReplyChannel(replyChannel).build();
		inputChannel.send(shouldBeAccepted1);
		inputChannel.send(shouldBeAccepted2);
		inputChannel.send(shouldBeRejected);
		assertThat(replyChannel.receive(0)).isEqualTo(shouldBeAccepted1);
		assertThat(replyChannel.receive(0)).isEqualTo(shouldBeAccepted2);
		assertThat(discardChannel.receive(0)).isEqualTo(shouldBeRejected);
		assertThat(replyChannel.receive(0)).isNull();
		assertThat(discardChannel.receive(0)).isNull();
	}

	@Test
	public void stringExpressionRegex() throws Exception {
		MessageChannel inputChannel = context.getBean("stringFilterRegexInput", MessageChannel.class);
		QueueChannel replyChannel = new QueueChannel();
		PollableChannel discardChannel = context.getBean("stringFilterRegexRejections", PollableChannel.class);
		Document docToAccept1 = XmlTestUtil.getDocumentForString("<name>aBcDeFgHiJk</name>");
		Document docToAccept2 = XmlTestUtil.getDocumentForString("<name>xyz</name>");
		Document docToReject = XmlTestUtil.getDocumentForString("<name>abc123</name>");
		Message<?> shouldBeAccepted1 = MessageBuilder.withPayload(docToAccept1).setReplyChannel(replyChannel).build();
		Message<?> shouldBeAccepted2 = MessageBuilder.withPayload(docToAccept2).setReplyChannel(replyChannel).build();
		Message<?> shouldBeRejected = MessageBuilder.withPayload(docToReject).setReplyChannel(replyChannel).build();
		inputChannel.send(shouldBeAccepted1);
		inputChannel.send(shouldBeAccepted2);
		inputChannel.send(shouldBeRejected);
		assertThat(replyChannel.receive(0)).isEqualTo(shouldBeAccepted1);
		assertThat(replyChannel.receive(0)).isEqualTo(shouldBeAccepted2);
		assertThat(discardChannel.receive(0)).isEqualTo(shouldBeRejected);
		assertThat(replyChannel.receive(0)).isNull();
		assertThat(discardChannel.receive(0)).isNull();
	}

}
