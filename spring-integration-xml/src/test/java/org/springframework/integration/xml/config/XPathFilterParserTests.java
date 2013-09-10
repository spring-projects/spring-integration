/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.integration.xml.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.xml.util.XmlTestUtil;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.w3c.dom.Document;

/**
 * @author Mark Fisher
 * @since 2.1
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class XPathFilterParserTests {

	@Autowired
	private ApplicationContext context;

	@Test
	public void simpleStringExpressionBoolean() throws Exception {
		MessageChannel inputChannel = context.getBean("booleanFilterInput", MessageChannel.class); 
		QueueChannel replyChannel = new QueueChannel();
		PollableChannel discardChannel = context.getBean("booleanFilterRejections", PollableChannel.class); 
		Message<?> shouldBeAccepted = MessageBuilder.withPayload("<name>outputOne</name>").setReplyChannel(replyChannel).build();
		Message<?> shouldBeRejected = MessageBuilder.withPayload("<other>outputOne</other>").setReplyChannel(replyChannel).build();
		inputChannel.send(shouldBeAccepted);
		inputChannel.send(shouldBeRejected);
		assertEquals(shouldBeAccepted, replyChannel.receive(0));
		assertEquals(shouldBeRejected, discardChannel.receive(0));
		assertNull(replyChannel.receive(0));
		assertNull(discardChannel.receive(0));
	}
	
	@Test
	public void stringExpressionWithNamespaceBoolean() throws Exception {
		MessageChannel inputChannel = context.getBean("booleanFilterWithNamespaceInput", MessageChannel.class); 
		QueueChannel replyChannel = new QueueChannel();
		PollableChannel discardChannel = context.getBean("booleanFilterWithNamespaceRejections", PollableChannel.class); 
		Document docToAccept = XmlTestUtil.getDocumentForString("<ns1:name xmlns:ns1='www.example.org'>outputOne</ns1:name>");
		Document docToReject = XmlTestUtil.getDocumentForString("<name>outputOne</name>");
		Message<?> shouldBeAccepted = MessageBuilder.withPayload(docToAccept).setReplyChannel(replyChannel).build();
		Message<?> shouldBeRejected = MessageBuilder.withPayload(docToReject).setReplyChannel(replyChannel).build();
		inputChannel.send(shouldBeAccepted);
		inputChannel.send(shouldBeRejected);
		assertEquals(shouldBeAccepted, replyChannel.receive(0));
		assertEquals(shouldBeRejected, discardChannel.receive(0));
		assertNull(replyChannel.receive(0));
		assertNull(discardChannel.receive(0));
	}

	@Test
	public void stringExpressionWithNestedMapBoolean() throws Exception {
		MessageChannel inputChannel = context.getBean("nestedNamespaceMapFilterInput", MessageChannel.class); 
		QueueChannel replyChannel = new QueueChannel();
		PollableChannel discardChannel = context.getBean("nestedNamespaceMapFilterRejections", PollableChannel.class); 
		Document docToAccept = XmlTestUtil.getDocumentForString("<ns1:name xmlns:ns1='www.example.org'>outputOne</ns1:name>");
		Document docToReject = XmlTestUtil.getDocumentForString("<name>outputOne</name>");
		Message<?> shouldBeAccepted = MessageBuilder.withPayload(docToAccept).setReplyChannel(replyChannel).build();
		Message<?> shouldBeRejected = MessageBuilder.withPayload(docToReject).setReplyChannel(replyChannel).build();
		inputChannel.send(shouldBeAccepted);
		inputChannel.send(shouldBeRejected);
		assertEquals(shouldBeAccepted, replyChannel.receive(0));
		assertEquals(shouldBeRejected, discardChannel.receive(0));
		assertNull(replyChannel.receive(0));
		assertNull(discardChannel.receive(0));
	}

	@Test
	public void stringExpressionWithNamespaceString() throws Exception {
		MessageChannel inputChannel = context.getBean("stringFilterWithNamespaceInput", MessageChannel.class); 
		QueueChannel replyChannel = new QueueChannel();
		PollableChannel discardChannel = context.getBean("stringFilterWithNamespaceRejections", PollableChannel.class); 
		Document docToAccept = XmlTestUtil.getDocumentForString("<ns1:name xmlns:ns1='www.example.org'>outputOne</ns1:name>");
		Document docToReject = XmlTestUtil.getDocumentForString("<name>outputOne</name>");
		Message<?> shouldBeAccepted = MessageBuilder.withPayload(docToAccept).setReplyChannel(replyChannel).build();
		Message<?> shouldBeRejected = MessageBuilder.withPayload(docToReject).setReplyChannel(replyChannel).build();
		inputChannel.send(shouldBeAccepted);
		inputChannel.send(shouldBeRejected);
		assertEquals(shouldBeAccepted, replyChannel.receive(0));
		assertEquals(shouldBeRejected, discardChannel.receive(0));
		assertNull(replyChannel.receive(0));
		assertNull(discardChannel.receive(0));
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
		assertEquals(shouldBeAccepted1, replyChannel.receive(0));
		assertEquals(shouldBeAccepted2, replyChannel.receive(0));
		assertEquals(shouldBeRejected, discardChannel.receive(0));
		assertNull(replyChannel.receive(0));
		assertNull(discardChannel.receive(0));
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
		assertEquals(shouldBeAccepted1, replyChannel.receive(0));
		assertEquals(shouldBeAccepted2, replyChannel.receive(0));
		assertEquals(shouldBeRejected, discardChannel.receive(0));
		assertNull(replyChannel.receive(0));
		assertNull(discardChannel.receive(0));
	}

}
