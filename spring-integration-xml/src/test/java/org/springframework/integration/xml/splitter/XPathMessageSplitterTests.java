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

package org.springframework.integration.xml.splitter;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.handler.ReplyRequiredException;
import org.springframework.integration.xml.util.XmlTestUtil;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Jonas Partner
 * @author Artem Bilan
 */
public class XPathMessageSplitterTests {

	private XPathMessageSplitter splitter;

	private final QueueChannel replyChannel = new QueueChannel();

	@BeforeEach
	public void setUp() {
		String splittingXPath = "/orders/order";
		this.splitter = new XPathMessageSplitter(splittingXPath);
		this.splitter.setOutputChannel(replyChannel);
		this.splitter.setRequiresReply(true);
	}

	@Test
	public void splitDocument() throws Exception {
		Document doc = XmlTestUtil.getDocumentForString("""
				<orders>
					<order>one</order>
					<order>two</order>
					<order>three</order>
				</orders>
				""");
		this.splitter.handleMessage(new GenericMessage<>(doc));
		List<Message<?>> docMessages = this.replyChannel.clear();
		assertThat(docMessages.size()).as("Wrong number of messages").isEqualTo(3);
		for (Message<?> message : docMessages) {
			assertThat(message.getPayload()).isInstanceOf(Node.class);
			assertThat(message.getPayload()).isNotInstanceOf(Document.class);
			assertThat(new IntegrationMessageHeaderAccessor(message).getSequenceSize()).isGreaterThan(0);
		}
	}

	@Test
	public void splitDocumentThatDoesNotMatch() throws Exception {
		Document doc = XmlTestUtil.getDocumentForString("<wrongDocument/>");
		assertThatExceptionOfType(ReplyRequiredException.class)
				.isThrownBy(() -> this.splitter.handleMessage(new GenericMessage<>(doc)));
	}

	@Test
	public void splitDocumentWithCreateDocumentsTrue() throws Exception {
		this.splitter.setCreateDocuments(true);
		Document doc = XmlTestUtil.getDocumentForString("""
				<orders>
					<order>one</order>
					<order>two</order>
					<order>three</order>
				</orders>
				""");
		this.splitter.handleMessage(new GenericMessage<>(doc));
		List<Message<?>> docMessages = this.replyChannel.clear();
		assertThat(docMessages.size()).as("Wrong number of messages").isEqualTo(3);
		for (Message<?> message : docMessages) {
			assertThat(message.getPayload()).isInstanceOf(Document.class);
			Document docPayload = (Document) message.getPayload();
			assertThat(docPayload.getDocumentElement().getLocalName()).as("Wrong root element name").isEqualTo("order");
			assertThat(new IntegrationMessageHeaderAccessor(message).getSequenceSize()).isGreaterThan(0);
		}
	}

	@Test
	public void splitStringXml() {
		String payload = """
				<orders>
					<order>one</order>
					<order>two</order>
					<order>three</order>
				</orders>
				""";
		this.splitter.handleMessage(new GenericMessage<>(payload));
		List<Message<?>> docMessages = this.replyChannel.clear();
		assertThat(docMessages.size()).as("Wrong number of messages").isEqualTo(3);
		for (Message<?> message : docMessages) {
			assertThat(message.getPayload()).isInstanceOf(String.class);
			assertThat(new IntegrationMessageHeaderAccessor(message).getSequenceSize()).isGreaterThan(0);
		}
	}

	@Test
	public void invalidPayloadType() {
		assertThatExceptionOfType(MessageHandlingException.class)
				.isThrownBy(() -> this.splitter.handleMessage(new GenericMessage<>(123)));
	}

}
