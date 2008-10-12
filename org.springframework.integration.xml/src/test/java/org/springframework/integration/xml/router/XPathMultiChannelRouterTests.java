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

package org.springframework.integration.xml.router;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.MessagingException;
import org.springframework.integration.message.StringMessage;
import org.springframework.integration.xml.util.XmlTestUtil;
import org.springframework.xml.xpath.XPathExpression;
import org.springframework.xml.xpath.XPathExpressionFactory;

/** 
 * @author Jonas Partner
 */
public class XPathMultiChannelRouterTests {

	@Test
	@SuppressWarnings("unchecked")
	public void simpleSingleAttribute() throws Exception {
		Document doc = XmlTestUtil.getDocumentForString("<doc type=\"one\" />");
		XPathExpression expression = XPathExpressionFactory.createXPathExpression("/doc/@type");
		XPathMultiChannelRouter router = new XPathMultiChannelRouter(expression);
		String[] channelNames = router.determineTargetChannelNames(new GenericMessage(doc));
		assertEquals("Wrong number of channels returned", 1, channelNames.length);
		assertEquals("Wrong channel name", "one", channelNames[0]);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void multipleNodeValues() throws Exception {
		Document doc = XmlTestUtil.getDocumentForString("<doc type=\"one\"><book>bOne</book><book>bTwo</book></doc>");
		XPathExpression expression = XPathExpressionFactory.createXPathExpression("/doc/book");
		XPathMultiChannelRouter router = new XPathMultiChannelRouter(expression);
		String[] channelNames = router.determineTargetChannelNames(new GenericMessage(doc));
		assertEquals("Wrong number of channels returned", 2, channelNames.length);
		assertEquals("Wrong channel name", "bOne", channelNames[0]);
		assertEquals("Wrong channel name", "bTwo", channelNames[1]);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void multipleNodeValuesAsString() throws Exception {
		XPathExpression expression = XPathExpressionFactory.createXPathExpression("/doc/book");
		XPathMultiChannelRouter router = new XPathMultiChannelRouter(expression);
		String[] channelNames = router.determineTargetChannelNames(new GenericMessage("<doc type=\"one\"><book>bOne</book><book>bTwo</book></doc>"));
		assertEquals("Wrong number of channels returned", 2, channelNames.length);
		assertEquals("Wrong channel name", "bOne", channelNames[0]);
		assertEquals("Wrong channel name", "bTwo", channelNames[1]);
	}

	@Test(expected = MessagingException.class)
	public void nonNodePayload() throws Exception {
		XPathExpression expression = XPathExpressionFactory.createXPathExpression("/doc/@type");
		XPathMultiChannelRouter router = new XPathMultiChannelRouter(expression);
		router.determineTargetChannelNames(new StringMessage("test"));
	}

	@Test
	public void nodePayload() throws Exception {
		XPathMultiChannelRouter router = new XPathMultiChannelRouter("./three/text()");
		Document testDocument = XmlTestUtil.getDocumentForString("<one><two><three>bob</three><three>dave</three></two></one>");
		String[] channelNames =  router.determineTargetChannelNames(new GenericMessage<Node>(testDocument.getElementsByTagName("two").item(0)));
		assertEquals("bob",channelNames[0]);
		assertEquals("dave",channelNames[1]);
	}

}
