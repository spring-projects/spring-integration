/*
 * Copyright 2002-2020 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringReader;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.SmartLifecycleRoleController;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.xml.XmlPayloadConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.MultiValueMap;
import org.springframework.xml.xpath.NodeMapper;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class XPathTransformerParserTests {

	@Autowired
	private MessageChannel defaultInput;

	@Autowired
	private MessageChannel numberInput;

	@Autowired
	private MessageChannel booleanInput;

	@Autowired
	private MessageChannel nodeInput;

	@Autowired
	private MessageChannel nodeListInput;

	@Autowired
	private MessageChannel nodeMapperInput;

	@Autowired
	private MessageChannel customConverterInput;

	@Autowired
	private MessageChannel expressionRefInput;

	@Autowired
	private PollableChannel output;

	@Autowired
	private EventDrivenConsumer parseOnly;

	@Autowired
	SmartLifecycleRoleController roleController;

	private final Message<?> message = MessageBuilder.withPayload("<person name='John Doe' age='42' married='true'/>").build();

	@Test
	public void testParse() throws Exception {
		assertThat(TestUtils.getPropertyValue(this.parseOnly, "handler.order")).isEqualTo(2);
		assertThat(TestUtils.getPropertyValue(this.parseOnly, "handler.messagingTemplate.sendTimeout")).isEqualTo(123L);
		assertThat(TestUtils.getPropertyValue(this.parseOnly, "phase")).isEqualTo(-1);
		assertThat(TestUtils.getPropertyValue(this.parseOnly, "autoStartup", Boolean.class)).isFalse();
		@SuppressWarnings("unchecked")
		List<SmartLifecycle> list = (List<SmartLifecycle>) TestUtils.getPropertyValue(roleController, "lifecycles",
				MultiValueMap.class).get("foo");
		assertThat(list).containsExactly((SmartLifecycle) this.parseOnly);
	}

	@Test
	public void stringResultByDefault() {
		this.defaultInput.send(message);
		assertThat(output.receive(0).getPayload()).isEqualTo("John Doe");
	}

	@Test
	public void numberResult() {
		this.numberInput.send(message);
		assertThat(output.receive(0).getPayload()).isEqualTo(42d);
	}

	@Test
	public void booleanResult() {
		this.booleanInput.send(message);
		assertThat(output.receive(0).getPayload()).isEqualTo(Boolean.TRUE);
	}

	@Test
	public void nodeResult() {
		this.nodeInput.send(message);
		Object payload = output.receive(0).getPayload();
		assertThat(payload instanceof Node).isTrue();
		Node node = (Node) payload;
		assertThat(node.getTextContent()).isEqualTo("42");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void nodeListResult() {
		this.nodeListInput.send(message);
		Object payload = output.receive(0).getPayload();
		assertThat(List.class.isAssignableFrom(payload.getClass())).isTrue();
		List<Node> nodeList = (List<Node>) payload;
		assertThat(nodeList.size()).isEqualTo(3);
	}

	@Test
	public void nodeMapper() {
		this.nodeMapperInput.send(message);
		assertThat(output.receive(0).getPayload()).isEqualTo("42-mapped");
	}

	@Test
	public void customConverter() {
		this.customConverterInput.send(message);
		assertThat(output.receive(0).getPayload()).isEqualTo("custom");
	}

	@Test
	public void expressionRef() {
		this.expressionRefInput.send(message);
		assertThat(output.receive(0).getPayload()).isEqualTo(84d);
	}


	@SuppressWarnings("unused")
	private static class TestNodeMapper implements NodeMapper<Object> {

		@Override
		public Object mapNode(Node node, int nodeNum) throws DOMException {
			return node.getTextContent() + "-mapped";
		}
	}


	@SuppressWarnings("unused")
	private static class TestXmlPayloadConverter implements XmlPayloadConverter {

		@Override
		public Source convertToSource(Object object) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Node convertToNode(Object object) {
			try {
				return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
						new InputSource(new StringReader("<test type='custom'/>")));
			}
			catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}

		@Override
		public Document convertToDocument(Object object) {
			throw new UnsupportedOperationException();
		}
	}

}
