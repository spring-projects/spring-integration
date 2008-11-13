/*
 * Copyright 2002-2008 the original author or authors.
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

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.xml.util.XmlTestUtil;
import org.springframework.test.context.ContextConfiguration;
import org.w3c.dom.Document;

/**
 * @author Jonas Partner
 */
@ContextConfiguration
public class XPathRouterParserTests {

	String channelConfig = "<si:channel id='test-input'/> <si:channel id='outputOne'><si:queue capacity='10'/></si:channel>";
	
	@Autowired @Qualifier("test-input")
	MessageChannel inputChannel;
	
	@Autowired @Qualifier("outputOne")
	QueueChannel outputChannel;
	
	ConfigurableApplicationContext appContext;
	
	public EventDrivenConsumer buildContext(String routerDef){
		appContext = TestXmlApplicationContextHelper.getTestAppContext( channelConfig + routerDef);
		appContext.getAutowireCapableBeanFactory().autowireBeanProperties(this, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);
		EventDrivenConsumer consumer = (EventDrivenConsumer) appContext.getBean("router");
		consumer.start();
		return consumer;
	}
	
	
	@After
	public void tearDown(){
		if(appContext != null){
			appContext.close();
		}
	}
	
	@Test
	public void testSimpleStringExpression() throws Exception {
		Document doc = XmlTestUtil.getDocumentForString("<name>outputOne</name>");
		GenericMessage<Document> docMessage = new GenericMessage<Document>(doc);
		buildContext("<si-xml:xpath-router id='router' input-channel='test-input'><si-xml:xpath-expression expression='/name'/></si-xml:xpath-router>");
		inputChannel.send(docMessage);
		assertEquals("Wrong number of messages", 1, outputChannel.getMesssageCount());
	
	}

	@Test
	public void testNamespacedStringExpression() throws Exception {
		Document doc = XmlTestUtil.getDocumentForString("<ns1:name xmlns:ns1='www.example.org'>outputOne</ns1:name>");
		GenericMessage<Document> docMessage = new GenericMessage<Document>(doc);
		buildContext("<si-xml:xpath-router id='router' input-channel='test-input'><si-xml:xpath-expression expression='/ns2:name' ns-prefix='ns2' ns-uri='www.example.org' /></si-xml:xpath-router>");
		inputChannel.send(docMessage);
		assertEquals("Wrong number of messages", 1, outputChannel.getMesssageCount());
	
	}

	@Test
	public void testStringExpressionWithNestedNamespaceMap() throws Exception {
		Document doc = XmlTestUtil.getDocumentForString(
				"<ns1:name xmlns:ns1='www.example.org' xmlns:ns2='www.example.org2'><ns2:type>outputOne</ns2:type></ns1:name>");
		GenericMessage<Document> docMessage = new GenericMessage<Document>(doc);
		StringBuffer buffer = new StringBuffer(
				"<si-xml:xpath-router id='router' input-channel='test-input'><si-xml:xpath-expression expression='/ns1:name/ns2:type'> ");
		buffer.append("<map><entry key='ns1' value='www.example.org' /> <entry key='ns2' value='www.example.org2'/></map>");
		buffer.append("</si-xml:xpath-expression></si-xml:xpath-router>");
		buildContext(buffer.toString());
		inputChannel.send(docMessage);
		assertEquals("Wrong number of messages", 1, outputChannel.getMesssageCount());
	
	}

	@Test
	public void testStringExpressionWithReferenceToNamespaceMap() throws Exception {
		Document doc = XmlTestUtil.getDocumentForString(
				"<ns1:name xmlns:ns1='www.example.org' xmlns:ns2='www.example.org2'><ns2:type>outputOne</ns2:type></ns1:name>");
		GenericMessage<Document> docMessage = new GenericMessage<Document>(doc);
		StringBuffer buffer = new StringBuffer(
				"<si-xml:xpath-router id='router' input-channel='test-input'><si-xml:xpath-expression expression='/ns1:name/ns2:type' namespace-map='nsMap'/>");
		buffer.append("</si-xml:xpath-router>");
		buffer.append("<util:map id='nsMap'><entry key='ns1' value='www.example.org' /><entry key='ns2' value='www.example.org2' /></util:map>");
		
		buildContext(buffer.toString());
		inputChannel.send(docMessage);
		assertEquals("Wrong number of messages", 1, outputChannel.getMesssageCount());
	}
	
	@Test
	public void testSetChannelResolver() throws Exception {
		Document doc = XmlTestUtil.getDocumentForString("<name>outputOne</name>");
		GenericMessage<Document> docMessage = new GenericMessage<Document>(doc);
		StringBuffer contextBuffer = new StringBuffer("<si-xml:xpath-router id='router' channel-resolver='stubResolver' input-channel='test-input'><si-xml:xpath-expression expression='/name'/></si-xml:xpath-router>");
		contextBuffer.append("<bean id='stubResolver' class='").append(StubChannelResolver.class.getName()).append("'/>");
		EventDrivenConsumer consumer = buildContext(contextBuffer.toString());
		
		DirectFieldAccessor accessor = new DirectFieldAccessor(consumer);
		Object handler = accessor.getPropertyValue("handler");
		accessor = new DirectFieldAccessor(handler);
		Object resolver = accessor.getPropertyValue("channelResolver");
		assertEquals("Wrong channel resolver ",StubChannelResolver.class, resolver.getClass());
		
	}
	
	@Test
	public void testSetResolutionRequiredFalse() throws Exception {
		Document doc = XmlTestUtil.getDocumentForString("<name>outputOne</name>");
		GenericMessage<Document> docMessage = new GenericMessage<Document>(doc);
		StringBuffer contextBuffer = new StringBuffer("<si-xml:xpath-router id='router' resolution-required='false' input-channel='test-input'><si-xml:xpath-expression expression='/name'/></si-xml:xpath-router>");
		EventDrivenConsumer consumer = buildContext(contextBuffer.toString());
		
		DirectFieldAccessor accessor = new DirectFieldAccessor(consumer);
		Object handler = accessor.getPropertyValue("handler");
		accessor = new DirectFieldAccessor(handler);
		Object resolutionRequired = accessor.getPropertyValue("resolutionRequired");
		assertEquals("Resolution required not set to false ", false, resolutionRequired);
		
	}
	
	@Test
	public void testSetResolutionRequiredTrue() throws Exception {
		Document doc = XmlTestUtil.getDocumentForString("<name>outputOne</name>");
		GenericMessage<Document> docMessage = new GenericMessage<Document>(doc);
		StringBuffer contextBuffer = new StringBuffer("<si-xml:xpath-router id='router' resolution-required='true' input-channel='test-input'><si-xml:xpath-expression expression='/name'/></si-xml:xpath-router>");
		EventDrivenConsumer consumer = buildContext(contextBuffer.toString());
		
		DirectFieldAccessor accessor = new DirectFieldAccessor(consumer);
		Object handler = accessor.getPropertyValue("handler");
		accessor = new DirectFieldAccessor(handler);
		Object resolutionRequired = accessor.getPropertyValue("resolutionRequired");
		assertEquals("Resolution required not set to true ", true, resolutionRequired);
	}
	
}
