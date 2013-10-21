/*
 * Copyright 2002-2010 the original author or authors.
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
import static org.junit.Assert.assertTrue;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Test;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.integration.xml.util.XmlTestUtil;
import org.springframework.test.context.ContextConfiguration;
import org.w3c.dom.Document;

/**
 * @author Jonas Partner
 */
@ContextConfiguration
public class XPathMessageSplitterParserTests {

	String channelDefinitions = "<si:channel id='test-input' /><si:channel id='test-output'><si:queue capacity='10'/></si:channel>";

	@Autowired
	@Qualifier("test-input")
	MessageChannel inputChannel;

	@Autowired
	@Qualifier("test-output")
	QueueChannel outputChannel;

	@Test
	public void testSimpleStringExpression() throws Exception {
		Document doc = XmlTestUtil.getDocumentForString("<names><name>Bob</name><name>John</name></names>");
		GenericMessage<Document> docMessage = new GenericMessage<Document>(doc);
		TestXmlApplicationContext ctx = TestXmlApplicationContextHelper
				.getTestAppContext(channelDefinitions
						+ "<si-xml:xpath-splitter id='splitter' input-channel='test-input' output-channel='test-output'><si-xml:xpath-expression expression='//name'/></si-xml:xpath-splitter>");
		EventDrivenConsumer consumer = (EventDrivenConsumer) ctx.getBean("splitter");
		consumer.start();
		ctx.getAutowireCapableBeanFactory().autowireBeanProperties(this, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE,
				false);
		inputChannel.send(docMessage);
		assertEquals("Wrong number of split messages ", 2, outputChannel.getQueueSize());
	}

	@Test
	public void testSimpleStringExpressionWithCreateDocuments() throws Exception {
		Document doc = XmlTestUtil.getDocumentForString("<names><name>Bob</name><name>John</name></names>");
		GenericMessage<Document> docMessage = new GenericMessage<Document>(doc);
		TestXmlApplicationContext ctx = TestXmlApplicationContextHelper
				.getTestAppContext(channelDefinitions
						+ "<si-xml:xpath-splitter id='splitter' input-channel='test-input' output-channel='test-output' create-documents='true'><si-xml:xpath-expression expression='//name'/></si-xml:xpath-splitter>");
		EventDrivenConsumer consumer = (EventDrivenConsumer) ctx.getBean("splitter");
		consumer.start();
		ctx.getAutowireCapableBeanFactory().autowireBeanProperties(this, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE,
				false);
		inputChannel.send(docMessage);
		assertEquals("Wrong number of split messages ", 2, outputChannel.getQueueSize());
		assertTrue("Splitter failed to create documents ",
				((Message<?>) outputChannel.receive(1000)).getPayload() instanceof Document);
		assertTrue("Splitter failed to create documents ",
				((Message<?>) outputChannel.receive(1000)).getPayload() instanceof Document);
	}

	@Test
	public void testProvideDocumentBuilder() throws Exception {
		TestXmlApplicationContext ctx = TestXmlApplicationContextHelper
				.getTestAppContext("<bean id='docBuilderFactory' class='org.springframework.integration.xml.config.StubDocumentBuilderFactory' />"
						+ channelDefinitions
						+ "<si-xml:xpath-splitter id='splitter' input-channel='test-input' output-channel='test-output' doc-builder-factory='docBuilderFactory'><si-xml:xpath-expression expression='//name'/></si-xml:xpath-splitter>");
		EventDrivenConsumer consumer = (EventDrivenConsumer) ctx.getBean("splitter");
		DirectFieldAccessor fieldAccessor = new DirectFieldAccessor(consumer);
		Object handler = fieldAccessor.getPropertyValue("handler");
		fieldAccessor = new DirectFieldAccessor(handler);
		Object documnetBuilderFactory = fieldAccessor.getPropertyValue("documentBuilderFactory");
		assertTrue("DocumnetBuilderFactory was not expected stub ", documnetBuilderFactory instanceof DocumentBuilderFactory);
	}
	
	@Test
	public void testXPathExpressionRef() throws Exception {
		TestXmlApplicationContext ctx = TestXmlApplicationContextHelper
				.getTestAppContext(
						 channelDefinitions +
						 "<si-xml:xpath-expression id='xpathOne' expression='//name'/>" +
						"<si-xml:xpath-splitter id='splitter' xpath-expression-ref='xpathOne' input-channel='test-input' output-channel='test-output' />");
		EventDrivenConsumer consumer = (EventDrivenConsumer) ctx.getBean("splitter");
		DirectFieldAccessor fieldAccessor = new DirectFieldAccessor(consumer);
		Object handler = fieldAccessor.getPropertyValue("handler");
		fieldAccessor = new DirectFieldAccessor(handler);
		Object documnetBuilderFactory = fieldAccessor.getPropertyValue("documentBuilderFactory");
		assertTrue("DocumnetBuilderFactory was not expected stub ", documnetBuilderFactory instanceof DocumentBuilderFactory);
	}

}
