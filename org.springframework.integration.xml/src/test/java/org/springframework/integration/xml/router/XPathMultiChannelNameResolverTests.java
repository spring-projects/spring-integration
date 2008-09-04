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

import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.MessagingException;
import org.springframework.integration.message.StringMessage;
import org.springframework.integration.xml.router.XPathMultiChannelNameResolver;
import org.springframework.integration.xml.util.XmlTestUtil;
import org.springframework.xml.xpath.XPathExpression;
import org.springframework.xml.xpath.XPathExpressionFactory;

/** 
 * @author Jonas Partner
 */
public class XPathMultiChannelNameResolverTests {

	@Test
	@SuppressWarnings("unchecked")
	public void testSimpleSingleeAttribute() throws Exception {
		Document doc = XmlTestUtil.getDocumentForString("<doc type=\"one\" />");
		XPathExpression expression = XPathExpressionFactory.createXPathExpression("/doc/@type");
		XPathMultiChannelNameResolver resolver = new XPathMultiChannelNameResolver(expression);
		String[] channelNames = resolver.resolveChannelNames(new GenericMessage(doc));
		assertEquals("Wrong number of channels returend", 1, channelNames.length);
		assertEquals("Wrong channel name", "one", channelNames[0]);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testMultipleNodeValues() throws Exception {
		Document doc = XmlTestUtil.getDocumentForString("<doc type=\"one\"><book>bOne</book><book>bTwo</book></doc>");
		XPathExpression expression = XPathExpressionFactory.createXPathExpression("/doc/book");
		XPathMultiChannelNameResolver resolver = new XPathMultiChannelNameResolver(expression);
		String[] channelNames = resolver.resolveChannelNames(new GenericMessage(doc));
		assertEquals("Wrong number of channels returend", 2, channelNames.length);
		assertEquals("Wrong channel name", "bOne", channelNames[0]);
		assertEquals("Wrong channel name", "bTwo", channelNames[1]);
	}

	@Test(expected = MessagingException.class)
	public void testNonNodePayload() throws Exception {
		XPathExpression expression = XPathExpressionFactory.createXPathExpression("/doc/@type");
		XPathMultiChannelNameResolver resolver = new XPathMultiChannelNameResolver(expression);
		resolver.resolveChannelNames(new StringMessage("test"));
	}

}
