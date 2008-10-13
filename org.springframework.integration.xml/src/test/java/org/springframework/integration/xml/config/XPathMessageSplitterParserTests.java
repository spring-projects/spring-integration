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

import org.junit.Test;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.xml.splitter.XPathMessageSplitter;
import org.springframework.integration.xml.util.XmlTestUtil;
import org.springframework.test.context.ContextConfiguration;
import org.w3c.dom.Document;

/**
 * @author Jonas Partner
 */
@ContextConfiguration
public class XPathMessageSplitterParserTests {


	@Test
	public void testSimpleStringExpression() throws Exception {
		Document doc = XmlTestUtil.getDocumentForString("<names><name>Bob</name><name>John</name></names>");
		GenericMessage<Document> docMessage = new GenericMessage<Document>(doc);

		TestXmlApplicationContext ctx = TestXmlApplicationContextHelper.getTestAppContext("<si-xml:xpath-splitter id='splitter'><si-xml:xpath-expression expression='//name'/></si-xml:xpath-splitter>");
		XPathMessageSplitter splitter = (XPathMessageSplitter) ctx.getBean("splitter");
		
		QueueChannel queueChannel = new QueueChannel(10);
		splitter.setOutputChannel(queueChannel);
		splitter.onMessage(docMessage);
		assertEquals("Wrong number of split messages ", 2, queueChannel.getMesssageCount());
	
	}

}
