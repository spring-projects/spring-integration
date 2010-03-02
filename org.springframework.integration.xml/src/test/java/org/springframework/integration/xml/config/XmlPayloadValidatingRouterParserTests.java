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

import org.junit.After;
import org.junit.Test;
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
public class XmlPayloadValidatingRouterParserTests {

	String channelConfig = "<si:channel id='test-input'/> <si:channel id='validOutputChannel'><si:queue capacity='10'/></si:channel> <si:channel id='invalidOutputChannel'><si:queue capacity='10'/></si:channel>";
	
	@Autowired @Qualifier("test-input")
	MessageChannel inputChannel;
	
	@Autowired @Qualifier("validOutputChannel")
	QueueChannel validOutputChannel;
	
	@Autowired @Qualifier("invalidOutputChannel")
	QueueChannel invalidOutputChannel;
	
	
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
	public void testValidMessage() throws Exception {
		Document doc = XmlTestUtil.getDocumentForString("<greeting>hello</greeting>");
		GenericMessage<Document> docMessage = new GenericMessage<Document>(doc);
		buildContext("<si-xml:validating-router id='router' input-channel='test-input' valid-channel='validOutputChannel' invalid-channel='invalidOutputChannel' schema-location='org/springframework/integration/xml/config/validationTestsSchema.xsd' />");
		inputChannel.send(docMessage);
		assertEquals("Wrong number of messages", 1, validOutputChannel.getQueueSize());
	}

	@Test
	public void testInvalidMessage() throws Exception {
		Document doc = XmlTestUtil.getDocumentForString("<greeting><other/></greeting>");
		GenericMessage<Document> docMessage = new GenericMessage<Document>(doc);
		buildContext("<si-xml:validating-router id='router' input-channel='test-input' valid-channel='validOutputChannel' invalid-channel='invalidOutputChannel' schema-location='org/springframework/integration/xml/config/validationTestsSchema.xsd' />");
		inputChannel.send(docMessage);
		assertEquals("Wrong number of messages", 1, invalidOutputChannel.getQueueSize());
	}

}
